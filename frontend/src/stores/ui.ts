import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { ModalName, ProjectTab } from '@/types'
import { useAutomationStore } from './automation'
import { useRequirementStore } from './requirement'

export const useUiStore = defineStore('ui', () => {
  const activeProjectTab = ref<ProjectTab>('overview')
  const activeAiTab = ref<'models' | 'prompts' | 'audits'>('models')
  const activeApiTab = ref<'collections' | 'variables' | 'history'>('collections')
  const activeApiRequestTab = ref<'params' | 'authorization' | 'headers' | 'body' | 'assertions' | 'pre-script' | 'post-script'>('params')
  const modal = ref<ModalName | null>(null)
  const toast = ref('')
  const processing = ref<{ message: string; hint: string } | null>(null)
  const search = ref('')
  const caseStatusFilter = ref('')
  const casePriorityFilter = ref('')
  const caseTypeFilter = ref('')
  const activeCaseSuiteType = ref<'FUNCTIONAL' | 'API'>('FUNCTIONAL')
  let toastTimer: number | undefined

  function openModal(name: ModalName) {
    modal.value = name
  }

  function closeModal() {
    modal.value = null
    // 关闭弹窗时清理业务域的瞬时状态（阶段 5 会下放到 modal 组件各自 onUnmounted）。
    useAutomationStore().stopOnboardingPolling()
    useRequirementStore().requirementDetail = null
  }

  function showToast(message: string) {
    if (toastTimer) window.clearTimeout(toastTimer)
    toast.value = message
    toastTimer = window.setTimeout(() => {
      toast.value = ''
      toastTimer = undefined
    }, 2600)
  }

  function startProcessing(message: string) {
    processing.value = { message, hint: '可离开页面，稍后再来查看' }
  }

  function stopProcessing() {
    processing.value = null
  }

  return {
    activeProjectTab,
    activeAiTab,
    activeApiTab,
    activeApiRequestTab,
    modal,
    toast,
    processing,
    search,
    caseStatusFilter,
    casePriorityFilter,
    caseTypeFilter,
    activeCaseSuiteType,
    openModal,
    closeModal,
    showToast,
    startProcessing,
    stopProcessing
  }
})
