import { request } from './client'
import type { ApiExecution } from './automation'

export interface ApiReportExecution extends ApiExecution {
  projectName: string
  taskName: string
}

export interface ApiExecutionDetail {
  id: number
  projectId: number
  projectName: string
  testTaskId: number
  taskName: string
  taskType: string
  automationProjectId: number
  automationProjectName: string
  jenkinsJob?: string
  queueUrl?: string
  buildNumber?: string
  commitSha?: string
  branch?: string
  environment?: string
  status: string
  startedAt?: string
  finishedAt?: string
  durationMs?: number
  consoleLogRef?: string
  syncFailureMessage?: string
  syncFailureCount?: number
  triggeredBy?: string
  triggerMethod?: string
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
    failureDetail?: string
    screenshotRef?: string
    videoRef?: string
    aiAnalysis?: string
  }
}

export interface ApiReportTrend {
  date: string
  total: number
  passed: number
  failed: number
  broken: number
  skipped: number
  passRate: number
}

export interface ApiReportCenter {
  content: ApiReportExecution[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  summary: {
    total: number
    passed: number
    failed: number
    broken: number
    skipped: number
    executionCount: number
  }
  trend: ApiReportTrend[]
}

export const reports = {
  center: (params: {
    projectId?: number | null
    status?: string
    from?: string
    to?: string
    page?: number
    size?: number
  } = {}) => {
    const query = new URLSearchParams()
    if (params.projectId) query.set('projectId', String(params.projectId))
    if (params.status) query.set('status', params.status)
    if (params.from) query.set('from', params.from)
    if (params.to) query.set('to', params.to)
    query.set('page', String(params.page ?? 0))
    query.set('size', String(params.size ?? 20))
    return request<ApiReportCenter>(`/reports?${query.toString()}`)
  },
  detail: (executionId: number) => request<ApiExecutionDetail>(`/reports/executions/${executionId}`)
}
