import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/api'
import type {
  ApiAnalysis,
  ApiExecution,
  ApiExecutionDetail,
  ApiReportCenter,
  ApiReportExecution
} from '@/api'
import { useUiStore } from './ui'
import { useAutomationStore } from './automation'
import { pollAiTask } from '@/utils/pollAiTask'

export const useReportStore = defineStore('report', () => {
  const ui = useUiStore()
  const automation = useAutomationStore()

  const reportCenter = ref<ApiReportCenter | null>(null)
  const reportProjectFilter = ref<number | null>(null)
  const reportStatusFilter = ref('')
  const reportPage = ref(0)
  const analysisResult = ref<ApiAnalysis | null>(null)
  const analyzingExecutionId = ref<number | null>(null)
  let stopAiPoll: (() => void) | undefined
  const executionDetail = ref<ApiExecutionDetail | null>(null)
  const executionDetailConsoleLog = ref('')
  const consoleLogLoading = ref(false)
  const loadingExecutionDetailId = ref<number | null>(null)

  const EMPTY_SUMMARY = { total: 0, passed: 0, failed: 0, broken: 0, skipped: 0, executionCount: 0 }

  // 全局报告中心的汇总（ReportsView 使用）
  const reportSummary = computed(() => reportCenter.value?.summary ?? EMPTY_SUMMARY)

  // 当前项目执行的汇总（ProjectDetailView「测试报告」Tab 使用）
  const projectExecutionSummary = computed(() =>
    automation.executionRecords.reduce((summary, execution) => {
      const report = execution.report
      if (!report) return summary
      summary.total += report.totalCount
      summary.passed += report.passedCount
      summary.failed += report.failedCount
      summary.broken += report.brokenCount
      summary.skipped += report.skippedCount
      return summary
    }, { total: 0, passed: 0, failed: 0, broken: 0, skipped: 0 })
  )

  const reportTrend = computed(() => reportCenter.value?.trend ?? [])
  const reportItems = computed<ApiReportExecution[]>(() => reportCenter.value?.content ?? [])
  const failedReports = computed(() => reportItems.value.filter((execution) =>
    execution.status === 'FAILED'
      || execution.status === 'TIMEOUT'
      || (execution.report?.failedCount || 0) > 0
      || (execution.report?.brokenCount || 0) > 0
  ))

  async function loadReportCenter(projectId?: number | null) {
    try {
      reportCenter.value = await api.reports.center({
        projectId: projectId ?? reportProjectFilter.value,
        status: reportStatusFilter.value || undefined,
        page: reportPage.value,
        size: 20
      })
    } catch (error) {
      ui.showToast('报告中心暂不可用，登录不受影响')
    }
  }

  async function refreshReportCenter() {
    reportPage.value = 0
    await loadReportCenter()
  }

  async function changeReportPage(delta: number) {
    const totalPages = reportCenter.value?.totalPages ?? 0
    const nextPage = reportPage.value + delta
    if (nextPage < 0 || nextPage >= totalPages) return
    reportPage.value = nextPage
    await loadReportCenter()
  }

  async function analyzeExecution(execution: ApiExecution) {
    const projectId = execution.projectId
    if (!projectId) return
    analyzingExecutionId.value = execution.id
    ui.startProcessing('AI 正在分析失败原因')
    let taskId: string
    try {
      ;({ taskId } = await api.automation.analyze(projectId, execution.id))
    } catch (error) {
      analyzingExecutionId.value = null
      ui.stopProcessing()
      ui.showToast(error instanceof Error ? error.message : 'AI 失败分析失败')
      return
    }
    stopAiPoll?.()
    stopAiPoll = pollAiTask<ApiAnalysis>(taskId, {
      onSuccess: async (analysis) => {
        analysisResult.value = analysis
        ui.openModal('analysis')
        await Promise.all([automation.loadAutomationData(), loadReportCenter()])
        analyzingExecutionId.value = null
        ui.stopProcessing()
      },
      onFailure: (message) => {
        analyzingExecutionId.value = null
        ui.stopProcessing()
        ui.showToast(message)
      }
    })
  }

  async function openExecutionDetail(execution: ApiExecution) {
    loadingExecutionDetailId.value = execution.id
    try {
      executionDetail.value = await api.reports.detail(execution.id)
      executionDetailConsoleLog.value = ''
      ui.openModal('executionDetail')
      await loadExecutionConsoleLog()
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '执行详情加载失败')
    } finally {
      loadingExecutionDetailId.value = null
    }
  }

  async function loadExecutionConsoleLog() {
    const detail = executionDetail.value
    if (!detail) return
    if (!detail.consoleLogRef) {
      executionDetailConsoleLog.value = ''
      return
    }
    consoleLogLoading.value = true
    try {
      executionDetailConsoleLog.value = await api.automation.consoleLog(detail.projectId, detail.id) || ''
    } catch (error) {
      executionDetailConsoleLog.value = `控制台日志读取失败：${error instanceof Error ? error.message : '未知错误'}`
    } finally {
      consoleLogLoading.value = false
    }
  }

  async function analyzeDetail() {
    if (executionDetail.value) await analyzeExecution(executionDetail.value)
  }

  function openReference(url?: string) {
    if (!url) {
      ui.showToast('当前执行还没有可打开的报告链接')
      return
    }
    window.open(url, '_blank', 'noopener,noreferrer')
  }

  async function openAllure(execution: ApiExecution) {
    try {
      const access = await api.automation.allureAccess(execution.id)
      if (access.viewUrl) {
        window.open(access.viewUrl, '_blank', 'noopener,noreferrer')
        return
      }
      openReference(execution.report?.allureUrl)
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : 'Allure 报告打开失败')
    }
  }

  async function downloadAllure(execution: ApiExecution) {
    try {
      const access = await api.automation.allureAccess(execution.id)
      if (!access.downloadUrl) {
        ui.showToast('该执行尚未归档离线 Allure 报告，请重新执行后再下载')
        return
      }
      window.open(access.downloadUrl, '_blank', 'noopener,noreferrer')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : 'Allure 报告下载失败')
    }
  }

  return {
    reportCenter,
    reportProjectFilter,
    reportStatusFilter,
    reportPage,
    analysisResult,
    analyzingExecutionId,
    executionDetail,
    executionDetailConsoleLog,
    consoleLogLoading,
    loadingExecutionDetailId,
    reportSummary,
    projectExecutionSummary,
    reportTrend,
    reportItems,
    failedReports,
    loadReportCenter,
    refreshReportCenter,
    changeReportPage,
    analyzeExecution,
    openExecutionDetail,
    loadExecutionConsoleLog,
    analyzeDetail,
    openReference,
    openAllure,
    downloadAllure
  }
})
