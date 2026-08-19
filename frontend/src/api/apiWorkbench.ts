import { request } from './client'

export interface ApiCollection {
  id: number
  projectId: number
  name: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

export interface ApiWorkbenchRequest {
  id: number
  projectId: number
  collectionId: number
  name: string
  method: string
  url: string
  paramsJson?: string
  headersJson?: string
  body?: string
  assertionsJson?: string
  preRequestScript?: string
  postRequestScript?: string
}

export interface ApiEnvironmentVariable {
  id: number
  environment: string
  name: string
  value?: string
  sensitive: boolean
  source?: string
}

export interface ApiRequestHistory {
  id: number
  requestId: number
  collectionRunId?: string
  requestName: string
  environment: string
  status: string
  statusCode?: number
  durationMs?: number
  responseHeadersJson?: string
  responseBody?: string
  assertionResultsJson?: string
  requestSnapshotJson?: string
  startedAt: string
  finishedAt?: string
}

export interface ApiRequestHistoryPage {
  content: ApiRequestHistory[]
  number: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ApiCollectionExecution {
  runId: string
  collectionId: number
  environment: string
  status: string
  results: ApiRequestHistory[]
}

export const apiWorkbench = {
  collections: (projectId: number) => request<ApiCollection[]>(
    `/projects/${projectId}/api-workbench/collections`
  ),
  createCollection: (projectId: number, payload: { name: string; description?: string }) =>
    request<ApiCollection>(`/projects/${projectId}/api-workbench/collections`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  requests: (projectId: number, collectionId: number) =>
    request<ApiWorkbenchRequest[]>(
      `/projects/${projectId}/api-workbench/collections/${collectionId}/requests`
    ),
  createRequest: (projectId: number, payload: {
    collectionId: number
    name: string
    method: string
    url: string
    paramsJson?: string
    headersJson?: string
    body?: string
    assertionsJson?: string
    preRequestScript?: string
    postRequestScript?: string
  }) => request<ApiWorkbenchRequest>(`/projects/${projectId}/api-workbench/requests`, {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  updateRequest: (projectId: number, requestId: number, payload: {
    collectionId: number
    name: string
    method: string
    url: string
    paramsJson?: string
    headersJson?: string
    body?: string
    assertionsJson?: string
    preRequestScript?: string
    postRequestScript?: string
  }) => request<ApiWorkbenchRequest>(
    `/projects/${projectId}/api-workbench/requests/${requestId}`,
    { method: 'PUT', body: JSON.stringify(payload) }
  ),
  execute: (projectId: number, requestId: number, environment: string) =>
    request<ApiRequestHistory>(
      `/projects/${projectId}/api-workbench/requests/${requestId}/execute`,
      { method: 'POST', body: JSON.stringify({ environment }) }
    ),
  executeCollection: (projectId: number, collectionId: number, environment: string, stopOnFailure = true) =>
    request<ApiCollectionExecution>(
      `/projects/${projectId}/api-workbench/collections/${collectionId}/execute`,
      { method: 'POST', body: JSON.stringify({ environment, stopOnFailure }) }
    ),
  variables: (projectId: number, environment: string) =>
    request<ApiEnvironmentVariable[]>(
      `/projects/${projectId}/api-workbench/variables?environment=${encodeURIComponent(environment)}`
    ),
  saveVariable: (projectId: number, payload: {
    environment: string
    name: string
    value?: string
    sensitive?: boolean
    source?: string
  }) => request<ApiEnvironmentVariable>(`/projects/${projectId}/api-workbench/variables`, {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  history: (projectId: number, page = 0, size = 20) =>
    request<ApiRequestHistoryPage>(
      `/projects/${projectId}/api-workbench/history?page=${page}&size=${size}`
    )
}
