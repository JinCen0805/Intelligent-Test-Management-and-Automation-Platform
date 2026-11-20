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
 * 采用串行 setTimeout 链而非 setInterval：单次状态查询未返回前不会叠加下一次请求，
 * 避免慢响应时并发请求与重复触发 onSuccess；回调异常也不会产生未处理的 Promise rejection。
 *
 * @returns 一个 stop 函数，可提前取消轮询。
 */
export function pollAiTask<T>(taskId: string, handlers: PollAiTaskHandlers<T>): () => void {
  const { onSuccess, onFailure, intervalMs = 1000 } = handlers
  let stopped = false
  let timer: number | undefined

  const finish = () => {
    stopped = true
    if (timer !== undefined) {
      window.clearTimeout(timer)
      timer = undefined
    }
  }

  const step = async () => {
    if (stopped) return
    let status: ApiAiTaskStatus
    try {
      status = await api.aiTasks.status(taskId)
    } catch (error) {
      finish()
      onFailure(error instanceof Error ? error.message : 'AI 任务状态查询失败')
      return
    }
    if (stopped) return
    if (status.status === 'SUCCESS') {
      finish()
      try {
        await onSuccess(status.result as T)
      } catch {
        // onSuccess 的异常不应冒泡为未处理 rejection。
      }
    } else if (status.status === 'FAILED') {
      finish()
      onFailure(status.errorMessage || 'AI 任务执行失败')
    } else {
      timer = window.setTimeout(step, intervalMs)
    }
  }

  timer = window.setTimeout(step, intervalMs)
  return finish
}
