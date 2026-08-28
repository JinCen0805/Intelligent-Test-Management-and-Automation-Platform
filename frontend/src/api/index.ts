import { auth } from './auth'
import { projects } from './project'
import { users } from './user'
import { requirements } from './requirement'
import { testCases } from './testcase'
import { ai } from './ai'
import { aiTasks } from './aiTasks'
import { automation } from './automation'
import { jenkins } from './jenkins'
import { apiWorkbench } from './apiWorkbench'
import { reports } from './report'

export const api = {
  auth,
  projects,
  users,
  requirements,
  testCases,
  ai,
  aiTasks,
  automation,
  jenkins,
  apiWorkbench,
  reports
}

export type { ApiResponse } from './client'
export type { ApiUser } from './user'
export type { ApiProject, ApiProjectMember } from './project'
export type { ApiRequirement } from './requirement'
export type { ApiTestCase, ApiTestCasePage } from './testcase'
export type {
  ApiGeneratedCase,
  ApiAiModel,
  ApiPromptTemplate,
  ApiCaseChatMessage,
  ApiCaseChatResponse,
  ApiCaseChatSession,
  ApiAiCallAudit
} from './ai'
export type { ApiAiTaskSubmit, ApiAiTaskStatus } from './aiTasks'
export type {
  ApiExecution,
  ApiAnalysis,
  ApiOnboardingStatus,
  ApiAutomationProject,
  ApiPipelineBinding,
  ApiTestTask,
  ApiAutomationTaskDraft,
  ApiAutomationTaskConfirm
} from './automation'
export type { ApiJenkinsInstance, ApiJenkinsJob, ApiCredentialGrant } from './jenkins'
export type {
  ApiCollection,
  ApiWorkbenchRequest,
  ApiEnvironmentVariable,
  ApiRequestHistory,
  ApiRequestHistoryPage,
  ApiCollectionExecution
} from './apiWorkbench'
export type { ApiReportExecution, ApiExecutionDetail, ApiReportTrend, ApiReportCenter } from './report'
