import { describe, it, expect, vi, beforeEach } from 'vitest'
import { pollAiTask } from './pollAiTask'
import { api } from '@/api'

vi.mock('@/api', () => ({
  api: {
    aiTasks: {
      status: vi.fn()
    }
  }
}))

describe('pollAiTask', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('串行轮询直到 SUCCESS，只回调一次', async () => {
    const statuses = ['RUNNING', 'RUNNING', 'SUCCESS']
    let calls = 0
    vi.mocked(api.aiTasks.status).mockImplementation(async () => ({
      taskId: 'task-1',
      type: 'TEST_CASE_GENERATION',
      status: statuses[Math.min(calls++, statuses.length - 1)] as 'RUNNING' | 'SUCCESS',
      result: { ok: true }
    }))

    const onSuccess = vi.fn()
    const onFailure = vi.fn()
    await new Promise<void>((resolve) => {
      pollAiTask('task-1', {
        intervalMs: 1,
        onSuccess: (result) => {
          onSuccess(result)
          resolve()
        },
        onFailure
      })
    })

    expect(calls).toBe(3)
    expect(onSuccess).toHaveBeenCalledTimes(1)
    expect(onFailure).not.toHaveBeenCalled()
  })

  it('FAILED 终态回调 onFailure 且停止轮询', async () => {
    vi.mocked(api.aiTasks.status).mockResolvedValue({
      taskId: 'task-2',
      type: 'FAILURE_ANALYSIS',
      status: 'FAILED',
      errorMessage: 'AI 任务执行失败'
    })

    const onSuccess = vi.fn()
    const onFailure = vi.fn()
    await new Promise<void>((resolve) => {
      pollAiTask('task-2', {
        intervalMs: 1,
        onSuccess,
        onFailure: (message) => {
          onFailure(message)
          resolve()
        }
      })
    })

    expect(onFailure).toHaveBeenCalledWith('AI 任务执行失败')
    expect(onSuccess).not.toHaveBeenCalled()
    expect(vi.mocked(api.aiTasks.status)).toHaveBeenCalledTimes(1)
  })

  it('stop() 后不再发起状态查询', async () => {
    vi.mocked(api.aiTasks.status).mockResolvedValue({
      taskId: 'task-3',
      type: 'TEST_CASE_GENERATION',
      status: 'RUNNING'
    })

    const stop = pollAiTask('task-3', { intervalMs: 5, onSuccess: vi.fn(), onFailure: vi.fn() })
    stop()
    await new Promise((resolve) => setTimeout(resolve, 30))
    expect(vi.mocked(api.aiTasks.status)).not.toHaveBeenCalled()
  })
})
