import { request } from './client'

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

export const testCases = {
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
}
