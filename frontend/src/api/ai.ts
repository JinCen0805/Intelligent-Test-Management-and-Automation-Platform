import { request } from './client'
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

export const ai = {
  models: () => request<ApiAiModel[]>('/ai/models'),
  prompts: () => request<ApiPromptTemplate[]>('/ai/prompts'),
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
    rules?: string
  } = {}) => request<ApiAiTaskSubmit>(`/projects/${projectId}/requirements/${requirementId}/test-cases/generate`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}
