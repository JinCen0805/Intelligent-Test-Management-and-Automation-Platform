export type View = 'home' | 'projects' | 'project' | 'ai' | 'reports' | 'admin'

export type ProjectTab =
  | 'overview'
  | 'requirements'
  | 'case-ai'
  | 'case-library'
  | 'api-workbench'
  | 'api-code'
  | 'ui'
  | 'tasks'
  | 'reports'
  | 'settings'

export type ModalName =
  | 'project'
  | 'requirement'
  | 'requirementDetail'
  | 'run'
  | 'caseEdit'
  | 'analysis'
  | 'model'
  | 'prompt'
  | 'automationProject'
  | 'task'
  | 'aiTask'
  | 'user'
  | 'member'
  | 'jenkinsInstance'
  | 'binding'
  | 'jobImport'
  | 'createJob'
  | 'onboarding'
  | 'executionDetail'
  | 'caseChatHistory'

export interface Project {
  id?: number
  name: string
  code: string
  ownerId?: number
  owner: string
  members: number
  status: '运行中' | '测试中'
  description: string
  createdAt?: string
}

export interface TestCase {
  id: number
  requirementId?: number | null
  no: string
  title: string
  module: string
  priority: 'P0' | 'P1' | 'P2' | 'P3'
  type: '功能' | '异常' | '边界' | '其他'
  suiteType: '功能测试' | '接口测试'
  status: 'DRAFT' | 'REVIEW' | 'APPROVED' | 'REJECTED'
  source: string
  precondition: string
  steps: string
  testData: string
  expectedResult: string
  tags: string
  extraFields: Record<string, unknown>
}

export interface Requirement {
  id?: number
  name: string
  version: string
  type: string
  status: '已存储' | '已解析' | '解析中' | '解析失败'
  updated: string
}

export interface RequirementDetail {
  name: string
  version: string
  fileType: string
  parseStatus: string
  parseError?: string
  contentText?: string
}

export interface Execution {
  run: string
  project: string
  task: string
  type: string
  branch: string
  environment: string
  status: 'PASSED' | 'FAILED' | 'RUNNING' | 'TIMEOUT'
  passRate: string
  time: string
}
