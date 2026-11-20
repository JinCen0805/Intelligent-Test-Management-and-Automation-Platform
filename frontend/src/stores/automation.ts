import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/api'
import type {
  ApiAutomationProject,
  ApiAutomationTaskConfirm,
  ApiAutomationTaskDraft,
  ApiExecution,
  ApiJenkinsJob,
  ApiOnboardingStatus,
  ApiPipelineBinding,
  ApiTestTask
} from '@/api'
import type { Execution } from '@/types'
import { useUiStore } from './ui'
import { useProjectStore } from './project'
import { useReportStore } from './report'
import { pollAiTask } from '@/utils/pollAiTask'

const ONBOARDING_STEPS: { key: string; label: string; role: string }[] = [
  { key: 'REPOSITORY_VALIDATE', label: '仓库校验', role: '平台' },
  { key: 'FRAMEWORK_DETECT', label: '框架识别', role: '平台' },
  { key: 'JENKINSFILE_CHECK', label: 'Jenkinsfile 检查', role: '平台' },
  { key: 'PIPELINE_BIND_OR_PROVISION', label: 'Pipeline 绑定/创建', role: '项目管理员' },
  { key: 'BRANCH_SCAN', label: '分支扫描', role: 'Jenkins' },
  { key: 'AGENT_PRECHECK', label: 'Agent 预检', role: 'Jenkins Agent' },
  { key: 'DEFAULT_TASK_CREATE', label: '默认任务创建', role: '平台' }
]

export const useAutomationStore = defineStore('automation', () => {
  const ui = useUiStore()
  const projectStore = useProjectStore()

  const automationProjects = ref<ApiAutomationProject[]>([])
  const automationTasks = ref<ApiTestTask[]>([])
  const executableTasks = computed(() => automationTasks.value.filter((task) => task.status === 'ENABLED'))
  const executionRecords = ref<ApiExecution[]>([])
  const executions = computed<Execution[]>(() => executionRecords.value.map(fromApiExecution))

  const runForm = ref({ taskId: null as number | null, parametersJson: '' })
  const automationProjectForm = ref({
    name: '',
    automationType: 'API',
    repositoryUrl: '',
    branch: 'main',
    credentialRef: '',
    framework: 'pytest',
    testPath: '',
    executeCommand: '',
    environment: 'TEST',
    jenkinsJob: ''
  })
  const editingAutomationProjectId = ref<number | null>(null)
  const taskForm = ref({
    name: '',
    taskType: 'REGRESSION',
    automationProjectId: null as number | null,
    pipelineBindingId: null as number | null,
    environment: 'TEST',
    branch: 'main',
    parametersJson: ''
  })
  const editingTaskId = ref<number | null>(null)
  const pipelineBindings = ref<ApiPipelineBinding[]>([])
  const taskBindings = ref<ApiPipelineBinding[]>([])
  const jenkinsJobs = ref<ApiJenkinsJob[]>([])
  const bindingAutomationProjectId = ref<number | null>(null)
  const editingBindingId = ref<number | null>(null)
  const bindingFormVisible = ref(false)
  const bindingForm = ref({
    jenkinsInstanceId: null as number | null,
    pipelineType: 'PIPELINE_JOB',
    folderPath: '',
    jobName: '',
    jobUrl: '',
    credentialScopeRef: '',
    managedBy: 'EXTERNAL'
  })
  const jobImportForm = ref({ jenkinsInstanceId: null as number | null })
  const createJobForm = ref({
    jenkinsInstanceId: null as number | null,
    jobName: '',
    pipelineType: 'PIPELINE_JOB' as 'PIPELINE_JOB' | 'FREESTYLE_JOB',
    scriptSource: 'TEMPLATE' as 'TEMPLATE' | 'CUSTOM',
    customScript: '',
    credentialScopeRef: ''
  })

  const onboardingAutomationProjectId = ref<number | null>(null)
  const onboarding = ref<ApiOnboardingStatus | null>(null)
  const onboardingLoading = ref(false)
  let onboardingTimer: number | undefined

  const aiTaskForm = ref({ repositoryUrl: '', branch: 'main', description: '' })
  const aiDraftForm = ref({
    id: null as number | null,
    name: '',
    taskType: 'REGRESSION',
    automationType: 'API',
    repositoryUrl: '',
    branch: 'main',
    framework: 'PYTEST',
    testPath: '',
    executeCommand: '',
    environment: 'TEST',
    parametersJson: '',
    pipelineType: 'FREESTYLE',
    jenkinsJob: '',
    allureEnabled: true,
    allureConfig: '',
    missingItems: [] as string[],
    riskHints: [] as string[],
    modelName: '',
    provider: '',
    promptName: '',
    promptVersion: '',
    disclaimer: ''
  })
  const aiTaskLoading = ref(false)
  const aiTaskConfirmResult = ref<ApiAutomationTaskConfirm | null>(null)
  let stopAiPoll: (() => void) | undefined

  const onboardingTotalSteps = computed(() => onboarding.value?.steps.length ?? ONBOARDING_STEPS.length)
  const onboardingReadySteps = computed(() =>
    (onboarding.value?.steps ?? []).filter((step) => step.status === 'READY').length
  )
  const onboardingProgress = computed(() => {
    const total = onboardingTotalSteps.value
    if (!total) return 0
    return Math.round(onboardingReadySteps.value / total * 100)
  })

  function onboardingStatusLabel(status: string) {
    return ({
      CREATED: '待开始', QUEUED: '排队中', RUNNING: '进行中',
      READY: '已完成', FAILED: '失败', CANCELED: '已取消'
    } as Record<string, string>)[status] ?? status
  }

  function onboardingStepStatusLabel(status: string) {
    return ({
      PENDING: '待处理', RUNNING: '进行中', READY: '完成', FAILED: '失败'
    } as Record<string, string>)[status] ?? status
  }

  function onboardingStepMeta(stepKey: string) {
    return ONBOARDING_STEPS.find((item) => item.key === stepKey)
      ?? { key: stepKey, label: stepKey, role: '平台' }
  }

  function stepDuration(step: { startedAt?: string; finishedAt?: string }) {
    if (!step.startedAt || !step.finishedAt) return '--'
    const ms = new Date(step.finishedAt).getTime() - new Date(step.startedAt).getTime()
    return ms >= 1000 ? `${(ms / 1000).toFixed(1)}s` : `${ms}ms`
  }

  function fromApiExecution(execution: ApiExecution): Execution {
    const total = execution.report?.totalCount || 0
    const passed = execution.report?.passedCount || 0
    const task = automationTasks.value.find((item) => item.id === execution.testTaskId)
    const automationProject = automationProjects.value.find((item) => item.id === execution.automationProjectId)
    return {
      run: execution.buildNumber ? `Build ${execution.buildNumber}` : `Execution #${execution.id}`,
      project: projectStore.currentProject.name,
      task: task?.name || '测试任务',
      type: task?.taskType || '自动化',
      branch: automationProject?.branch || '--',
      environment: automationProject?.environment || '--',
      status: execution.status === 'PASSED'
        ? 'PASSED'
        : execution.status === 'FAILED'
          ? 'FAILED'
          : execution.status === 'TIMEOUT'
            ? 'TIMEOUT'
            : 'RUNNING',
      passRate: total > 0 ? `${Math.round((passed / total) * 1000) / 10}%` : '--',
      time: execution.startedAt ? new Date(execution.startedAt).toLocaleString('zh-CN') : '刚刚'
    }
  }

  async function loadAutomationData() {
    const projectId = projectStore.currentProject.id
    if (!projectId) return
    const [projectsList, tasks, executionList] = await Promise.all([
      api.automation.projects(projectId),
      api.automation.tasks(projectId),
      api.automation.executions(projectId)
    ])
    automationProjects.value.splice(0, automationProjects.value.length, ...projectsList)
    automationTasks.value.splice(0, automationTasks.value.length, ...tasks)
    executionRecords.value.splice(0, executionRecords.value.length, ...executionList)
    if (runForm.value.taskId === null && executableTasks.value.length > 0) {
      runForm.value.taskId = executableTasks.value[0].id
    }
  }

  function openNewAutomationProject(automationType: 'API' | 'UI' = 'API') {
    editingAutomationProjectId.value = null
    Object.assign(automationProjectForm.value, {
      name: '',
      automationType,
      repositoryUrl: '',
      branch: 'main',
      credentialRef: '',
      framework: 'pytest',
      testPath: '',
      executeCommand: '',
      environment: 'TEST',
      jenkinsJob: ''
    })
    ui.openModal('automationProject')
  }

  function editAutomationProject(project: ApiAutomationProject) {
    editingAutomationProjectId.value = project.id
    Object.assign(automationProjectForm.value, {
      name: project.name,
      automationType: project.automationType,
      repositoryUrl: project.repositoryUrl,
      branch: project.branch,
      credentialRef: project.credentialRef || '',
      framework: project.framework,
      testPath: project.testPath || '',
      executeCommand: project.executeCommand || '',
      environment: project.environment || 'TEST',
      jenkinsJob: project.jenkinsJob || ''
    })
    ui.openModal('automationProject')
  }

  async function createAutomationProject() {
    const projectId = projectStore.currentProject.id
    if (!projectId || !automationProjectForm.value.name.trim() || !automationProjectForm.value.repositoryUrl.trim()) {
      ui.showToast('请填写自动化项目名称和 Git 仓库地址')
      return
    }
    try {
      const payload = { ...automationProjectForm.value }
      const saved = editingAutomationProjectId.value
        ? await api.automation.updateProject(projectId, editingAutomationProjectId.value, payload)
        : await api.automation.createProject(projectId, payload)
      const index = automationProjects.value.findIndex((item) => item.id === saved.id)
      if (index >= 0) {
        automationProjects.value[index] = saved
      } else {
        automationProjects.value.unshift(saved)
      }
      const wasEditing = Boolean(editingAutomationProjectId.value)
      editingAutomationProjectId.value = null
      Object.assign(automationProjectForm.value, {
        name: '',
        automationType: 'API',
        repositoryUrl: '',
        branch: 'main',
        credentialRef: '',
        framework: 'pytest',
        testPath: '',
        executeCommand: '',
        environment: 'TEST',
        jenkinsJob: ''
      })
      ui.closeModal()
      ui.showToast(wasEditing ? '自动化项目已更新' : '自动化项目已创建')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '自动化项目创建失败')
    }
  }

  function resetTaskForm() {
    Object.assign(taskForm.value, {
      name: '',
      taskType: 'REGRESSION',
      automationProjectId: null,
      pipelineBindingId: null,
      environment: 'TEST',
      branch: 'main',
      parametersJson: ''
    })
    taskBindings.value.splice(0, taskBindings.value.length)
  }

  function openNewTask() {
    editingTaskId.value = null
    resetTaskForm()
    ui.openModal('task')
  }

  function editTask(task: ApiTestTask) {
    editingTaskId.value = task.id
    Object.assign(taskForm.value, {
      name: task.name,
      taskType: task.taskType,
      automationProjectId: task.automationProjectId,
      pipelineBindingId: task.pipelineBindingId ?? null,
      environment: task.environment || 'TEST',
      branch: task.branch || 'main',
      parametersJson: task.parametersJson || ''
    })
    onTaskAutomationProjectChange()
    ui.openModal('task')
  }

  async function saveTask() {
    const projectId = projectStore.currentProject.id
    if (!projectId || !taskForm.value.name.trim() || !taskForm.value.automationProjectId) {
      ui.showToast('请填写任务名称并选择自动化项目')
      return
    }
    const wasEditing = Boolean(editingTaskId.value)
    try {
      const payload = {
        ...taskForm.value,
        automationProjectId: taskForm.value.automationProjectId as number,
        pipelineBindingId: taskForm.value.pipelineBindingId || undefined
      }
      const saved = editingTaskId.value
        ? await api.automation.updateTask(projectId, editingTaskId.value, payload)
        : await api.automation.createTask(projectId, payload)
      if (editingTaskId.value) {
        const index = automationTasks.value.findIndex((item) => item.id === editingTaskId.value)
        if (index >= 0) automationTasks.value.splice(index, 1, saved)
      } else {
        automationTasks.value.unshift(saved)
        runForm.value.taskId = saved.id
      }
      editingTaskId.value = null
      ui.closeModal()
      ui.showToast(wasEditing ? '测试任务已更新' : '测试任务已创建')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '测试任务保存失败')
    }
  }

  async function deleteTask(task: ApiTestTask) {
    const projectId = projectStore.currentProject.id
    if (!projectId) return
    if (!window.confirm(`确认删除测试任务「${task.name}」？`)) return
    try {
      await api.automation.deleteTask(projectId, task.id)
      const index = automationTasks.value.findIndex((item) => item.id === task.id)
      if (index >= 0) automationTasks.value.splice(index, 1)
      ui.showToast('测试任务已删除')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '测试任务删除失败')
    }
  }

  async function toggleTaskStatus(task: ApiTestTask) {
    const projectId = projectStore.currentProject.id
    if (!projectId) return
    const next = task.status === 'DISABLED' ? 'ENABLED' : 'DISABLED'
    try {
      const updated = await api.automation.updateTaskStatus(projectId, task.id, next)
      const index = automationTasks.value.findIndex((item) => item.id === task.id)
      if (index >= 0) automationTasks.value.splice(index, 1, updated)
      ui.showToast(next === 'DISABLED' ? '测试任务已禁用' : '测试任务已启用')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '任务状态更新失败')
    }
  }

  async function stopTask(task: ApiTestTask) {
    const projectId = projectStore.currentProject.id
    if (!projectId) return
    try {
      await api.automation.stopTask(projectId, task.id)
      await loadAutomationData()
      ui.showToast('已停止该任务进行中的执行')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '停止任务失败')
    }
  }

  function taskActiveExecution(task: ApiTestTask) {
    return executionRecords.value.find(
      (execution) => execution.testTaskId === task.id && ['QUEUED', 'RUNNING'].includes(execution.status)
    )
  }

  function bindingProject() {
    return automationProjects.value.find((item) => item.id === bindingAutomationProjectId.value) || null
  }

  async function openBindingManager(automationProject: ApiAutomationProject) {
    bindingAutomationProjectId.value = automationProject.id
    resetBindingForm()
    bindingFormVisible.value = false
    await loadBindings()
    ui.openModal('binding')
  }

  async function loadBindings() {
    const projectId = projectStore.currentProject.id
    if (!projectId || !bindingAutomationProjectId.value) return
    try {
      const list = await api.automation.bindings(projectId, bindingAutomationProjectId.value)
      pipelineBindings.value.splice(0, pipelineBindings.value.length, ...list)
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '执行目标加载失败')
    }
  }

  function resetBindingForm() {
    editingBindingId.value = null
    Object.assign(bindingForm.value, {
      jenkinsInstanceId: null,
      pipelineType: 'PIPELINE_JOB',
      folderPath: '',
      jobName: '',
      jobUrl: '',
      credentialScopeRef: '',
      managedBy: 'EXTERNAL'
    })
  }

  function editBinding(binding: ApiPipelineBinding) {
    editingBindingId.value = binding.id
    Object.assign(bindingForm.value, {
      jenkinsInstanceId: binding.jenkinsInstanceId,
      pipelineType: binding.pipelineType,
      folderPath: binding.folderPath || '',
      jobName: binding.jobName,
      jobUrl: binding.jobUrl || '',
      credentialScopeRef: binding.credentialScopeRef || '',
      managedBy: binding.managedBy
    })
    bindingFormVisible.value = true
  }

  function openNewBinding() {
    resetBindingForm()
    bindingFormVisible.value = true
  }

  function closeBindingForm() {
    bindingFormVisible.value = false
  }

  async function saveBinding() {
    const projectId = projectStore.currentProject.id
    if (!projectId || !bindingAutomationProjectId.value) return
    if (!bindingForm.value.jenkinsInstanceId || !bindingForm.value.jobName.trim()) {
      ui.showToast('请选择 Jenkins 实例并填写 Job 名称')
      return
    }
    try {
      const payload = {
        jenkinsInstanceId: bindingForm.value.jenkinsInstanceId as number,
        pipelineType: bindingForm.value.pipelineType,
        folderPath: bindingForm.value.folderPath || undefined,
        jobName: bindingForm.value.jobName.trim(),
        jobUrl: bindingForm.value.jobUrl || undefined,
        credentialScopeRef: bindingForm.value.credentialScopeRef || undefined,
        managedBy: bindingForm.value.managedBy
      }
      if (editingBindingId.value) {
        await api.automation.updateBinding(projectId, editingBindingId.value, payload)
      } else {
        await api.automation.createBinding(projectId, bindingAutomationProjectId.value, payload)
      }
      resetBindingForm()
      bindingFormVisible.value = false
      await loadBindings()
      ui.showToast('执行目标已保存')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '执行目标保存失败')
    }
  }

  function openJobImport() {
    Object.assign(jobImportForm.value, { jenkinsInstanceId: null })
    jenkinsJobs.value.splice(0, jenkinsJobs.value.length)
    ui.openModal('jobImport')
  }

  async function loadJenkinsJobs() {
    const projectId = projectStore.currentProject.id
    if (!projectId || !bindingAutomationProjectId.value || !jobImportForm.value.jenkinsInstanceId) {
      jenkinsJobs.value.splice(0, jenkinsJobs.value.length)
      return
    }
    try {
      const list = await api.automation.listJobs(
        projectId,
        bindingAutomationProjectId.value,
        jobImportForm.value.jenkinsInstanceId
      )
      jenkinsJobs.value.splice(0, jenkinsJobs.value.length, ...list)
    } catch (error) {
      jenkinsJobs.value.splice(0, jenkinsJobs.value.length)
      ui.showToast(error instanceof Error ? error.message : 'Jenkins Job 列表加载失败')
    }
  }

  async function importJenkinsJob(job: ApiJenkinsJob) {
    const projectId = projectStore.currentProject.id
    if (!projectId || !bindingAutomationProjectId.value || !jobImportForm.value.jenkinsInstanceId) return
    try {
      await api.automation.importJob(projectId, bindingAutomationProjectId.value, {
        jenkinsInstanceId: jobImportForm.value.jenkinsInstanceId,
        jobName: job.path
      })
      await loadJenkinsJobs()
      await loadBindings()
      ui.showToast('已导入 Job 为执行目标')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : 'Job 导入失败')
    }
  }

  function openCreateJob() {
    Object.assign(createJobForm.value, {
      jenkinsInstanceId: null,
      jobName: '',
      pipelineType: 'PIPELINE_JOB',
      scriptSource: 'TEMPLATE',
      customScript: '',
      credentialScopeRef: ''
    })
    ui.openModal('createJob')
  }

  async function createJenkinsJob() {
    const projectId = projectStore.currentProject.id
    if (!projectId || !bindingAutomationProjectId.value) return
    if (!createJobForm.value.jenkinsInstanceId || !createJobForm.value.jobName.trim()) {
      ui.showToast('请选择 Jenkins 实例并填写 Job 名称')
      return
    }
    if (
      createJobForm.value.pipelineType === 'PIPELINE_JOB'
      && createJobForm.value.scriptSource === 'CUSTOM'
      && !createJobForm.value.customScript.trim()
    ) {
      ui.showToast('请粘贴 Jenkinsfile 脚本')
      return
    }
    try {
      await api.automation.createJob(projectId, bindingAutomationProjectId.value, {
        jenkinsInstanceId: createJobForm.value.jenkinsInstanceId,
        jobName: createJobForm.value.jobName.trim(),
        pipelineType: createJobForm.value.pipelineType,
        scriptSource: createJobForm.value.scriptSource,
        customScript: createJobForm.value.customScript.trim() || undefined,
        credentialScopeRef: createJobForm.value.credentialScopeRef.trim() || undefined
      })
      await loadBindings()
      ui.openModal('binding')
      ui.showToast('Jenkins Job 已创建并绑定')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : 'Jenkins Job 创建失败')
    }
  }

  async function onTaskAutomationProjectChange() {
    taskBindings.value.splice(0, taskBindings.value.length)
    taskForm.value.pipelineBindingId = null
    const projectId = projectStore.currentProject.id
    if (!projectId || !taskForm.value.automationProjectId) return
    try {
      const list = await api.automation.bindings(projectId, taskForm.value.automationProjectId)
      taskBindings.value.splice(0, taskBindings.value.length, ...list)
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '执行目标加载失败')
    }
  }

  function syncRunFormFromTask() {
    const task = automationTasks.value.find((item) => item.id === runForm.value.taskId)
    if (task) {
      runForm.value.parametersJson = task.parametersJson || ''
    }
  }

  function openRunModal(task?: ApiTestTask) {
    runForm.value.taskId = task?.id ?? executableTasks.value[0]?.id ?? null
    syncRunFormFromTask()
    ui.openModal('run')
  }

  async function runTest() {
    const projectId = projectStore.currentProject.id
    const task = executableTasks.value.find((item) => item.id === runForm.value.taskId) || executableTasks.value[0]
    if (!projectId || !task) {
      ui.showToast('当前项目还没有可执行的自动化任务')
      return
    }
    try {
      const execution = await api.automation.execute(projectId, task.id, {
        parametersJson: runForm.value.parametersJson.trim() || undefined
      })
      await loadAutomationData()
      ui.closeModal()
      ui.showToast(`已触发 Jenkins Build ${execution.buildNumber || execution.id}`)
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '执行任务失败')
    }
  }

  async function syncExecution(execution: ApiExecution) {
    const projectId = execution.projectId
    if (!projectId) return
    try {
      await api.automation.sync(projectId, execution.id)
      await Promise.all([loadAutomationData(), useReportStore().loadReportCenter()])
      ui.showToast('执行状态已同步')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '执行状态同步失败')
    }
  }

  async function retryExecution(execution: ApiExecution) {
    const projectId = execution.projectId
    if (!projectId) return
    try {
      await api.automation.retry(projectId, execution.id)
      await loadAutomationData()
      ui.showToast('已重新触发 Jenkins 执行')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '执行重试失败')
    }
  }

  async function deleteExecution(execution: ApiExecution) {
    const projectId = execution.projectId
    if (!projectId) return
    if (!window.confirm(`确认删除执行记录 #${execution.id}？关联报告与离线 Allure 文件将一并删除。`)) return
    try {
      await api.automation.deleteExecution(projectId, execution.id)
      await Promise.all([loadAutomationData(), useReportStore().loadReportCenter()])
      ui.showToast('执行记录已删除')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '执行记录删除失败')
    }
  }

  function onboardingProject() {
    return automationProjects.value.find((item) => item.id === onboardingAutomationProjectId.value) || null
  }

  async function openOnboarding(automationProject: ApiAutomationProject) {
    onboardingAutomationProjectId.value = automationProject.id
    onboarding.value = null
    ui.openModal('onboarding')
    await startOnboarding()
  }

  async function startOnboarding() {
    const projectId = projectStore.currentProject.id
    const automationProjectId = onboardingAutomationProjectId.value
    if (!projectId || !automationProjectId) return
    onboardingLoading.value = true
    try {
      const started = await api.automation.initialize(projectId, automationProjectId)
      const status = await api.automation.onboardingStatus(projectId, automationProjectId, started.onboardingId)
      onboarding.value = status
      if (!['READY', 'FAILED', 'CANCELED'].includes(status.status)) {
        startOnboardingPolling()
      }
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '初始化启动失败')
    } finally {
      onboardingLoading.value = false
    }
  }

  async function pollOnboarding() {
    const projectId = projectStore.currentProject.id
    const automationProjectId = onboardingAutomationProjectId.value
    if (!projectId || !automationProjectId || !onboarding.value) return
    const onboardingId = onboarding.value.onboardingId
    try {
      const status = await api.automation.onboardingStatus(projectId, automationProjectId, onboardingId)
      onboarding.value = status
      if (['READY', 'FAILED', 'CANCELED'].includes(status.status)) {
        stopOnboardingPolling()
      }
    } catch (error) {
      stopOnboardingPolling()
      ui.showToast(error instanceof Error ? error.message : '初始化状态查询失败')
    }
  }

  function startOnboardingPolling() {
    stopOnboardingPolling()
    onboardingTimer = window.setInterval(() => { void pollOnboarding() }, 1000)
  }

  function stopOnboardingPolling() {
    if (onboardingTimer !== undefined) {
      window.clearInterval(onboardingTimer)
      onboardingTimer = undefined
    }
  }

  async function retryOnboarding() {
    const projectId = projectStore.currentProject.id
    const automationProjectId = onboardingAutomationProjectId.value
    if (!projectId || !automationProjectId || !onboarding.value) return
    onboardingLoading.value = true
    try {
      const status = await api.automation.retryOnboarding(
        projectId, automationProjectId, onboarding.value.onboardingId
      )
      onboarding.value = status
      startOnboardingPolling()
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '重试失败')
    } finally {
      onboardingLoading.value = false
    }
  }

  async function cancelOnboarding() {
    const projectId = projectStore.currentProject.id
    const automationProjectId = onboardingAutomationProjectId.value
    if (!projectId || !automationProjectId || !onboarding.value) return
    try {
      const status = await api.automation.cancelOnboarding(
        projectId, automationProjectId, onboarding.value.onboardingId
      )
      onboarding.value = status
      stopOnboardingPolling()
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '初始化取消失败')
    }
  }

  function openAiTask() {
    aiTaskConfirmResult.value = null
    aiTaskLoading.value = false
    // 已生成过草稿时保留草稿（重开弹窗即见「编辑确认」步骤），仅重置描述表单。
    if (aiDraftForm.value.id == null) {
      resetAiDraftForm()
    }
    Object.assign(aiTaskForm.value, { repositoryUrl: '', branch: 'main', description: '' })
    ui.openModal('aiTask')
  }

  function resetAiTask() {
    aiTaskConfirmResult.value = null
    aiTaskLoading.value = false
    resetAiDraftForm()
    Object.assign(aiTaskForm.value, { repositoryUrl: '', branch: 'main', description: '' })
  }

  function resetAiDraftForm() {
    Object.assign(aiDraftForm.value, {
      id: null,
      name: '',
      taskType: 'REGRESSION',
      automationType: 'API',
      repositoryUrl: '',
      branch: 'main',
      framework: 'PYTEST',
      testPath: '',
      executeCommand: '',
      environment: 'TEST',
      parametersJson: '',
      pipelineType: 'FREESTYLE',
      jenkinsJob: '',
      allureEnabled: true,
      allureConfig: '',
      missingItems: [],
      riskHints: [],
      modelName: '',
      provider: '',
      promptName: '',
      promptVersion: '',
      disclaimer: ''
    })
  }

  function fillAiDraftForm(draft: ApiAutomationTaskDraft) {
    Object.assign(aiDraftForm.value, {
      id: draft.id,
      name: draft.name,
      taskType: draft.taskType,
      automationType: draft.automationType,
      repositoryUrl: draft.repositoryUrl,
      branch: draft.branch,
      framework: draft.framework,
      testPath: draft.testPath || '',
      executeCommand: draft.executeCommand || '',
      environment: draft.environment,
      parametersJson: draft.parametersJson || '',
      pipelineType: draft.pipelineType || 'FREESTYLE',
      jenkinsJob: draft.jenkinsJob || '',
      allureEnabled: draft.allureEnabled,
      allureConfig: draft.allureConfig || '',
      missingItems: draft.missingItems || [],
      riskHints: draft.riskHints || [],
      modelName: draft.modelName || '',
      provider: draft.provider || '',
      promptName: draft.promptName || '',
      promptVersion: draft.promptVersion || '',
      disclaimer: draft.disclaimer || ''
    })
  }

  async function generateAiTask() {
    const projectId = projectStore.currentProject.id
    if (!projectId || !aiTaskForm.value.repositoryUrl.trim() || !aiTaskForm.value.branch.trim() || !aiTaskForm.value.description.trim()) {
      ui.showToast('请填写 Git 仓库、Branch 和自然语言描述')
      return
    }
    aiTaskLoading.value = true
    aiTaskConfirmResult.value = null
    ui.startProcessing('AI 正在生成自动化任务配置')
    let taskId: string
    try {
      ;({ taskId } = await api.automation.aiGenerateTask(projectId, {
        repositoryUrl: aiTaskForm.value.repositoryUrl,
        branch: aiTaskForm.value.branch,
        description: aiTaskForm.value.description
      }))
    } catch (error) {
      aiTaskLoading.value = false
      ui.stopProcessing()
      ui.showToast(error instanceof Error ? error.message : 'AI 生成草稿失败')
      return
    }
    stopAiPoll?.()
    stopAiPoll = pollAiTask<ApiAutomationTaskDraft>(taskId, {
      onSuccess: (draft) => {
        fillAiDraftForm(draft)
        aiTaskLoading.value = false
        ui.stopProcessing()
        ui.showToast('AI 已生成自动化任务配置草稿')
      },
      onFailure: (message) => {
        aiTaskLoading.value = false
        ui.stopProcessing()
        ui.showToast(message)
      }
    })
  }

  async function saveAiDraft() {
    const projectId = projectStore.currentProject.id
    if (!projectId || aiDraftForm.value.id == null) return
    aiTaskLoading.value = true
    try {
      const draft = await api.automation.updateAiDraft(projectId, aiDraftForm.value.id, {
        name: aiDraftForm.value.name,
        taskType: aiDraftForm.value.taskType,
        automationType: aiDraftForm.value.automationType,
        repositoryUrl: aiDraftForm.value.repositoryUrl,
        branch: aiDraftForm.value.branch,
        framework: aiDraftForm.value.framework,
        testPath: aiDraftForm.value.testPath,
        executeCommand: aiDraftForm.value.executeCommand,
        environment: aiDraftForm.value.environment,
        parametersJson: aiDraftForm.value.parametersJson,
        pipelineType: aiDraftForm.value.pipelineType,
        jenkinsJob: aiDraftForm.value.jenkinsJob,
        allureEnabled: aiDraftForm.value.allureEnabled
      })
      fillAiDraftForm(draft)
      ui.showToast('草稿已保存')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '草稿保存失败')
    } finally {
      aiTaskLoading.value = false
    }
  }

  async function confirmAiDraft() {
    const projectId = projectStore.currentProject.id
    if (!projectId || aiDraftForm.value.id == null) return
    aiTaskLoading.value = true
    try {
      aiTaskConfirmResult.value = await api.automation.confirmAiDraft(projectId, aiDraftForm.value.id)
      await loadAutomationData()
      ui.showToast('自动化任务已创建')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '任务创建失败')
    } finally {
      aiTaskLoading.value = false
    }
  }

  return {
    automationProjects,
    automationTasks,
    executableTasks,
    executionRecords,
    executions,
    runForm,
    automationProjectForm,
    editingAutomationProjectId,
    taskForm,
    editingTaskId,
    pipelineBindings,
    taskBindings,
    jenkinsJobs,
    bindingAutomationProjectId,
    editingBindingId,
    bindingForm,
    bindingFormVisible,
    jobImportForm,
    createJobForm,
    onboardingAutomationProjectId,
    onboarding,
    onboardingLoading,
    onboardingTotalSteps,
    onboardingReadySteps,
    onboardingProgress,
    aiTaskForm,
    aiDraftForm,
    aiTaskLoading,
    aiTaskConfirmResult,
    onboardingStatusLabel,
    onboardingStepStatusLabel,
    onboardingStepMeta,
    stepDuration,
    loadAutomationData,
    openNewAutomationProject,
    editAutomationProject,
    createAutomationProject,
    resetTaskForm,
    openNewTask,
    editTask,
    saveTask,
    deleteTask,
    toggleTaskStatus,
    stopTask,
    taskActiveExecution,
    bindingProject,
    openBindingManager,
    loadBindings,
    resetBindingForm,
    openNewBinding,
    closeBindingForm,
    editBinding,
    saveBinding,
    openJobImport,
    loadJenkinsJobs,
    importJenkinsJob,
    openCreateJob,
    createJenkinsJob,
    onTaskAutomationProjectChange,
    syncRunFormFromTask,
    openRunModal,
    runTest,
    syncExecution,
    retryExecution,
    deleteExecution,
    onboardingProject,
    openOnboarding,
    retryOnboarding,
    cancelOnboarding,
    stopOnboardingPolling,
    openAiTask,
    resetAiTask,
    generateAiTask,
    saveAiDraft,
    confirmAiDraft
  }
})
