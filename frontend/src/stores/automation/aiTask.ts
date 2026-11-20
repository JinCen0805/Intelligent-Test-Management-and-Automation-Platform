import { ref } from 'vue'
import { api } from '@/api'
import type { ApiAutomationTaskConfirm, ApiAutomationTaskDraft } from '@/api'
import { pollAiTask } from '@/utils/pollAiTask'
import { useUiStore } from '../ui'
import { useProjectStore } from '../project'

/**
 * 「AI 创建自动化任务」域：描述生成草稿、编辑确认与创建。
 */
export function createAiTaskDomain(deps: {
  loadAutomationData: () => Promise<void>
}) {
  const ui = useUiStore()
  const projectStore = useProjectStore()

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
      await deps.loadAutomationData()
      ui.showToast('自动化任务已创建')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '任务创建失败')
    } finally {
      aiTaskLoading.value = false
    }
  }

  return {
    aiTaskForm,
    aiDraftForm,
    aiTaskLoading,
    aiTaskConfirmResult,
    openAiTask,
    resetAiTask,
    generateAiTask,
    saveAiDraft,
    confirmAiDraft
  }
}
