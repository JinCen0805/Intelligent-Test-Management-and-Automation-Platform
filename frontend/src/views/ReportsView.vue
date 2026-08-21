<script setup lang="ts">
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useProjectStore } from '@/stores/project'
import { useReportStore } from '@/stores/report'
import { useAutomationStore } from '@/stores/automation'

const projectStore = useProjectStore()
const reportStore = useReportStore()
const automationStore = useAutomationStore()

const { projects } = storeToRefs(projectStore)
const {
  reportProjectFilter,
  reportStatusFilter,
  reportPage,
  reportCenter,
  reportSummary,
  reportTrend,
  reportItems,
  failedReports,
  analyzingExecutionId,
  loadingExecutionDetailId
} = storeToRefs(reportStore)
const {
  loadReportCenter,
  refreshReportCenter,
  changeReportPage,
  analyzeExecution,
  openReference,
  openAllure,
  openExecutionDetail,
  downloadAllure
} = reportStore
const { syncExecution, retryExecution, deleteExecution } = automationStore

onMounted(() => { void loadReportCenter() })
</script>

<template>
  <section class="view">
    <div class="page-head"><div><h1>测试报告中心</h1><p>跨项目查看 Jenkins 构建、Allure 报告、日志、截图、视频与失败分析。</p></div><div class="head-actions report-filters"><select class="report-filter" v-model.number="reportProjectFilter" aria-label="按项目筛选" @change="refreshReportCenter"><option :value="null">全部项目</option><option v-for="project in projects" :key="project.id" :value="project.id">{{ project.name }}</option></select><select class="report-filter" v-model="reportStatusFilter" aria-label="按状态筛选" @change="refreshReportCenter"><option value="">全部状态</option><option value="PASSED">PASSED</option><option value="FAILED">FAILED</option><option value="TIMEOUT">TIMEOUT</option><option value="ABORTED">ABORTED</option></select><button class="btn" @click="refreshReportCenter">刷新报告</button></div></div>
    <div class="report-kpis"><div class="card report-kpi"><span>Total</span><strong>{{ reportSummary.total }}</strong></div><div class="card report-kpi"><span>Passed</span><strong class="trend-up">{{ reportSummary.passed }}</strong></div><div class="card report-kpi"><span>Failed</span><strong class="danger-text">{{ reportSummary.failed }}</strong></div><div class="card report-kpi"><span>Broken</span><strong class="warning-text">{{ reportSummary.broken }}</strong></div><div class="card report-kpi"><span>Skipped</span><strong>{{ reportSummary.skipped }}</strong></div></div>
    <div class="section-grid grid report-bottom"><div class="card"><div class="card-head"><h3>通过率趋势</h3></div><div class="card-body table-wrap"><table><thead><tr><th>日期</th><th>Total</th><th>Passed</th><th>Failed / Broken</th><th>通过率</th></tr></thead><tbody><tr v-for="item in reportTrend" :key="item.date"><td>{{ item.date }}</td><td>{{ item.total }}</td><td class="trend-up">{{ item.passed }}</td><td class="danger-text">{{ item.failed + item.broken }}</td><td>{{ item.passRate }}%</td></tr><tr v-if="reportTrend.length === 0"><td colspan="5" class="muted">暂无趋势数据</td></tr></tbody></table></div></div><div class="card"><div class="card-head"><h3>失败执行</h3><span class="muted">{{ failedReports.length }} 条</span></div><div class="card-body"><div v-for="execution in failedReports.slice(0, 5)" :key="execution.id" class="failure-box report-failure"><strong>Execution #{{ execution.id }} · {{ execution.projectName || '当前项目' }} · {{ execution.status }}</strong><p>{{ execution.report?.failureDetail || execution.report?.aiAnalysis || '暂无失败详情' }}</p><div class="head-actions"><button class="btn primary" :disabled="analyzingExecutionId === execution.id" @click="analyzeExecution(execution)">AI 分析</button><button v-if="execution.consoleLogRef" class="btn" @click="openReference(execution.consoleLogRef)">打开日志</button><button v-if="execution.report?.allureUrl" class="btn" @click="openAllure(execution)">Allure</button></div></div><p v-if="failedReports.length === 0" class="muted">暂无失败执行。</p></div></div></div>
    <div class="card"><div class="card-head"><h3>执行报告</h3><span class="muted">第 {{ (reportCenter?.page ?? 0) + 1 }} / {{ reportCenter?.totalPages || 1 }} 页，共 {{ reportCenter?.totalElements ?? 0 }} 条</span></div><div class="card-body table-wrap"><table><thead><tr><th>Execution</th><th>项目 / 任务</th><th>Jenkins Job</th><th>Build</th><th>状态</th><th>Allure</th><th>资产</th><th></th></tr></thead><tbody><tr v-for="execution in reportItems" :key="execution.id"><td><strong>#{{ execution.id }}</strong><br><span class="muted">{{ execution.startedAt ? new Date(execution.startedAt).toLocaleString('zh-CN') : '--' }}</span></td><td>{{ execution.projectName }}<br><span class="muted">{{ execution.taskName }}</span></td><td>{{ execution.jenkinsJob || '--' }}</td><td>{{ execution.buildNumber || '队列中' }}</td><td><span class="status" :class="execution.status.toLowerCase()">{{ execution.status }}</span></td><td><div class="head-actions"><button v-if="execution.report?.allureUrl" class="link-btn" @click="openAllure(execution)">打开</button><button v-if="execution.report?.artifactRef === 'OFFLINE_ALLURE'" class="btn" @click="downloadAllure(execution)">下载 ZIP</button><span v-if="!execution.report?.allureUrl && !execution.report?.artifactRef">--</span></div></td><td><div class="head-actions"><button v-if="execution.consoleLogRef" class="btn" @click="openReference(execution.consoleLogRef)">日志</button><button v-if="execution.report?.screenshotRef" class="btn" @click="openReference(execution.report.screenshotRef)">截图</button><button v-if="execution.report?.videoRef" class="btn" @click="openReference(execution.report.videoRef)">视频</button></div></td><td><div class="head-actions"><button class="btn" :disabled="loadingExecutionDetailId === execution.id" @click="openExecutionDetail(execution)">详情</button><button class="btn" @click="syncExecution(execution)">同步</button><button v-if="['FAILED', 'ABORTED', 'TIMEOUT'].includes(execution.status)" class="btn primary" :disabled="analyzingExecutionId === execution.id" @click="analyzeExecution(execution)"><span v-if="analyzingExecutionId === execution.id" class="spinner"></span>{{ analyzingExecutionId === execution.id ? '分析中...' : 'AI 分析' }}</button><button v-if="execution.status === 'FAILED' || execution.status === 'ABORTED' || execution.status === 'TIMEOUT'" class="btn" @click="retryExecution(execution)">重试</button><button class="btn danger-text" @click="deleteExecution(execution)">删除</button></div></td></tr><tr v-if="reportItems.length === 0"><td colspan="8" class="muted">暂无执行记录</td></tr></tbody></table><div class="pagination"><button class="btn" :disabled="reportPage <= 0" @click="changeReportPage(-1)">上一页</button><span>第 {{ (reportPage ?? 0) + 1 }} 页</span><button class="btn" :disabled="reportPage + 1 >= (reportCenter?.totalPages ?? 0)" @click="changeReportPage(1)">下一页</button></div></div></div>
  </section>
</template>
