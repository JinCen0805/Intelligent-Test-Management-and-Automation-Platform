import { api } from '@/api'
import type { ApiAiTaskStatus } from '@/api'

export interface PollAiTaskHandlers<T> {
  onSuccess: (result: T) => void | Promise<void>
  onFailure: (message: string) => void
  intervalMs?: number
}

/**
 * 轮询一个 AI 异步任务直到终态。
 *
 * 提交 AI 生成/分析后，后端立即返回 taskId 并在后台线程执行；本工具每 intervalMs
 * 查询一次任务状态，`SUCCESS` 时把 result 交给 onSuccess，`FAILED` 时把错误信息交给
 * onFailure，到达终态即自清理。定时器生命周期独立于弹窗，只有走到终态才停止。
 *
 * @returns 一个 stop 函数，可提前取消轮询。
 */
export function pollAiTask<T>(taskId: string, handlers: PollAiTaskHandlers<T>): () => void {
  const { onSuccess, onFailure, intervalMs = 1000 } = handlers
  let stopped = false

  const timer = window.setInterval(async () => {
    if (stopped) return
    let status: ApiAiTaskStatus
    try {
      status = await api.aiTasks.status(taskId)
    } catch (error) {
      stopped = true
      window.clearInterval(timer)
      onFailure(error instanceof Error ? error.message : 'AI 任务状态查询失败')
      return
    }
    if (status.status === 'SUCCESS') {
      stopped = true
      window.clearInterval(timer)
      await onSuccess(status.result as T)
    } else if (status.status === 'FAILED') {
      stopped = true
      window.clearInterval(timer)
      onFailure(status.errorMessage || 'AI 任务执行失败')
    }
  }, intervalMs)

  return () => {
    stopped = true
    window.clearInterval(timer)
  }
}
