export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface ApiUser {
  id: number
  username: string
  displayName: string
  email: string
  role: string
  status?: string
  lastLoginAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface ApiProjectMember {
  membershipId?: number
  userId: number
  username: string
  displayName: string
  email?: string
  role: string
  joinedAt?: string
}

export interface ApiProject {
  id: number
  name: string
  code: string
  description: string
  ownerId: number
  ownerName: string
  defaultEnvironment: string
  status: string
  memberCount: number
  createdAt: string
  updatedAt: string
}

export interface ApiRequirement {
  id: number
  projectId: number
  name: string
  fileType: string
  version: string
  parseStatus: string
  contentLength: number
  uploadedBy: number
  createdAt: string
  updatedAt: string
}

export interface ApiTestCase {
  id: number
  projectId: number
  requirementId?: number | null
  caseNo: string
  title: string
  moduleName?: string | null
  precondition?: string | null
  steps?: string | null
  testData?: string | null
  expectedResult?: string | null
  priority: string
  caseType: string
  tags?: string | null
  status: 'DRAFT' | 'REVIEW' | 'APPROVED' | 'REJECTED'
  source: string
  createdBy: number
  createdAt: string
  updatedAt: string
}

export interface ApiTestCasePage {
  content: ApiTestCase[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface ApiGeneratedCase {
  id: number
  caseNo: string
  title: string
  status: string
}

export interface ApiExecution {
  id: number
  projectId: number
  testTaskId: number
  automationProjectId: number
  jenkinsJob?: string
  queueUrl?: string
  buildNumber?: string
  commitSha?: string
  status: string
  startedAt?: string
  finishedAt?: string
  durationMs?: number
  consoleLogRef?: string
  report?: {
    id: number
    executionId: number
    allureUrl?: string
    totalCount: number
    passedCount: number
    failedCount: number
    brokenCount: number
    skippedCount: number
    artifactRef?: string
  }
}

export interface ApiAutomationProject {
  id: number
  projectId: number
  name: string
  automationType: string
  repositoryUrl: string
  branch: string
  credentialRef?: string
  framework: string
  testPath?: string
  executeCommand?: string
  environment?: string
  jenkinsJob?: string
  status: string
}

export interface ApiTestTask {
  id: number
  projectId: number
  name: string
  taskType: string
  automationProjectId: number
  environment: string
  branch: string
  jenkinsJob: string
  parametersJson?: string
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

interface LoginResponse {
  accessToken: string
  user: ApiUser
}

const apiBase = import.meta.env.VITE_API_BASE_URL ?? '/api'

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('testpilot.accessToken')
  const headers = new Headers(init.headers)
  headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(`${apiBase}${path}`, { ...init, headers })
  const payload = await response.json() as ApiResponse<T>
  if (!response.ok || payload.code !== 0) {
    throw new Error(payload.message || `请求失败 (${response.status})`)
  }
  return payload.data
}

export const api = {
  auth: {
    login: (username: string, password: string) => request<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    }),
    me: () => request<ApiUser>('/auth/me'),
    logout: () => request<void>('/auth/logout', { method: 'POST' })
  },
  projects: {
    list: () => request<ApiProject[]>('/projects'),
    create: (payload: {
      name: string
      code: string
      description: string
      defaultEnvironment: string
    }) => request<ApiProject>('/projects', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
    update: (projectId: number, payload: {
      name: string
      description?: string
      defaultEnvironment?: string
      status?: string
    }) => request<ApiProject>(`/projects/${projectId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
    remove: (projectId: number) => request<void>(`/projects/${projectId}`, {
      method: 'DELETE'
    }),
    members: (projectId: number) => request<ApiProjectMember[]>(`/projects/${projectId}/members`),
    addMember: (projectId: number, payload: { userId: number; role: string }) =>
      request<ApiProjectMember>(`/projects/${projectId}/members`, {
        method: 'POST',
        body: JSON.stringify(payload)
      }),
    updateMember: (projectId: number, userId: number, payload: { role: string }) =>
      request<ApiProjectMember>(`/projects/${projectId}/members/${userId}`, {
        method: 'PUT',
        body: JSON.stringify({ userId, ...payload })
      }),
    removeMember: (projectId: number, userId: number) =>
      request<void>(`/projects/${projectId}/members/${userId}`, { method: 'DELETE' })
  },
  users: {
    list: () => request<ApiUser[]>('/users'),
    create: (payload: {
      username: string
      password: string
      displayName: string
      email?: string
      role: string
    }) => request<ApiUser>('/users', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
    update: (userId: number, payload: {
      displayName: string
      email?: string
      role: string
    }) => request<ApiUser>(`/users/${userId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
    changeStatus: (userId: number, status: string) =>
      request<ApiUser>(`/users/${userId}/status`, {
        method: 'POST',
        body: JSON.stringify({ status })
      }),
    resetPassword: (userId: number, password: string) =>
      request<void>(`/users/${userId}/password`, {
        method: 'POST',
        body: JSON.stringify({ password })
      })
  },
  requirements: {
    list: (projectId: number) => request<ApiRequirement[]>(`/projects/${projectId}/requirements`),
    upload: async (projectId: number, file: File, version: string) => {
      const token = localStorage.getItem('testpilot.accessToken')
      const formData = new FormData()
      formData.append('file', file)
      formData.append('version', version)
      const headers = new Headers()
      if (token) headers.set('Authorization', `Bearer ${token}`)
      const response = await fetch(`${apiBase}/projects/${projectId}/requirements/upload`, {
        method: 'POST',
        headers,
        body: formData
      })
      const payload = await response.json() as ApiResponse<ApiRequirement>
      if (!response.ok || payload.code !== 0) {
        throw new Error(payload.message || `上传失败 (${response.status})`)
      }
      return payload.data
    },
    parse: (projectId: number, requirementId: number) => request<ApiRequirement>(
      `/projects/${projectId}/requirements/${requirementId}/parse`,
      { method: 'POST' }
    ),
    detail: (projectId: number, requirementId: number) => request<{
      id: number
      projectId: number
      name: string
      fileType: string
      version: string
      parseStatus: string
      parseError?: string
      contentText?: string
      uploadedBy: number
      createdAt: string
      updatedAt: string
    }>(`/projects/${projectId}/requirements/${requirementId}`)
  },
  testCases: {
    list: (projectId: number, params: {
      status?: string
      keyword?: string
      priority?: string
      caseType?: string
      page?: number
      size?: number
    } = {}) => {
      const query = new URLSearchParams()
      if (params.status) query.set('status', params.status)
      if (params.keyword) query.set('keyword', params.keyword)
      if (params.priority) query.set('priority', params.priority)
      if (params.caseType) query.set('caseType', params.caseType)
      query.set('page', String(params.page ?? 0))
      query.set('size', String(params.size ?? 100))
      return request<ApiTestCasePage>(`/projects/${projectId}/test-cases?${query.toString()}`)
    },
    create: (projectId: number, payload: {
      requirementId?: number | null
      caseNo?: string
      title: string
      moduleName?: string
      precondition?: string
      steps?: string
      testData?: string
      expectedResult?: string
      priority: string
      caseType: string
      tags?: string
    }) => request<ApiTestCase>(`/projects/${projectId}/test-cases`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
    update: (projectId: number, caseId: number, payload: {
      requirementId?: number | null
      caseNo?: string
      title: string
      moduleName?: string
      precondition?: string
      steps?: string
      testData?: string
      expectedResult?: string
      priority: string
      caseType: string
      tags?: string
    }) => request<ApiTestCase>(`/projects/${projectId}/test-cases/${caseId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }),
    changeStatus: (projectId: number, caseId: number, status: ApiTestCase['status']) =>
      request<ApiTestCase>(`/projects/${projectId}/test-cases/${caseId}/status`, {
        method: 'POST',
        body: JSON.stringify({ status })
      }),
    remove: (projectId: number, caseId: number) => request<void>(
      `/projects/${projectId}/test-cases/${caseId}`,
      { method: 'DELETE' }
    )
  },
  ai: {
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
    generate: (projectId: number, requirementId: number, payload: {
      modelId?: number
      promptId?: number
      rules?: string
    } = {}) => request<{
      requirementId: number
      modelId: number
      provider: string
      testCases: ApiGeneratedCase[]
    }>(`/projects/${projectId}/requirements/${requirementId}/test-cases/generate`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  automation: {
    projects: (projectId: number) => request<ApiAutomationProject[]>(`/projects/${projectId}/automation-projects`),
    tasks: (projectId: number) => request<ApiTestTask[]>(`/projects/${projectId}/test-tasks`),
    createProject: (projectId: number, payload: {
      name: string
      automationType: string
      repositoryUrl: string
      branch: string
      credentialRef?: string
      framework: string
      testPath?: string
      executeCommand?: string
      environment?: string
      jenkinsJob?: string
    }) => request<ApiAutomationProject>(`/projects/${projectId}/automation-projects`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
    createTask: (projectId: number, payload: {
      name: string
      taskType: string
      automationProjectId: number
      environment: string
      branch: string
      jenkinsJob?: string
      parametersJson?: string
    }) => request<ApiTestTask>(`/projects/${projectId}/test-tasks`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
    executions: (projectId: number) => request<ApiExecution[]>(`/projects/${projectId}/test-executions`),
    execute: (projectId: number, taskId: number) => request<ApiExecution>(
      `/projects/${projectId}/test-tasks/${taskId}/execute`,
      { method: 'POST' }
    ),
    sync: (projectId: number, executionId: number) => request<ApiExecution>(
      `/projects/${projectId}/test-executions/${executionId}/sync`,
      { method: 'POST' }
    )
  }
}
