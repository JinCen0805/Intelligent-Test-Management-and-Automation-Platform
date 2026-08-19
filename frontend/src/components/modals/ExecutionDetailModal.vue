<script setup lang="ts">
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useReportStore } from '@/stores/report'
import BaseModal from '@/components/common/BaseModal.vue'

const router = useRouter()
const ui = useUiStore()
const { closeModal } = ui
const reportStore = useReportStore()
const { executionDetail, executionDetailConsoleLog, consoleLogLoading, analyzingExecutionId } = storeToRefs(reportStore)
const { openAllure, downloadAllure, openReference, analyzeDetail } = reportStore

async function openDetailTask() {
  const detail = executionDetail.value
  if (!detail?.projectId) return
  ui.activeProjectTab = 'tasks'
  closeModal()
  await router.push({ name: 'project', params: { id: String(detail.projectId) } })
}
</script>

<template>
  <BaseModal title="执行详情">
    <div class="modal-body">
      <template v-if="executionDetail">
        <div class="detail-grid">
          <div><span class="muted">项目</span><strong>{{ executionDetail.projectName }}</strong></div>
          <div><span class="muted">测试任务</span><strong>{{ executionDetail.taskName }}<small class="muted">（{{ executionDetail.taskType }}）</small></strong></div>
          <div><span class="muted">自动化项目</span><strong>{{ executionDetail.automationProjectName || '--' }}</strong></div>
          <div><span class="muted">状态</span><strong><span class="tag" :class="executionDetail.status === 'PASSED' ? 'green' : executionDetail.status === 'FAILED' ? 'red' : 'orange'">{{ executionDetail.status }}</span></strong></div>
          <div><span class="muted">Build #</span><strong>{{ executionDetail.buildNumber || '队列中' }}</strong></div>
          <div><span class="muted">Branch</span><strong>{{ executionDetail.branch || '--' }}</strong></div>
          <div><span class="muted">Commit</span><strong>{{ executionDetail.commitSha || '--' }}</strong></div>
          <div><span class="muted">环境</span><strong>{{ executionDetail.environment || '--' }}</strong></div>
          <div><span class="muted">Jenkins Job</span><strong>{{ executionDetail.jenkinsJob || '--' }}</strong></div>
          <div><span class="muted">耗时</span><strong>{{ executionDetail.durationMs ? `${Math.round(executionDetail.durationMs / 1000)}s` : '--' }}</strong></div>
          <div><span class="muted">触发</span><strong>{{ executionDetail.triggeredBy || '系统' }} · {{ executionDetail.triggerMethod || 'MANUAL' }}</strong></div>
          <div><span class="muted">开始时间</span><strong>{{ executionDetail.startedAt ? new Date(executionDetail.startedAt).toLocaleString('zh-CN') : '--' }}</strong></div>
        </div>

        <div v-if="executionDetail.report" class="report-kpis">
          <div class="card report-kpi"><span>Total</span><strong>{{ executionDetail.report.totalCount }}</strong></div>
          <div class="card report-kpi"><span>Passed</span><strong class="trend-up">{{ executionDetail.report.passedCount }}</strong></div>
          <div class="card report-kpi"><span>Failed</span><strong class="danger-text">{{ executionDetail.report.failedCount }}</strong></div>
          <div class="card report-kpi"><span>Broken</span><strong class="warning-text">{{ executionDetail.report.brokenCount }}</strong></div>
          <div class="card report-kpi"><span>Skipped</span><strong>{{ executionDetail.report.skippedCount }}</strong></div>
        </div>

        <div v-if="executionDetail.syncFailureMessage" class="auth-error">Jenkins 同步失败：{{ executionDetail.syncFailureMessage }}<span v-if="executionDetail.syncFailureCount" class="muted">（累计 {{ executionDetail.syncFailureCount }} 次）</span></div>

        <div v-if="executionDetail.report?.failureDetail" class="analysis-section">
          <h4>失败堆栈</h4>
          <pre>{{ executionDetail.report.failureDetail }}</pre>
        </div>

        <div v-if="executionDetail.report?.aiAnalysis" class="analysis-section">
          <h4>AI 分析</h4>
          <p>{{ executionDetail.report.aiAnalysis }}</p>
        </div>

        <div class="analysis-section">
          <h4>报告与资产</h4>
          <div class="head-actions">
            <button v-if="executionDetail.report?.allureUrl" class="btn" @click="openAllure(executionDetail)">打开 Allure</button>
            <button v-if="executionDetail.report?.artifactRef === 'OFFLINE_ALLURE'" class="btn" @click="downloadAllure(executionDetail)">下载 Allure ZIP</button>
            <button v-if="executionDetail.report?.artifactRef && executionDetail.report?.artifactRef !== 'OFFLINE_ALLURE'" class="btn" @click="openReference(executionDetail.report?.artifactRef)">下载 Artifact</button>
            <button v-if="executionDetail.report?.screenshotRef" class="btn" @click="openReference(executionDetail.report?.screenshotRef)">截图</button>
            <button v-if="executionDetail.report?.videoRef" class="btn" @click="openReference(executionDetail.report?.videoRef)">视频</button>
            <span v-if="!executionDetail.report?.allureUrl && !executionDetail.report?.artifactRef && !executionDetail.report?.screenshotRef && !executionDetail.report?.videoRef" class="muted">无报告资产（同步 Jenkins 后生成）</span>
          </div>
        </div>

        <div v-if="executionDetail.consoleLogRef" class="analysis-section">
          <h4>Console Log</h4>
          <p v-if="consoleLogLoading" class="muted">正在读取控制台日志...</p>
          <pre v-else>{{ executionDetailConsoleLog || '暂无控制台日志内容' }}</pre>
        </div>

        <div class="modal-foot">
          <button class="btn" @click="openDetailTask">查看测试任务</button>
          <button v-if="['FAILED', 'ABORTED', 'TIMEOUT'].includes(executionDetail.status)" class="btn primary" :disabled="analyzingExecutionId === executionDetail.id" @click="analyzeDetail"><span v-if="analyzingExecutionId === executionDetail.id" class="spinner"></span>{{ analyzingExecutionId === executionDetail.id ? '分析中...' : 'AI 分析' }}</button>
          <button class="btn" @click="closeModal">关闭</button>
        </div>
      </template>
      <p v-else class="muted">正在加载执行详情...</p>
    </div>
  </BaseModal>
</template>
