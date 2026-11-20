import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/api'
import type { ApiAutomationProject, ApiExecution, ApiTestTask } from '@/api'
import { useUiStore } from './ui'
import { useProjectStore } from './project'
import { useReportStore } from './report'
import { createBindingsDomain } from './automation/bindings'
import { createOnboardingDomain } from './automation/onboarding'
import { createAiTaskDomain } from './automation/aiTask'

/**
 * 自动化领域门面 store：项目/任务/执行在本文件维护；
 * 执行目标（binding/job）与 onboarding、AI 创建任务拆分到 ./automation/* 领域模块。
 */
export const useAutomationStore = defineStore('automation', () => {
  const ui = useUiStore()
  const projectStore = useProjectStore()

  const automationProjects = ref<ApiAutomationProject[]>([])
  const automationTasks = ref<ApiTestTask[]>([])
  const executableTasks = computed(() => automationTasks.value.filter((task) => task.status === 'ENABLED'))
  const executionRecords = ref<ApiExecution[]>([])
  const executionPagination = ref({ page: 0, size: 20, totalPages: 1, totalElements: 0 })

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

  const bindings = createBindingsDomain({ automationProjects, taskForm })
  const onboarding = createOnboardingDomain({ automationProjects })
  const aiTask = createAiTaskDomain({ loadAutomationData })

  async function loadAutomationData() {
    const projectId = projectStore.currentProject.id
    if (!projectId) return
    const [projectsList, tasks] = await Promise.all([
      api.automation.projects(projectId),
      api.automation.tasks(projectId)
    ])
    automationProjects.value.splice(0, automationProjects.value.length, ...projectsList)
    automationTasks.value.splice(0, automationTasks.value.length, ...tasks)
    await loadExecutions(0)
    if (runForm.value.taskId === null && executableTasks.value.length > 0) {
      runForm.value.taskId = executableTasks.value[0].id
    }
  }

  async function loadExecutions(page = 0) {
    const projectId = projectStore.currentProject.id
    if (!projectId) return
    const result = await api.automation.executions(projectId, page, executionPagination.value.size)
    executionRecords.value.splice(0, executionRecords.value.length, ...result.content)
    executionPagination.value = {
      page: result.page,
      size: result.size,
      totalPages: result.totalPages,
      totalElements: result.totalElements
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
    bindings.taskBindings.value.splice(0, bindings.taskBindings.value.length)
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
    void bindings.onTaskAutomationProjectChange()
    ui.openModal('task')
  }

  let savingTask = false

  async function saveTask() {
    const projectId = projectStore.currentProject.id
    if (!projectId || !taskForm.value.name.trim() || !taskForm.value.automationProjectId) {
      ui.showToast('请填写任务名称并选择自动化项目')
      return
    }
    if (savingTask) return
    savingTask = true
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
    } finally {
      savingTask = false
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

  let runningTest = false

  async function runTest() {
    const projectId = projectStore.currentProject.id
    const task = executableTasks.value.find((item) => item.id === runForm.value.taskId) || executableTasks.value[0]
    if (!projectId || !task) {
      ui.showToast('当前项目还没有可执行的自动化任务')
      return
    }
    if (runningTest) return
    runningTest = true
    try {
      const execution = await api.automation.execute(projectId, task.id, {
        parametersJson: runForm.value.parametersJson.trim() || undefined
      })
      await loadAutomationData()
      ui.closeModal()
      ui.showToast(`已触发 Jenkins Build ${execution.buildNumber || execution.id}`)
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '执行任务失败')
    } finally {
      runningTest = false
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

  return {
    automationProjects,
    automationTasks,
    executableTasks,
    executionRecords,
    executionPagination,
    runForm,
    automationProjectForm,
    editingAutomationProjectId,
    taskForm,
    editingTaskId,
    loadAutomationData,
    loadExecutions,
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
    syncRunFormFromTask,
    openRunModal,
    runTest,
    syncExecution,
    retryExecution,
    deleteExecution,
    ...bindings,
    ...onboarding,
    ...aiTask
  }
})
