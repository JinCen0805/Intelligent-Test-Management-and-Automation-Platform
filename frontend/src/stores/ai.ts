import { ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/api'
import type { ApiAiModel, ApiPromptTemplate, ApiCaseChatMessage, ApiCaseChatSession, ApiAiCallAudit } from '@/api'
import { useUiStore } from './ui'
import { useAuthStore } from './auth'
import { useProjectStore } from './project'
import { useRequirementStore } from './requirement'
import { useTestcaseStore } from './testcase'

const DEFAULT_CASE_CHAT_MESSAGE: ApiCaseChatMessage = {
  id: Date.now(),
  role: 'assistant',
  content: '你可以先告诉我想补哪类场景，或者直接让我帮你优化选中的测试用例。'
}

function cloneCaseChatMessages(messages: ApiCaseChatMessage[]) {
  return messages.map((item) => ({ ...item }))
}

export const useAiStore = defineStore('ai', () => {
  const ui = useUiStore()
  const auth = useAuthStore()
  const projectStore = useProjectStore()
  const requirement = useRequirementStore()

  const aiModels = ref<ApiAiModel[]>([])
  const promptTemplates = ref<ApiPromptTemplate[]>([])
  const aiCallAudits = ref<ApiAiCallAudit[]>([])
  const modelForm = ref({ name: '', provider: '', endpoint: '', apiKey: '' })
  const editingModelId = ref<number | null>(null)
  const editingPromptId = ref<number | null>(null)
  const generationForm = ref({
    requirementId: null as number | null,
    selectedCaseId: null as number | null,
    modelId: null as number | null,
    suiteType: ui.activeCaseSuiteType,
    rules: '按项目文件中的业务能力和风险生成可执行测试用例；覆盖主流程、必填校验、有效/无效等价类、明确边界值、权限、状态流转、异常、回显和一致性；不明确规则放入待确认事项。'
  })
  const promptForm = ref({ name: '', taskType: '', content: '', version: '' })
  const caseChatMessages = ref<ApiCaseChatMessage[]>([DEFAULT_CASE_CHAT_MESSAGE])
  const caseChatHistory = ref<ApiCaseChatSession[]>([])
  const activeCaseChatSessionId = ref<number | null>(null)
  const caseChatInput = ref('')
  const caseChatFile = ref<File | null>(null)
  const isChatting = ref(false)
  const testingModelId = ref<number | null>(null)
  let caseChatHistoryLoadSeq = 0

  watch(
    () => ui.activeCaseSuiteType,
    (suiteType) => {
      generationForm.value.suiteType = suiteType
      generationForm.value.selectedCaseId = null
      activeCaseChatSessionId.value = null
      caseChatMessages.value.splice(0, caseChatMessages.value.length, ...defaultCaseChatMessages())
      caseChatInput.value = ''
      caseChatFile.value = null
      void loadCaseChatHistory(true)
    },
    { immediate: true }
  )

  function defaultCaseChatMessages() {
    return [{ ...DEFAULT_CASE_CHAT_MESSAGE, id: Date.now() }]
  }

  async function loadCaseChatHistory(restoreLatest = false) {
    const projectId = projectStore.currentProject.id
    const suiteType = ui.activeCaseSuiteType
    const loadSeq = ++caseChatHistoryLoadSeq
    if (!projectId) {
      caseChatHistory.value.splice(0, caseChatHistory.value.length)
      caseChatMessages.value.splice(0, caseChatMessages.value.length, ...defaultCaseChatMessages())
      activeCaseChatSessionId.value = null
      return
    }
    try {
      const sessions = await api.ai.caseChatHistory(projectId, suiteType)
      if (loadSeq !== caseChatHistoryLoadSeq) return
      caseChatHistory.value.splice(0, caseChatHistory.value.length, ...sessions)
      if (restoreLatest) {
        const latest = sessions[0]
        if (latest?.messages?.length) {
          caseChatMessages.value.splice(0, caseChatMessages.value.length, ...cloneCaseChatMessages(latest.messages))
          activeCaseChatSessionId.value = latest.sessionId
        } else {
          caseChatMessages.value.splice(0, caseChatMessages.value.length, ...defaultCaseChatMessages())
          activeCaseChatSessionId.value = null
        }
      }
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '历史对话加载失败')
    }
  }

  function restoreCaseChatSession(sessionId: number) {
    const session = caseChatHistory.value.find((item) => item.sessionId === sessionId)
    if (!session) return
    activeCaseChatSessionId.value = session.sessionId
    caseChatMessages.value.splice(0, caseChatMessages.value.length, ...cloneCaseChatMessages(session.messages))
    ui.closeModal()
  }

  async function deleteCaseChatSession(sessionId: number) {
    const projectId = projectStore.currentProject.id
    if (!projectId) return
    try {
      await api.ai.deleteCaseChatHistory(projectId, sessionId, ui.activeCaseSuiteType)
      await loadCaseChatHistory(false)
      if (activeCaseChatSessionId.value === sessionId) {
        activeCaseChatSessionId.value = null
        caseChatMessages.value.splice(0, caseChatMessages.value.length, ...defaultCaseChatMessages())
      }
      ui.showToast('历史对话已删除')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '历史对话删除失败')
    }
  }

  async function deleteAllCaseChatSessions() {
    const projectId = projectStore.currentProject.id
    if (!projectId || caseChatHistory.value.length === 0) return
    if (!window.confirm('确认删除全部历史对话吗？')) return
    try {
      await api.ai.deleteAllCaseChatHistory(projectId, ui.activeCaseSuiteType)
      caseChatHistory.value.splice(0, caseChatHistory.value.length)
      activeCaseChatSessionId.value = null
      caseChatMessages.value.splice(0, caseChatMessages.value.length, ...defaultCaseChatMessages())
      ui.showToast('全部历史对话已删除')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '全部删除失败')
    }
  }

  async function loadAiConfig() {
    if (auth.authState.role !== 'ADMIN') return
    try {
      const [models, prompts, audits] = await Promise.all([
        api.ai.models(),
        api.ai.prompts(),
        api.ai.audits()
      ])
      aiModels.value.splice(0, aiModels.value.length, ...models)
      promptTemplates.value.splice(0, promptTemplates.value.length, ...prompts)
      aiCallAudits.value.splice(0, aiCallAudits.value.length, ...audits)
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : 'AI 配置加载失败')
    }
  }

  async function saveModel() {
    if (auth.authState.role !== 'ADMIN') {
      ui.showToast('只有管理员可以保存 AI 模型')
      return
    }
    if (!modelForm.value.name.trim() || !modelForm.value.provider.trim()) {
      ui.showToast('请填写模型名称和 Provider')
      return
    }
    try {
      const editingId = editingModelId.value
      const saved = editingId
        ? await api.ai.updateModel(editingId, modelForm.value)
        : await api.ai.createModel(modelForm.value)
      const index = aiModels.value.findIndex((item) => item.id === saved.id)
      if (index >= 0) {
        aiModels.value.splice(index, 1, saved)
      } else {
        aiModels.value.unshift(saved)
      }
      ui.closeModal()
      ui.showToast(editingId ? 'AI 模型已更新' : 'AI 模型配置已保存')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : 'AI 模型保存失败')
    }
  }

  function openModelEditor(model: ApiAiModel | null) {
    editingModelId.value = model?.id ?? null
    Object.assign(modelForm.value, {
      name: model?.name ?? '',
      provider: model?.provider ?? '',
      endpoint: model?.endpoint ?? '',
      apiKey: ''
    })
    ui.openModal('model')
  }

  async function toggleModelEnabled(model: ApiAiModel) {
    if (auth.authState.role !== 'ADMIN') return
    try {
      const saved = await api.ai.setModelEnabled(model.id, !model.enabled)
      const index = aiModels.value.findIndex((item) => item.id === saved.id)
      if (index >= 0) aiModels.value.splice(index, 1, saved)
      ui.showToast(saved.enabled ? '模型已启用' : '模型已停用')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '模型状态更新失败')
    }
  }

  async function testModel(model: ApiAiModel) {
    if (auth.authState.role !== 'ADMIN') return
    testingModelId.value = model.id
    try {
      await api.ai.testModel(model.id)
      ui.showToast('模型连接测试成功')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '模型连接测试失败')
    } finally {
      testingModelId.value = null
    }
  }

  async function deleteModel(model: ApiAiModel) {
    if (auth.authState.role !== 'ADMIN') return
    if (!window.confirm(`确认删除 AI 模型「${model.name}」吗？`)) return
    try {
      await api.ai.deleteModel(model.id)
      const index = aiModels.value.findIndex((item) => item.id === model.id)
      if (index >= 0) aiModels.value.splice(index, 1)
      ui.showToast('AI 模型已删除')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : 'AI 模型删除失败')
    }
  }

  async function savePrompt() {
    if (auth.authState.role !== 'ADMIN') {
      ui.showToast('只有管理员可以保存 Prompt')
      return
    }
    if (!promptForm.value.name.trim() || !promptForm.value.content.trim()) {
      ui.showToast('请填写 Prompt 名称和内容')
      return
    }
    try {
      const editingId = editingPromptId.value
      const saved = editingId
        ? await api.ai.updatePrompt(editingId, promptForm.value)
        : await api.ai.createPrompt(promptForm.value)
      const index = promptTemplates.value.findIndex((item) => item.id === saved.id)
      if (index >= 0) {
        promptTemplates.value.splice(index, 1, saved)
      } else {
        promptTemplates.value.unshift(saved)
      }
      ui.closeModal()
      ui.showToast(editingId ? 'Prompt 模板已更新' : 'Prompt 模板已保存')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : 'Prompt 保存失败')
    }
  }

  function openPromptEditor(prompt: ApiPromptTemplate | null) {
    editingPromptId.value = prompt?.id ?? null
    Object.assign(promptForm.value, {
      name: prompt?.name ?? '',
      taskType: prompt?.taskType ?? '',
      content: prompt?.content ?? '',
      version: prompt?.version ?? ''
    })
    ui.openModal('prompt')
  }

  async function togglePromptEnabled(prompt: ApiPromptTemplate) {
    if (auth.authState.role !== 'ADMIN') return
    try {
      const saved = await api.ai.setPromptEnabled(prompt.id, !prompt.enabled)
      const index = promptTemplates.value.findIndex((item) => item.id === saved.id)
      if (index >= 0) promptTemplates.value.splice(index, 1, saved)
      ui.showToast(saved.enabled ? 'Prompt 已启用' : 'Prompt 已停用')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : 'Prompt 状态更新失败')
    }
  }

  async function publishPrompt(prompt: ApiPromptTemplate) {
    if (auth.authState.role !== 'ADMIN') return
    try {
      const saved = await api.ai.publishPrompt(prompt.id)
      promptTemplates.value.unshift(saved)
      ui.showToast('已发布新版本 ' + saved.version)
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : 'Prompt 发布失败')
    }
  }

  async function activatePrompt(prompt: ApiPromptTemplate) {
    if (auth.authState.role !== 'ADMIN') return
    try {
      const saved = await api.ai.activatePrompt(prompt.id)
      const index = promptTemplates.value.findIndex((item) => item.id === saved.id)
      if (index >= 0) promptTemplates.value.splice(index, 1, saved)
      ui.showToast('已切换到该版本')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : 'Prompt 切换失败')
    }
  }

  function resetCaseChat() {
    activeCaseChatSessionId.value = null
    caseChatMessages.value.splice(0, caseChatMessages.value.length, ...defaultCaseChatMessages())
    caseChatInput.value = ''
    caseChatFile.value = null
  }

  function setCaseChatFile(file: File | null) {
    caseChatFile.value = file
  }

  async function sendCaseChat() {
    const projectId = projectStore.currentProject.id
    const message = caseChatInput.value.trim() || (caseChatFile.value || generationForm.value.requirementId ? '请根据这个文件内容生成测试用例。' : '')
    if (!projectId || !message) return
    caseChatMessages.value.push({
      id: Date.now(),
      role: 'user',
      content: message
    })
    caseChatInput.value = ''
    isChatting.value = true
    try {
      const chatPayload = {
        sessionId: activeCaseChatSessionId.value,
        modelId: generationForm.value.modelId ?? undefined,
        selectedCaseId: generationForm.value.selectedCaseId,
        suiteType: generationForm.value.suiteType,
        message
      }
      const reply = caseChatFile.value
        ? await api.ai.caseChatWithFile(projectId, caseChatFile.value, chatPayload)
        : await api.ai.caseChat(projectId, {
            ...chatPayload,
            requirementId: generationForm.value.requirementId ?? requirement.selectedRequirementId
          })
      activeCaseChatSessionId.value = reply.sessionId
      caseChatMessages.value.push({
        id: Date.now() + 1,
        role: 'assistant',
        content: reply.reply
      })
      caseChatFile.value = null
      await loadCaseChatHistory(false)
      if (reply.generatedCases?.length) {
        const testcaseStore = useTestcaseStore()
        await testcaseStore.loadTestCases({
          source: 'AI',
          status: '',
          keyword: '',
          priority: '',
          caseType: '',
          suiteType: ui.activeCaseSuiteType
        })
        await testcaseStore.loadAllTestCases()
        ui.activeProjectTab = 'case-ai'
        ui.showToast(`AI 对话已生成 ${reply.generatedCases.length} 条待处理用例`)
      }
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : 'AI 对话失败')
      caseChatMessages.value.push({
        id: Date.now() + 1,
        role: 'assistant',
        content: '刚才这轮对话没有成功，你可以再发一次。'
      })
    } finally {
      isChatting.value = false
    }
  }

  return {
    aiModels,
    promptTemplates,
    aiCallAudits,
    modelForm,
    editingModelId,
    editingPromptId,
    generationForm,
    promptForm,
    caseChatMessages,
    caseChatHistory,
    activeCaseChatSessionId,
    caseChatInput,
    caseChatFile,
    isChatting,
    loadAiConfig,
    saveModel,
    openModelEditor,
    toggleModelEnabled,
    testModel,
    testingModelId,
    deleteModel,
    savePrompt,
    openPromptEditor,
    togglePromptEnabled,
    publishPrompt,
    activatePrompt,
    loadCaseChatHistory,
    resetCaseChat,
    setCaseChatFile,
    restoreCaseChatSession,
    deleteCaseChatSession,
    deleteAllCaseChatSessions,
    sendCaseChat
  }
})
