<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import type { View } from '@/types'
import { useAuthStore } from '@/stores/auth'
import { useProjectStore } from '@/stores/project'
import { useReportStore } from '@/stores/report'
import { useTestcaseStore } from '@/stores/testcase'
import { useUiStore } from '@/stores/ui'

const router = useRouter()
const auth = useAuthStore()
const projectStore = useProjectStore()
const reportStore = useReportStore()
const testcaseStore = useTestcaseStore()
const ui = useUiStore()

const { authState } = storeToRefs(auth)
const { projects } = storeToRefs(projectStore)
const { openProject } = projectStore
const { reportItems, failedReports, reportSummary } = storeToRefs(reportStore)
const { allTestCases } = storeToRefs(testcaseStore)
const isRefreshing = ref(false)

async function refreshHomeData() {
  if (isRefreshing.value) return
  isRefreshing.value = true
  try {
    // Test cases depend on the project selected while loading the project list.
    await projectStore.loadProjects()
    await Promise.all([
      reportStore.loadReportCenter(),
      testcaseStore.loadAllTestCases()
    ])
  } catch (error) {
    const message = error instanceof Error ? error.message : '首页数据刷新失败'
    ui.showToast(message)
  } finally {
    isRefreshing.value = false
  }
}

function navigate(view: View) {
  void router.push({ name: view })
}

const todayLabel = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric'
}).format(new Date())

const homeAveragePassRate = computed(() => {
  const summary = reportSummary.value
  return summary.total > 0
    ? `${Math.round(summary.passed / summary.total * 1000) / 10}%`
    : '--'
})

const testCasesPendingReview = computed(() => allTestCases.value.filter((item) => item.status === 'REVIEW').length)

onMounted(() => { void refreshHomeData() })
</script>

<template>
  <section class="view home-view">
    <div class="home-hero">
      <div>
        <span class="eyebrow">TESTPILOT / QUALITY CONTROL</span>
        <h1>质量总览</h1>
        <p>今天是 {{ todayLabel }}，查看项目质量、自动化执行和 AI 工作流的最新状态。</p>
      </div>
      <div class="head-actions">
        <button class="btn" :disabled="isRefreshing" @click="refreshHomeData"><span v-if="isRefreshing" class="spinner"></span>{{ isRefreshing ? '刷新中...' : '↻ 刷新数据' }}</button>
        <button class="btn primary" @click="navigate('projects')">＋ 创建项目</button>
      </div>
    </div>

    <div class="quality-signal card">
      <div><span>今日质量状态</span><strong :class="failedReports.length ? 'danger-text' : 'good-text'">{{ failedReports.length ? '需要关注' : '稳定运行' }}</strong></div>
      <div><span>平均通过率</span><strong>{{ homeAveragePassRate }}</strong></div>
      <div><span>失败执行</span><strong class="danger-text">{{ failedReports.length }}</strong></div>
      <div><span>推荐下一步</span><strong class="signal-action">Review {{ testCasesPendingReview }} 条 AI Draft 用例 →</strong></div>
    </div>

    <div class="home-metrics grid">
      <div class="card stat"><span class="stat-label">项目数量 <b class="stat-icon blue">▣</b></span><strong class="stat-value">{{ projects.length }}</strong><span class="stat-foot">当前账号可访问</span></div>
      <div class="card stat"><span class="stat-label">执行次数 <b class="stat-icon green">▶</b></span><strong class="stat-value">{{ reportSummary.executionCount }}</strong><span class="stat-foot">报告中心累计</span></div>
      <div class="card stat"><span class="stat-label">待 Review <b class="stat-icon purple">✦</b></span><strong class="stat-value">{{ testCasesPendingReview }}</strong><span class="stat-foot">AI 生成测试用例</span></div>
      <div class="card stat"><span class="stat-label">失败执行 <b class="stat-icon orange">!</b></span><strong class="stat-value">{{ failedReports.length }}</strong><span class="stat-foot">其中 {{ failedReports.slice(0, 3).length }} 条待分析</span></div>
      <div class="card stat"><span class="stat-label">自动化覆盖 <b class="stat-icon cyan">◌</b></span><strong class="stat-value">--</strong><span class="stat-foot">等待覆盖率统计接入</span></div>
    </div>

    <div class="section-grid grid home-main-grid">
      <div class="card">
        <div class="card-head"><div class="card-title"><span class="panel-index">01</span><h3>项目质量矩阵</h3></div><span class="muted">最近执行</span></div>
        <div class="card-body table-wrap">
          <table>
            <thead><tr><th>项目名称</th><th>状态</th><th>通过率</th><th>最近执行</th><th></th></tr></thead>
            <tbody>
              <tr v-for="(project, index) in projects.slice(0, 4)" :key="project.code">
                <td><strong>{{ project.name }}</strong><br><span class="muted">{{ project.code }} · {{ project.members }} 位成员</span></td>
                <td><span class="tag" :class="project.status === '运行中' ? 'green' : 'orange'">{{ project.status }}</span></td>
                <td>--</td>
                <td>查看报告中心</td>
                <td><button class="btn" @click="openProject(project)">查看</button></td>
              </tr>
              <tr v-if="projects.length === 0"><td colspan="5" class="muted">暂无项目</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="card">
        <div class="card-head"><div class="card-title"><span class="panel-index">02</span><h3>失败诊断</h3></div><span class="muted">{{ failedReports.length }} 条待处理</span></div>
        <div class="card-body">
          <div v-for="execution in failedReports.slice(0, 3)" :key="execution.id" class="failure-box report-failure">
            <strong>#{{ execution.id }} · {{ execution.projectName || '--' }}</strong>
            <span class="tag red failure-status">{{ execution.status }}</span>
            <p>{{ execution.report?.failureDetail || execution.report?.aiAnalysis || '暂无失败详情' }}</p>
            <div class="head-actions"><button class="btn ai-btn" @click="navigate('reports')">✦ AI 分析</button><button class="btn" @click="navigate('reports')">查看报告</button></div>
          </div>
          <p v-if="failedReports.length === 0" class="muted">当前没有失败执行。</p>
        </div>
      </div>
    </div>

    <div class="card pipeline-card">
      <div class="card-head"><div class="card-title"><span class="panel-index">03</span><h3>测试质量流水线</h3></div><span class="muted">从项目文件到报告的全链路状态</span></div>
      <div class="quality-pipeline">
        <div class="pipeline-step done"><b>▤</b><strong>项目文件</strong><small>管理项目资料</small></div>
        <div class="pipeline-step done"><b>✦</b><strong>用例生成</strong><small>{{ testCasesPendingReview }} 条待 Review</small></div>
        <div class="pipeline-step active"><b>▶</b><strong>自动化执行</strong><small>{{ reportItems.filter((item) => item.status === 'RUNNING' || item.status === 'QUEUED').length }} 个任务运行中</small></div>
        <div class="pipeline-step"><b>▥</b><strong>报告聚合</strong><small>{{ reportItems.length }} 条最近执行</small></div>
        <div class="pipeline-step"><b>◈</b><strong>失败分析</strong><small>AI 建议待确认</small></div>
      </div>
    </div>

    <div class="card home-executions">
      <div class="card-head"><div class="card-title"><span class="panel-index">04</span><h3>最近执行</h3></div><button class="link-btn" @click="navigate('reports')">查看报告中心 →</button></div>
      <div class="card-body table-wrap">
        <table>
          <thead><tr><th>项目 / 任务</th><th>Jenkins Job</th><th>Build</th><th>执行时间</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-for="execution in reportItems.slice(0, 5)" :key="execution.id">
              <td><strong>{{ execution.projectName }}</strong><br><span class="muted">{{ execution.taskName }}</span></td>
              <td>{{ execution.jenkinsJob || '--' }}</td><td>{{ execution.buildNumber || '队列中' }}</td>
              <td>{{ execution.startedAt ? new Date(execution.startedAt).toLocaleString('zh-CN') : '--' }}</td>
              <td><span class="status" :class="execution.status.toLowerCase()">{{ execution.status }}</span></td>
            </tr>
            <tr v-if="reportItems.length === 0"><td colspan="5" class="muted">暂无执行记录</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>
