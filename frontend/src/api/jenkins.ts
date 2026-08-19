import { request } from './client'

export interface ApiJenkinsInstance {
  id: number
  name: string
  baseUrl: string
  username: string
  tokenConfigured: boolean
  status: string
  connectionStatus: string
  connectTimeoutMs: number
  readTimeoutMs: number
  pollIntervalMs: number
  maxWaitMinutes: number
  lastCheckedAt?: string
  lastError?: string
}

export interface ApiJenkinsJob {
  name: string
  path: string
  url?: string
  pipelineType: string
  jenkinsClass?: string
  buildable: boolean
  parametersJson?: string
  imported: boolean
}

export interface ApiCredentialGrant {
  id: number
  projectId: number
  credentialRef: string
  credentialScopeRef?: string
  status: string
}

export const jenkins = {
  instances: () => request<ApiJenkinsInstance[]>('/admin/jenkins-instances'),
  createInstance: (payload: {
    name: string
    baseUrl: string
    username: string
    apiToken?: string
    connectTimeoutMs?: number
    readTimeoutMs?: number
    pollIntervalMs?: number
    maxWaitMinutes?: number
  }) => request<ApiJenkinsInstance>('/admin/jenkins-instances', {
    method: 'POST', body: JSON.stringify(payload)
  }),
  updateInstance: (instanceId: number, payload: {
    name: string
    baseUrl: string
    username: string
    apiToken?: string
    connectTimeoutMs?: number
    readTimeoutMs?: number
    pollIntervalMs?: number
    maxWaitMinutes?: number
  }) => request<ApiJenkinsInstance>(`/admin/jenkins-instances/${instanceId}`, {
    method: 'PUT', body: JSON.stringify(payload)
  }),
  testConnection: (instanceId: number) => request<ApiJenkinsInstance>(
    `/admin/jenkins-instances/${instanceId}/test-connection`, { method: 'POST' }
  ),
  changeStatus: (instanceId: number, status: string) => request<ApiJenkinsInstance>(
    `/admin/jenkins-instances/${instanceId}/status`,
    { method: 'POST', body: JSON.stringify({ status }) }
  ),
  deleteInstance: (instanceId: number) => request<void>(
    `/admin/jenkins-instances/${instanceId}`, { method: 'DELETE' }
  ),
  credentialGrants: (projectId: number) => request<ApiCredentialGrant[]>(
    `/projects/${projectId}/jenkins-credential-grants`
  ),
  createCredentialGrant: (projectId: number, payload: {
    credentialRef: string
    credentialScopeRef?: string
  }) => request<ApiCredentialGrant>(`/projects/${projectId}/jenkins-credential-grants`, {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  revokeCredentialGrant: (projectId: number, grantId: number) => request<void>(
    `/projects/${projectId}/jenkins-credential-grants/${grantId}`,
    { method: 'DELETE' }
  )
}
