<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import type { View } from '@/types'
import { useAuthStore } from '@/stores/auth'
import { useProjectStore } from '@/stores/project'
import { useReportStore } from '@/stores/report'

const router = useRouter()
const auth = useAuthStore()
const projectStore = useProjectStore()
const reportStore = useReportStore()

const { authState } = storeToRefs(auth)
const { projects } = storeToRefs(projectStore)
const { openProject } = projectStore
const { reportItems, failedReports, reportSummary } = storeToRefs(reportStore)
const { loadReportCenter } = reportStore

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

onMounted(() => { void loadReportCenter() })
</script>

<template>
  <section class="view">
    <div class="page-head">
      <div><h1>你好，{{ authState.displayName }}</h1><p>今天是 {{ todayLabel }}。</p></div>
    </div>
    <div class="stats-grid grid">
      <div class="card stat"><span class="stat-label">项目数量 <b class="stat-icon blue">▣</b></span><strong class="stat-value">{{ projects.length }}</strong><span class="stat-foot">当前账号可访问</span></div>
      <div class="card stat"><span class="stat-label">执行次数 <b class="stat-icon green">▶</b></span><strong class="stat-value">{{ reportSummary.executionCount }}</strong><span class="stat-foot">报告中心累计</span></div>
      <div class="card stat"><span class="stat-label">平均通过率 <b class="stat-icon red">%</b></span><strong class="stat-value">{{ homeAveragePassRate }}</strong><span class="stat-foot">报告中心统计</span></div>
      <div class="card stat"><span class="stat-label">失败执行 <b class="stat-icon orange">!</b></span><strong class="stat-value">{{ failedReports.length }}</strong><span class="stat-foot">待关注</span></div>
    </div>
    <div class="section-grid grid">
      <div class="card">
        <div class="card-head"><h3>我的项目</h3><button class="link-btn" @click="navigate('projects')">查看全部 →</button></div>
        <div class="card-body table-wrap"><table><thead><tr><th>项目名称</th><th>状态</th><th></th></tr></thead><tbody>
          <tr v-for="project in projects" :key="project.code"><td><strong>{{ project.name }}</strong><br><span class="muted">{{ project.code }}</span></td><td><span class="tag" :class="project.status === '运行中' ? 'green' : 'orange'">{{ project.status }}</span></td><td><button class="btn" @click="openProject(project)">进入 →</button></td></tr>
          <tr v-if="projects.length === 0"><td colspan="3" class="muted">暂无项目</td></tr>
        </tbody></table></div>
      </div>
      <div class="card">
        <div class="card-head"><h3>最近失败</h3><button class="link-btn" @click="navigate('reports')">查看报告 →</button></div>
        <div class="card-body">
          <div v-for="execution in failedReports.slice(0, 3)" :key="execution.id" class="failure-box report-failure"><strong>#{{ execution.id }} · {{ execution.projectName || '--' }} · {{ execution.status }}</strong><p>{{ execution.report?.failureDetail || execution.report?.aiAnalysis || '暂无失败详情' }}</p></div>
          <p v-if="failedReports.length === 0" class="muted">当前没有失败执行。</p>
        </div>
      </div>
    </div>
    <div class="card">
      <div class="card-head"><h3>最近执行</h3><button class="link-btn" @click="navigate('reports')">查看报告中心 →</button></div>
      <div class="card-body table-wrap"><table><thead><tr><th>项目 / 任务</th><th>Jenkins Job</th><th>Build</th><th>执行时间</th><th>状态</th></tr></thead><tbody>
        <tr v-for="execution in reportItems.slice(0, 5)" :key="execution.id"><td><strong>{{ execution.projectName }}</strong><br><span class="muted">{{ execution.taskName }}</span></td><td>{{ execution.jenkinsJob || '--' }}</td><td>{{ execution.buildNumber || '队列中' }}</td><td>{{ execution.startedAt ? new Date(execution.startedAt).toLocaleString('zh-CN') : '--' }}</td><td><span class="status" :class="execution.status.toLowerCase()">{{ execution.status }}</span></td></tr>
        <tr v-if="reportItems.length === 0"><td colspan="5" class="muted">暂无执行记录</td></tr>
      </tbody></table></div>
    </div>
  </section>
</template>
