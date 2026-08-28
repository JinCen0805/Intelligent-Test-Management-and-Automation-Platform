import { request, requestFormData } from './client'
import type { ApiAiTaskSubmit } from './aiTasks'

export interface ApiGeneratedCase {
  id: number
  caseNo: string
  title: string
  status: string
}

export interface ApiAiModel {
  id: number
  name: string
  provider: string
  endpoint: string
  hasApiKey: boolean
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

export interface ApiPromptTemplate {
  id: number
  name: string
  taskType: string
  content: string
  version: string
  enabled: boolean
}

export interface ApiAiCallAudit {
  id: number
  projectId?: number | null
  userId?: number | null
  userName?: string | null
  taskType: string
  modelId?: number | null
  modelName?: string | null
  provider?: string | null
  promptId?: number | null
  promptName?: string | null
  promptVersion?: string | null
  status: string
  statusCode?: number | null
  errorMessage?: string | null
  latencyMs?: number | null
  createdAt: string
}

export interface ApiCaseChatResponse {
  sessionId: number
  modelId: number
  provider: string
  reply: string
  generatedCases?: ApiGeneratedCase[]
}

export interface ApiCaseChatMessage {
  id: number
  role: 'assistant' | 'user'
  content: string
}

export interface ApiCaseChatSession {
  sessionId: number
  title: string
  messages: ApiCaseChatMessage[]
  createdAt: string
  updatedAt: string
  suiteType?: string
}

export const ai = {
  models: () => request<ApiAiModel[]>('/ai/models'),
  prompts: () => request<ApiPromptTemplate[]>('/ai/prompts'),
  audits: () => request<ApiAiCallAudit[]>('/ai/audits'),
  createModel: (payload: {
    name: string
    provider: string
    endpoint: string
    apiKey?: string
    enabled?: boolean
  }) => request<ApiAiModel>('/ai/models', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  updateModel: (modelId: number, payload: {
    name: string
    provider: string
    endpoint: string
    apiKey?: string
    enabled?: boolean
  }) => request<ApiAiModel>(`/ai/models/${modelId}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  }),
  setModelEnabled: (modelId: number, enabled: boolean) => request<ApiAiModel>(
    `/ai/models/${modelId}/enabled`,
    { method: 'PUT', body: JSON.stringify({ enabled }) }
  ),
  testModel: (modelId: number) => request<void>(
    `/ai/models/${modelId}/test`,
    { method: 'POST' }
  ),
  deleteModel: (modelId: number) => request<void>(
    `/ai/models/${modelId}`,
    { method: 'DELETE' }
  ),
  createPrompt: (payload: {
    name: string
    taskType: string
    content: string
    version: string
    enabled?: boolean
  }) => request<ApiPromptTemplate>('/ai/prompts', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  updatePrompt: (promptId: number, payload: {
    name: string
    taskType: string
    content: string
    version: string
    enabled?: boolean
  }) => request<ApiPromptTemplate>(`/ai/prompts/${promptId}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  }),
  setPromptEnabled: (promptId: number, enabled: boolean) => request<ApiPromptTemplate>(
    `/ai/prompts/${promptId}/enabled`,
    { method: 'PUT', body: JSON.stringify({ enabled }) }
  ),
  publishPrompt: (promptId: number, version?: string) => request<ApiPromptTemplate>(
    `/ai/prompts/${promptId}/publish`,
    { method: 'POST', body: JSON.stringify(version ? { version } : {}) }
  ),
  activatePrompt: (promptId: number) => request<ApiPromptTemplate>(
    `/ai/prompts/${promptId}/activate`,
    { method: 'POST' }
  ),
  generate: (projectId: number, requirementId: number, payload: {
    modelId?: number
    promptId?: number
    suiteType?: string
    rules?: string
  } = {}) => request<ApiAiTaskSubmit>(`/projects/${projectId}/files/${requirementId}/test-cases/generate`, {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  caseChat: (projectId: number, payload: {
    sessionId?: number | null
    modelId?: number | null
    requirementId?: number | null
    selectedCaseId?: number | null
    suiteType: string
    message: string
  }) => request<ApiCaseChatResponse>(`/projects/${projectId}/test-cases/chat`, {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  caseChatWithFile: (projectId: number, file: File, payload: {
    sessionId?: number | null
    modelId?: number | null
    selectedCaseId?: number | null
    suiteType: string
    message: string
  }) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('message', payload.message)
    formData.append('suiteType', payload.suiteType)
    if (payload.sessionId != null) formData.append('sessionId', String(payload.sessionId))
    if (payload.modelId != null) formData.append('modelId', String(payload.modelId))
    if (payload.selectedCaseId != null) formData.append('selectedCaseId', String(payload.selectedCaseId))
    return requestFormData<ApiCaseChatResponse>(`/projects/${projectId}/test-cases/chat/file`, formData)
  },
  caseChatHistory: (projectId: number, suiteType: string) =>
    request<ApiCaseChatSession[]>(`/projects/${projectId}/test-cases/chat/history?suiteType=${encodeURIComponent(suiteType)}`),
  deleteCaseChatHistory: (projectId: number, sessionId: number, suiteType: string) =>
    request<void>(`/projects/${projectId}/test-cases/chat/history/${sessionId}?suiteType=${encodeURIComponent(suiteType)}`, { method: 'DELETE' }),
  deleteAllCaseChatHistory: (projectId: number, suiteType: string) =>
    request<void>(`/projects/${projectId}/test-cases/chat/history?suiteType=${encodeURIComponent(suiteType)}`, { method: 'DELETE' })
}
