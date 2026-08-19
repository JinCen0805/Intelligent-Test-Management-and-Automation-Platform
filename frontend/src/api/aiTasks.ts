import { request } from './client'

export interface ApiAiTaskSubmit {
  taskId: string
  status: string
}

export interface ApiAiTaskStatus {
  taskId: string
  type: string
  status: 'RUNNING' | 'SUCCESS' | 'FAILED'
  errorCode?: number
  errorMessage?: string
  result?: any
}

export const aiTasks = {
  status: (taskId: string) => request<ApiAiTaskStatus>(`/ai-tasks/${encodeURIComponent(taskId)}`)
}
