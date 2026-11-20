import { request } from './client'
import type { ApiJenkinsJob } from './jenkins'
import type { ApiAiTaskSubmit } from './aiTasks'

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

export interface ApiAnalysis {
  executionId: number
  projectId: number
  projectName: string
  testTaskId: number
  taskName: string
  buildNumber?: string
  branch?: string
  commitSha?: string
  environment?: string
  status: string
  category: string
  categoryLabel: string
  possibleCauses: string[]
  relatedErrors: string[]
  checkLocations: string[]
  fixDirections: string[]
  riskWarnings: string[]
  modelName: string
  provider: string
  promptName: string
  promptVersion: string
  analyzedAt: string
  disclaimer: string
  errorMessage?: string
}

export interface ApiOnboardingStatus {
  onboardingId: string
  automationProjectId: number
  status: string
  currentStep?: string
  retryCount: number
  errorCode?: string
  errorMessage?: string
  startedAt?: string
  finishedAt?: string
  steps: Array<{
    id: number
    stepKey: string
    status: string
    inputSummary?: string
    outputSummary?: string
    errorMessage?: string
    retryCount: number
    startedAt?: string
    finishedAt?: string
  }>
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

export interface ApiPipelineBinding {
  id: number
  projectId: number
  automationProjectId: number
  jenkinsInstanceId: number
  pipelineType: 'MULTIBRANCH_PIPELINE' | 'PIPELINE_JOB' | 'FREESTYLE_JOB'
  folderPath?: string
  jobName: string
  jobUrl?: string
  credentialScopeRef?: string
  managedBy: 'PLATFORM' | 'EXTERNAL'
  provisioningStatus: string
  parametersJson?: string
  lastScanAt?: string
  lastError?: string
}

export interface ApiTestTask {
  id: number
  projectId: number
  name: string
  taskType: string
  automationProjectId: number
  pipelineBindingId?: number
  jenkinsJob: string
  environment: string
  branch: string
  parametersJson?: string
  status: string
}

export interface ApiAutomationTaskDraft {
  id: number
  projectId: number
  name: string
  taskType: string
  automationType: string
  repositoryUrl: string
  branch: string
  framework: string
  testPath?: string
  executeCommand?: string
  environment: string
  parametersJson?: string
  pipelineType?: string
  jenkinsJob?: string
  allureEnabled: boolean
  allureConfig?: string
  missingItems: string[]
  riskHints: string[]
  status: string
  modelName: string
  provider: string
  promptName: string
  promptVersion: string
  createdAt: string
  disclaimer: string
}

export interface ApiAutomationTaskConfirm {
  draftId: number
  automationProjectId: number
  testTaskId: number
  task: ApiTestTask
  disclaimer: string
}

export const automation = {
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
  updateProject: (projectId: number, automationProjectId: number, payload: {
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
  }) => request<ApiAutomationProject>(
    `/projects/${projectId}/automation-projects/${automationProjectId}`,
    { method: 'PUT', body: JSON.stringify(payload) }
  ),
  initialize: (projectId: number, automationProjectId: number) =>
    request<{ onboardingId: string; status: string }>(
      `/projects/${projectId}/automation-projects/${automationProjectId}/initialize`,
      { method: 'POST' }
    ),
  onboardingStatus: (projectId: number, automationProjectId: number, onboardingId: string) =>
    request<ApiOnboardingStatus>(
      `/projects/${projectId}/automation-projects/${automationProjectId}/onboarding-status?onboardingId=${encodeURIComponent(onboardingId)}`
    ),
  retryOnboarding: (projectId: number, automationProjectId: number, onboardingId: string) =>
    request<ApiOnboardingStatus>(
      `/projects/${projectId}/automation-projects/${automationProjectId}/onboarding/${onboardingId}/retry`,
      { method: 'POST' }
    ),
  cancelOnboarding: (projectId: number, automationProjectId: number, onboardingId: string) =>
    request<ApiOnboardingStatus>(
      `/projects/${projectId}/automation-projects/${automationProjectId}/onboarding/${onboardingId}/cancel`,
      { method: 'POST' }
    ),
  bindings: (projectId: number, automationProjectId: number) =>
    request<ApiPipelineBinding[]>(
      `/projects/${projectId}/automation-projects/${automationProjectId}/pipeline-bindings`
    ),
  createBinding: (projectId: number, automationProjectId: number, payload: {
    jenkinsInstanceId: number
    pipelineType: string
    folderPath?: string
    jobName: string
    jobUrl?: string
    credentialScopeRef?: string
    managedBy?: string
    parametersJson?: string
  }) => request<ApiPipelineBinding>(
    `/projects/${projectId}/automation-projects/${automationProjectId}/pipeline-bindings`,
    { method: 'POST', body: JSON.stringify(payload) }
  ),
  updateBinding: (projectId: number, bindingId: number, payload: {
    jenkinsInstanceId: number
    pipelineType: string
    folderPath?: string
    jobName: string
    jobUrl?: string
    credentialScopeRef?: string
    managedBy?: string
    parametersJson?: string
  }) => request<ApiPipelineBinding>(
    `/projects/${projectId}/pipeline-bindings/${bindingId}`,
    { method: 'PUT', body: JSON.stringify(payload) }
  ),
  listJobs: (projectId: number, automationProjectId: number, jenkinsInstanceId: number) =>
    request<ApiJenkinsJob[]>(
      `/projects/${projectId}/automation-projects/${automationProjectId}/jenkins-jobs?jenkinsInstanceId=${jenkinsInstanceId}`
    ),
  importJob: (projectId: number, automationProjectId: number, payload: {
    jenkinsInstanceId: number
    jobName: string
    credentialScopeRef?: string
  }) => request<ApiPipelineBinding>(
    `/projects/${projectId}/automation-projects/${automationProjectId}/jenkins-jobs/import`,
    { method: 'POST', body: JSON.stringify(payload) }
  ),
  createJob: (projectId: number, automationProjectId: number, payload: {
    jenkinsInstanceId: number
    jobName: string
    pipelineType: 'PIPELINE_JOB' | 'FREESTYLE_JOB'
    scriptSource?: 'TEMPLATE' | 'CUSTOM'
    customScript?: string
    credentialScopeRef?: string
  }) => request<ApiPipelineBinding>(
    `/projects/${projectId}/automation-projects/${automationProjectId}/jenkins-jobs/create`,
    { method: 'POST', body: JSON.stringify(payload) }
  ),
  createTask: (projectId: number, payload: {
    name: string
    taskType: string
    automationProjectId: number
    pipelineBindingId?: number
    environment: string
    branch: string
    parametersJson?: string
  }) => request<ApiTestTask>(`/projects/${projectId}/test-tasks`, {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  updateTask: (projectId: number, taskId: number, payload: {
    name: string
    taskType: string
    automationProjectId: number
    pipelineBindingId?: number
    environment: string
    branch: string
    parametersJson?: string
  }) => request<ApiTestTask>(`/projects/${projectId}/test-tasks/${taskId}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  }),
  deleteTask: (projectId: number, taskId: number) => request<void>(
    `/projects/${projectId}/test-tasks/${taskId}`,
    { method: 'DELETE' }
  ),
  updateTaskStatus: (projectId: number, taskId: number, status: string) =>
    request<ApiTestTask>(`/projects/${projectId}/test-tasks/${taskId}/status`, {
      method: 'POST',
      body: JSON.stringify({ status })
    }),
  aiGenerateTask: (projectId: number, payload: {
    repositoryUrl: string
    branch: string
    description: string
    modelId?: number
    promptId?: number
  }) => request<ApiAiTaskSubmit>(`/projects/${projectId}/automation-tasks/ai-generate`, {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  updateAiDraft: (projectId: number, draftId: number, payload: {
    name: string
    taskType: string
    automationType: string
    repositoryUrl: string
    branch: string
    framework: string
    testPath?: string
    executeCommand?: string
    environment: string
    parametersJson?: string
    pipelineType?: string
    jenkinsJob?: string
    allureEnabled: boolean
  }) => request<ApiAutomationTaskDraft>(
    `/projects/${projectId}/automation-tasks/ai-drafts/${draftId}`,
    { method: 'PUT', body: JSON.stringify(payload) }
  ),
  confirmAiDraft: (projectId: number, draftId: number) =>
    request<ApiAutomationTaskConfirm>(
      `/projects/${projectId}/automation-tasks/ai-drafts/${draftId}/confirm`,
      { method: 'POST' }
    ),
  executions: (projectId: number) => request<ApiExecution[]>(`/projects/${projectId}/test-executions`),
  deleteExecution: (projectId: number, executionId: number) => request<void>(
    `/projects/${projectId}/test-executions/${executionId}`,
    { method: 'DELETE' }
  ),
  execute: (projectId: number, taskId: number, payload: { parametersJson?: string } = {}) =>
    request<ApiExecution>(
      `/projects/${projectId}/test-tasks/${taskId}/execute`,
      { method: 'POST', body: JSON.stringify(payload) }
    ),
  stopTask: (projectId: number, taskId: number) => request<ApiExecution>(
    `/projects/${projectId}/test-tasks/${taskId}/stop`,
    { method: 'POST' }
  ),
  sync: (projectId: number, executionId: number) => request<ApiExecution>(
    `/projects/${projectId}/test-executions/${executionId}/sync`,
    { method: 'POST' }
  ),
  retry: (projectId: number, executionId: number) => request<ApiExecution>(
    `/projects/${projectId}/test-executions/${executionId}/retry`,
    { method: 'POST' }
  ),
  analyze: (projectId: number, executionId: number, payload: {
    modelId?: number
    promptId?: number
  } = {}) => request<ApiAiTaskSubmit>(
    `/projects/${projectId}/test-executions/${executionId}/analyze`,
    { method: 'POST', body: JSON.stringify(payload) }
  ),
  consoleLog: (projectId: number, executionId: number) =>
    request<string>(`/projects/${projectId}/test-executions/${executionId}/console-log`),
  allureAccess: (executionId: number) => request<{
    viewUrl: string
    downloadUrl: string
    expiresInSeconds: string
  }>(`/reports/executions/${executionId}/allure-access`, {
    method: 'POST'
  })
}
