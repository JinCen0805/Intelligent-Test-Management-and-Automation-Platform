import { ref } from 'vue'
import type { Ref } from 'vue'
import { api } from '@/api'
import type { ApiAutomationProject, ApiJenkinsJob, ApiPipelineBinding } from '@/api'
import { useUiStore } from '../ui'
import { useProjectStore } from '../project'

interface BindingsDomainDeps {
  automationProjects: Ref<ApiAutomationProject[]>
  taskForm: Ref<{
    automationProjectId: number | null
    pipelineBindingId: number | null
  }>
}

/**
 * 自动化「执行目标（Pipeline Binding / Jenkins Job）」域。
 * 包含绑定列表、Job 导入/创建，以及任务表单选择自动化项目时联动加载绑定。
 */
export function createBindingsDomain(deps: BindingsDomainDeps) {
  const ui = useUiStore()
  const projectStore = useProjectStore()

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

  function bindingProject() {
    return deps.automationProjects.value.find((item) => item.id === bindingAutomationProjectId.value) || null
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
    deps.taskForm.value.pipelineBindingId = null
    const projectId = projectStore.currentProject.id
    if (!projectId || !deps.taskForm.value.automationProjectId) return
    try {
      const list = await api.automation.bindings(projectId, deps.taskForm.value.automationProjectId)
      taskBindings.value.splice(0, taskBindings.value.length, ...list)
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '执行目标加载失败')
    }
  }

  return {
    pipelineBindings,
    taskBindings,
    jenkinsJobs,
    bindingAutomationProjectId,
    editingBindingId,
    bindingForm,
    bindingFormVisible,
    jobImportForm,
    createJobForm,
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
    onTaskAutomationProjectChange
  }
}
