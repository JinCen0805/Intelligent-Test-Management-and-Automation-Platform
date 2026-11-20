import { computed, ref } from 'vue'
import { api } from '@/api'
import type { ApiAutomationProject, ApiOnboardingStatus } from '@/api'
import { useUiStore } from '../ui'
import { useProjectStore } from '../project'

const ONBOARDING_STEPS: { key: string; label: string; role: string }[] = [
  { key: 'REPOSITORY_VALIDATE', label: '仓库校验', role: '平台' },
  { key: 'FRAMEWORK_DETECT', label: '框架识别', role: '平台' },
  { key: 'JENKINSFILE_CHECK', label: 'Jenkinsfile 检查', role: '平台' },
  { key: 'PIPELINE_BIND_OR_PROVISION', label: 'Pipeline 绑定/创建', role: '项目管理员' },
  { key: 'BRANCH_SCAN', label: '分支扫描', role: 'Jenkins' },
  { key: 'AGENT_PRECHECK', label: 'Agent 预检', role: 'Jenkins Agent' },
  { key: 'DEFAULT_TASK_CREATE', label: '默认任务创建', role: '平台' }
]

/**
 * 自动化项目「初始化（onboarding）」域：启动、轮询、重试、取消与进度展示。
 */
export function createOnboardingDomain(deps: {
  automationProjects: { value: ApiAutomationProject[] }
}) {
  const ui = useUiStore()
  const projectStore = useProjectStore()

  const onboardingAutomationProjectId = ref<number | null>(null)
  const onboarding = ref<ApiOnboardingStatus | null>(null)
  const onboardingLoading = ref(false)
  let onboardingTimer: number | undefined

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

  function onboardingProject() {
    return deps.automationProjects.value.find((item) => item.id === onboardingAutomationProjectId.value) || null
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

  return {
    onboardingAutomationProjectId,
    onboarding,
    onboardingLoading,
    onboardingTotalSteps,
    onboardingReadySteps,
    onboardingProgress,
    onboardingStatusLabel,
    onboardingStepStatusLabel,
    onboardingStepMeta,
    stepDuration,
    onboardingProject,
    openOnboarding,
    retryOnboarding,
    cancelOnboarding,
    stopOnboardingPolling
  }
}
