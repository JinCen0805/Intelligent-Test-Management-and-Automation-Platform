<script setup lang="ts">
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import type { ProjectTab } from '@/types'
import ApiWorkbenchView from '@/components/ApiWorkbenchView.vue'
import { useApiWorkbench } from '@/composables/useApiWorkbench'
import { useUiStore } from '@/stores/ui'
import { useProjectStore } from '@/stores/project'
import { useRequirementStore } from '@/stores/requirement'
import { useTestcaseStore } from '@/stores/testcase'
import { useAutomationStore } from '@/stores/automation'
import { useReportStore } from '@/stores/report'
import { useAiStore } from '@/stores/ai'
import { useAdminStore } from '@/stores/admin'
import { storage } from '@/storage'

const route = useRoute()
const router = useRouter()
const ui = useUiStore()
const projectStore = useProjectStore()
const requirementStore = useRequirementStore()
const testcaseStore = useTestcaseStore()
const automationStore = useAutomationStore()
const reportStore = useReportStore()
const aiStore = useAiStore()
const adminStore = useAdminStore()

const {
  activeProjectTab,
  activeApiTab,
  activeApiRequestTab,
  caseStatusFilter,
  casePriorityFilter,
  caseTypeFilter
} = storeToRefs(ui)
const {
  projects,
  selectedProjectId,
  currentProject,
  projectForm,
  projectMembers,
  canManageCurrentProject
} = storeToRefs(projectStore)
const { requirements } = storeToRefs(requirementStore)
const { testCases, filteredCases } = storeToRefs(testcaseStore)
const { automationProjects, automationTasks, executableTasks, executionRecords } = storeToRefs(automationStore)
const { failedReports, projectExecutionSummary, analyzingExecutionId, loadingExecutionDetailId } = storeToRefs(reportStore)

const { openModal } = ui
const {
  syncProjectForm,
  loadProjects,
  loadProjectMembers,
  saveProjectSettings,
  deleteCurrentProject,
  openAddMember,
  removeProjectMember,
  updateProjectMemberRole
} = projectStore
const { loadRequirements, parseRequirement, openRequirementDetail } = requirementStore
const { loadTestCases, caseStatusLabel, openNewCase, editCase, changeCaseStatus, deleteCase } = testcaseStore
const {
  loadAutomationData,
  openNewAutomationProject,
  openBindingManager,
  editAutomationProject,
  openOnboarding,
  openAiTask,
  openNewTask,
  editTask,
  deleteTask,
  toggleTaskStatus,
  stopTask,
  taskActiveExecution,
  openRunModal,
  syncExecution,
  retryExecution,
  deleteExecution
} = automationStore
const {
  loadReportCenter,
  analyzeExecution,
  openExecutionDetail,
  openAllure,
  downloadAllure,
  openReference
} = reportStore
const { openGenerationModal } = aiStore
const { loadAiConfig } = aiStore
const { loadCredentialGrants } = adminStore

const projectTabs: { key: ProjectTab; label: string; icon: string }[] = [
  { key: 'overview', label: '项目概览', icon: '▦' },
  { key: 'requirements', label: '需求管理', icon: '◫' },
  { key: 'cases', label: '测试用例', icon: '▤' },
  { key: 'api-workbench', label: '接口测试工作台', icon: '⌁' },
  { key: 'api-code', label: '接口自动化代码', icon: '⌘' },
  { key: 'ui', label: 'UI 自动化', icon: '⌗' },
  { key: 'tasks', label: '测试任务', icon: '▶' },
  { key: 'reports', label: '测试报告', icon: '▥' },
  { key: 'settings', label: '项目设置', icon: '⚙' }
]

const PROJECT_TAB_KEYS = projectTabs.map((tab) => tab.key)

function isProjectTab(value: unknown): value is ProjectTab {
  return typeof value === 'string' && (PROJECT_TAB_KEYS as string[]).includes(value)
}

function selectTab(key: ProjectTab) {
  activeProjectTab.value = key
  if (route.query.tab !== key) {
    void router.replace({ query: { ...route.query, tab: key } })
  }
}

const {
  apiCollections,
  apiRequests,
  apiVariables,
  apiHistory,
  selectedApiCollectionId,
  selectedApiRequestId,
  apiResponse,
  apiCollectionForm,
  apiVariableForm,
  apiRequestForm,
  loadApiWorkbench,
  loadApiRequests,
  applyApiRequest,
  createApiCollection,
  saveApiRequest,
  sendApiRequest,
  executeApiCollection,
  saveApiVariable,
  variableToken
} = useApiWorkbench()

// ==================== 项目数据加载 ====================
async function loadCurrentProjectData() {
  await Promise.all([
    loadRequirements(),
    loadTestCases(),
    loadAutomationData(),
    loadProjectMembers(),
    loadCredentialGrants(),
    loadAiConfig()
  ])
}

async function loadProjectData() {
  const id = Number(route.params.id)
  if (!id) return
  selectedProjectId.value = id
  storage.projectId = id
  if (projects.value.length === 0) await loadProjects()
  const projectExists = projects.value.some((project) => project.id === id)
  if (!projectExists) {
    ui.showToast('项目不存在或您没有访问权限')
    void router.replace({ name: 'projects' })
    return
  }
  selectedProjectId.value = id
  syncProjectForm()
  await loadCurrentProjectData()
  await loadReportCenter(id)
}

watch(() => route.params.id, () => { void loadProjectData() }, { immediate: true })

watch(
  () => route.query.tab,
  (tab) => {
    if (isProjectTab(tab) && tab !== activeProjectTab.value) {
      activeProjectTab.value = tab
    }
  },
  { immediate: true }
)
</script>

<template>
  <section class="view">
    <div class="project-top"><div class="project-title"><div class="project-logo">{{ currentProject.name.slice(0, 1) }}</div><div><h2>{{ currentProject.name }} <span class="role-chip">{{ currentProject.code }}</span></h2><p>负责人 {{ currentProject.owner }} · {{ currentProject.members }} 位项目成员 · 创建于 {{ currentProject.createdAt ? new Date(currentProject.createdAt).toLocaleDateString('zh-CN') : '--' }}</p></div></div></div>
    <div class="project-layout">
      <nav class="project-nav card">
        <button v-for="tab in projectTabs" :key="tab.key" :class="{ active: activeProjectTab === tab.key }" @click="selectTab(tab.key)"><span>{{ tab.icon }}</span>{{ tab.label }}</button>
      </nav>
      <div class="project-content">
        <template v-if="activeProjectTab === 'overview'">
          <div class="page-head compact"><div><h1>项目概览</h1><p>查看项目质量趋势、AI 分析和最近执行。</p></div></div>
          <div class="stats-grid grid"><div class="card stat"><span class="stat-label">需求文档</span><strong class="stat-value">{{ requirements.length }}</strong><span class="stat-foot">{{ requirements.filter((item) => item.status === '已解析').length }} 份已解析</span></div><div class="card stat"><span class="stat-label">测试用例</span><strong class="stat-value">{{ testCases.length }}</strong><span class="stat-foot trend-up">{{ testCases.filter((item) => item.status === 'REVIEW').length }} 条待 Review</span></div><div class="card stat"><span class="stat-label">接口自动化</span><strong class="stat-value">{{ automationProjects.filter((item) => item.automationType === 'API').length }}</strong><span class="stat-foot">来自自动化项目配置</span></div><div class="card stat"><span class="stat-label">UI 自动化</span><strong class="stat-value">{{ automationProjects.filter((item) => item.automationType === 'UI').length }}</strong><span class="stat-foot">来自自动化项目配置</span></div></div>
          <div class="section-grid grid"><div class="card"><div class="card-head"><h3>AI 工作流</h3></div><div class="card-body"><div class="flow compact-flow"><span class="done">1 创建项目</span><i>→</i><span class="done">2 上传 PRD</span><i>→</i><span class="done">3 生成用例</span><i>→</i><span class="current">4 Review</span><i>→</i><span>5 自动化执行</span></div><div class="ai-callout"><strong>建议动作</strong><p>当前有 {{ testCases.filter((item) => item.status === 'DRAFT').length }} 条 AI Draft 等待人工 Review，完成后可以关联接口自动化项目。</p><button class="btn" @click="selectTab('cases')">去 Review 用例</button></div></div></div><div class="card"><div class="card-head"><h3>最近失败</h3><span class="muted">执行报告</span></div><div class="card-body"><div v-if="failedReports[0]" class="failure-box"><strong>Execution #{{ failedReports[0].id }} · {{ failedReports[0].status }}</strong><p>{{ failedReports[0].report?.failureDetail || failedReports[0].report?.aiAnalysis || '暂无失败详情' }}</p><button class="btn" @click="selectTab('reports')">查看执行报告</button></div><div v-else class="muted">当前没有失败执行。</div></div></div></div>
        </template>
        <template v-else-if="activeProjectTab === 'requirements'">
          <div class="page-head compact"><div><h1>需求管理</h1><p>上传 PRD，跟踪解析状态并沉淀需求到测试用例。</p></div><div class="head-actions"><button class="btn" @click="openModal('requirement')">⇧ 上传需求</button></div></div>
           <div class="card"><div class="card-body table-wrap"><table><thead><tr><th>文档名称</th><th>版本</th><th>格式</th><th>解析状态</th><th>更新时间</th><th></th></tr></thead><tbody><tr v-for="requirement in requirements" :key="requirement.id || requirement.name"><td><strong>{{ requirement.name }}</strong></td><td>{{ requirement.version }}</td><td><span class="tag blue">{{ requirement.type }}</span></td><td><span class="tag" :class="requirement.status === '已解析' ? 'green' : requirement.status === '解析失败' ? 'red' : 'orange'">{{ requirement.status }}</span></td><td>{{ requirement.updated }}</td><td><button class="btn" @click="requirement.status === '解析中' || requirement.status === '解析失败' ? parseRequirement(requirement) : openRequirementDetail(requirement)">{{ requirement.status === '解析中' || requirement.status === '解析失败' ? '解析' : '查看' }}</button></td></tr><tr v-if="requirements.length === 0"><td colspan="6" class="muted">暂无需求文档</td></tr></tbody></table></div></div>
        </template>
        <template v-else-if="activeProjectTab === 'cases'">
          <div class="page-head compact"><div><h1>测试用例</h1><p>维护 AI Draft、人工 Review 和正式测试用例。</p></div><div class="head-actions"><button class="btn" @click="openNewCase">＋ 新建用例</button><button class="btn primary" @click="openGenerationModal">✦ AI 生成用例</button></div></div>
          <div class="card"><div class="card-head"><div class="subtabs"><button :class="{ active: !caseStatusFilter }" @click="caseStatusFilter = ''">全部 <span class="count">{{ testCases.length }}</span></button><button :class="{ active: caseStatusFilter === 'REVIEW' }" @click="caseStatusFilter = 'REVIEW'">待 Review <span class="count">{{ testCases.filter((item) => item.status === 'REVIEW').length }}</span></button><button :class="{ active: caseStatusFilter === 'APPROVED' }" @click="caseStatusFilter = 'APPROVED'">已通过 <span class="count">{{ testCases.filter((item) => item.status === 'APPROVED').length }}</span></button></div><div class="head-actions"><select v-model="casePriorityFilter"><option value="">全部优先级</option><option>P0</option><option>P1</option><option>P2</option><option>P3</option></select><select v-model="caseTypeFilter"><option value="">全部类型</option><option value="功能">功能</option><option value="异常">异常</option><option value="边界">边界</option><option value="其他">其他</option></select><button class="btn" @click="loadTestCases">刷新</button></div></div><div class="card-body table-wrap"><table><thead><tr><th>编号</th><th>标题</th><th>模块</th><th>优先级</th><th>类型</th><th>来源</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="testCase in filteredCases" :key="testCase.id"><td><strong>{{ testCase.no }}</strong></td><td>{{ testCase.title }}</td><td>{{ testCase.module }}</td><td><span class="tag" :class="testCase.priority === 'P0' ? 'red' : 'orange'">{{ testCase.priority }}</span></td><td>{{ testCase.type }}</td><td>{{ testCase.source === 'AI' ? 'AI' : '手工' }}</td><td><span class="tag" :class="testCase.status === 'APPROVED' ? 'green' : testCase.status === 'REVIEW' ? 'orange' : testCase.status === 'REJECTED' ? 'red' : 'gray'">{{ caseStatusLabel(testCase.status) }}</span></td><td><div class="head-actions"><button class="btn" @click="editCase(testCase)">编辑</button><button v-if="testCase.status === 'DRAFT' || testCase.status === 'REJECTED'" class="btn" @click="changeCaseStatus(testCase, 'REVIEW')">送 Review</button><button v-if="testCase.status === 'REVIEW'" class="btn primary" @click="changeCaseStatus(testCase, 'APPROVED')">通过</button><button v-if="testCase.status === 'REVIEW'" class="btn" @click="changeCaseStatus(testCase, 'REJECTED')">退回</button><button class="btn" @click="deleteCase(testCase)">删除</button></div></td></tr><tr v-if="filteredCases.length === 0"><td colspan="8" class="muted">暂无测试用例</td></tr></tbody></table></div></div>
        </template>
        <template v-else-if="activeProjectTab === 'api-workbench'">
          <ApiWorkbenchView />
          <div v-if="false">
          <div class="page-head compact">
            <div><h1>接口测试工作台</h1><p>管理接口请求、断言与环境变量。</p></div>
            <div class="head-actions"><select v-model="apiVariableForm.environment" @change="loadApiWorkbench"><option>DEV</option><option>TEST</option><option>STAGING</option></select><button class="btn" @click="loadApiWorkbench">刷新</button></div>
          </div>
          <div class="card api-toolbar">
             <div class="api-toolbar-row"><select v-model="apiRequestForm.method" class="api-method"><option>GET</option><option>POST</option><option>PUT</option><option>PATCH</option><option>DELETE</option></select><input v-model="apiRequestForm.url" placeholder="https://api.example.com/orders" /><button class="btn primary" :disabled="!selectedApiCollectionId" @click="sendApiRequest">发送</button><button class="btn" :disabled="!selectedApiCollectionId" @click="executeApiCollection">执行集合</button><button class="btn" :disabled="!selectedApiCollectionId" @click="saveApiRequest">保存请求</button></div>
            <div class="two-col api-request-meta"><div class="field"><label>请求名称</label><input v-model="apiRequestForm.name" /></div><div class="field"><label>当前集合</label><select v-model.number="selectedApiCollectionId" @change="selectedApiCollectionId && loadApiRequests(selectedApiCollectionId)"><option :value="null" disabled>请选择集合</option><option v-for="collection in apiCollections" :key="collection.id" :value="collection.id">{{ collection.name }}</option></select></div></div>
          </div>
          <div class="api-layout">
            <div class="card"><div class="card-head"><h3>请求集合</h3><span class="muted">{{ apiRequests.length }} 个请求</span></div><div class="api-side-list"><button v-for="request in apiRequests" :key="request.id" :class="{ active: selectedApiRequestId === request.id }" @click="applyApiRequest(request)"><span class="method" :class="request.method.toLowerCase()">{{ request.method }}</span>{{ request.name }}</button><div v-if="apiRequests.length === 0" class="muted api-empty">暂无请求，请先保存当前请求。</div></div><form class="card-body" @submit.prevent="createApiCollection"><div class="field"><label>新建集合</label><input v-model="apiCollectionForm.name" placeholder="例如：订单回归集合" /></div><button class="btn" :disabled="!apiCollectionForm.name.trim()">创建集合</button></form></div>
             <div class="card"><div class="card-head"><div class="subtabs"><button v-for="tab in ['params', 'headers', 'body', 'assertions', 'pre-script', 'post-script']" :key="tab" :class="{ active: activeApiRequestTab === tab }" @click="activeApiRequestTab = tab as typeof activeApiRequestTab">{{ { params: '参数 JSON', headers: 'Headers JSON', body: 'Body', assertions: '断言 JSON', 'pre-script': '前置脚本', 'post-script': '后置脚本' }[tab] }}</button></div></div><div class="card-body"><div v-if="activeApiRequestTab === 'params'" class="field"><label>Query 参数</label><textarea v-model="apiRequestForm.paramsJson" rows="9"></textarea></div><div v-else-if="activeApiRequestTab === 'headers'" class="field"><label>请求 Headers</label><textarea v-model="apiRequestForm.headersJson" rows="9"></textarea></div><div v-else-if="activeApiRequestTab === 'body'" class="field"><label>请求 Body</label><textarea v-model="apiRequestForm.body" rows="12"></textarea></div><div v-else-if="activeApiRequestTab === 'assertions'" class="field"><label>断言配置</label><textarea v-model="apiRequestForm.assertionsJson" rows="12"></textarea><span class="muted">支持 status、json、field、contains、header 类型，JSON 路径示例：$.data.orderStatus</span></div><div v-else-if="activeApiRequestTab === 'pre-script'" class="field"><label>前置脚本</label><textarea v-model="apiRequestForm.preRequestScript" rows="12" placeholder="set token=demo&#10;extract order_id=$.data.id"></textarea><span class="muted">支持 set name=value 和 extract name=$.path。</span></div><div v-else class="field"><label>后置脚本</label><textarea v-model="apiRequestForm.postRequestScript" rows="12" placeholder="extract order_id=$.data.id"></textarea><span class="muted">后置脚本可以从响应 JSON 提取变量，供集合中的后续请求使用。</span></div></div></div>
          </div>
          <div v-if="apiResponse" class="card response-card"><div class="card-head"><h3>响应结果</h3><span class="status" :class="apiResponse?.status === 'PASSED' ? 'passed' : 'failed'">{{ apiResponse?.statusCode || '--' }} {{ apiResponse?.status }}</span></div><div class="card-body"><div class="response-meta"><span>耗时 {{ apiResponse?.durationMs || 0 }} ms</span><span>{{ apiResponse?.environment }}</span><span>{{ apiResponse?.requestName }}</span></div><pre>{{ apiResponse?.responseBody || '无响应体' }}</pre><details v-if="apiResponse?.assertionResultsJson" class="response-details"><summary>断言结果</summary><pre>{{ apiResponse?.assertionResultsJson }}</pre></details></div></div>
           <div class="card api-bottom"><div class="card-body"><div class="subtabs"><button :class="{ active: activeApiTab === 'collections' }" @click="activeApiTab = 'collections'">集合</button><button :class="{ active: activeApiTab === 'variables' }" @click="activeApiTab = 'variables'; loadApiWorkbench()">环境变量</button><button :class="{ active: activeApiTab === 'history' }" @click="activeApiTab = 'history'">执行历史</button></div><div v-if="activeApiTab === 'collections'" class="collection-grid"><div v-for="collection in apiCollections" :key="collection.id" class="collection-item"><span class="mini-icon">O</span><div><strong>{{ collection.name }}</strong><small>{{ apiRequests.length }} 个当前请求 · {{ collection.description || '可执行' }}</small></div><span class="tag green">已持久化</span></div><div v-if="apiCollections.length === 0" class="muted">暂无集合</div></div><div v-else-if="activeApiTab === 'variables'" class="table-wrap"><form class="variable-form" @submit.prevent="saveApiVariable"><input v-model="apiVariableForm.name" placeholder="变量名" /><input v-model="apiVariableForm.value" placeholder="变量值" /><input v-model="apiVariableForm.source" placeholder="来源" /><label class="switch-row"><input v-model="apiVariableForm.sensitive" type="checkbox" />敏感</label><button class="btn primary">保存变量</button></form><table><thead><tr><th>变量名</th><th>值</th><th>来源</th><th>敏感</th></tr></thead><tbody><tr v-for="variable in apiVariables" :key="variable.id"><td><strong>{{ variableToken(variable.name) }}</strong></td><td>{{ variable.value || '--' }}</td><td>{{ variable.source || '--' }}</td><td><span class="tag" :class="variable.sensitive ? 'red' : 'gray'">{{ variable.sensitive ? '是' : '否' }}</span></td></tr><tr v-if="apiVariables.length === 0"><td colspan="4" class="muted">当前环境暂无变量</td></tr></tbody></table></div><div v-else class="table-wrap"><table><thead><tr><th>执行时间</th><th>请求</th><th>环境</th><th>HTTP</th><th>状态</th><th></th></tr></thead><tbody><tr v-for="history in apiHistory" :key="history.id"><td>{{ new Date(history.startedAt).toLocaleString('zh-CN') }}</td><td>{{ history.requestName }}</td><td>{{ history.environment }}</td><td>{{ history.statusCode || '--' }}</td><td><span class="status" :class="history.status === 'PASSED' ? 'passed' : 'failed'">{{ history.status }}</span></td><td><button class="btn" @click="apiResponse = history">查看</button></td></tr><tr v-if="apiHistory.length === 0"><td colspan="6" class="muted">暂无执行历史</td></tr></tbody></table></div></div></div>
            </div>
         </template>
        <template v-else-if="activeProjectTab === 'api-code'">
          <div class="page-head compact"><div><h1>接口自动化代码</h1><p>管理 Git 仓库配置，平台通过 Jenkins 执行真实测试项目。</p></div><div class="head-actions"><button class="btn" @click="openNewAutomationProject()">＋ 新建自动化项目</button></div></div>
          <div v-for="project in automationProjects.filter((item) => item.automationType === 'API')" :key="project.id" class="card">
            <div class="card-head"><div><h3>{{ project.name }}</h3><span class="muted">{{ project.framework }} · {{ project.status }}</span></div><div class="head-actions"><button class="btn primary" @click="openOnboarding(project)">初始化</button><button class="btn" @click="openBindingManager(project)">执行目标</button><button class="btn" @click="editAutomationProject(project)">编辑配置</button></div></div>
          </div>
          <div v-if="automationProjects.filter((item) => item.automationType === 'API').length === 0" class="card"><div class="card-body muted">暂无接口自动化项目，请先创建配置。</div></div>
        </template>
        <template v-else-if="activeProjectTab === 'ui'">
          <div class="page-head compact"><div><h1>UI 自动化</h1><p>管理 Playwright 项目、执行环境和失败资产。</p></div><div class="head-actions"><button class="btn" @click="openNewAutomationProject('UI')">＋ 新建 UI 项目</button></div></div>
          <div v-for="project in automationProjects.filter((item) => item.automationType === 'UI')" :key="project.id" class="card">
            <div class="card-head"><div><h3>{{ project.name }}</h3><span class="muted">{{ project.framework }} · {{ project.status }}</span></div><div class="head-actions"><button class="btn primary" @click="openOnboarding(project)">初始化</button><button class="btn" @click="openBindingManager(project)">执行目标</button><button class="btn" @click="editAutomationProject(project)">编辑配置</button></div></div>
          </div>
          <div v-if="automationProjects.filter((item) => item.automationType === 'UI').length === 0" class="card"><div class="card-body muted">暂无 UI 自动化项目，请先创建配置。</div></div>        </template>
        <template v-else-if="activeProjectTab === 'tasks'">
          <div class="page-head compact"><div><h1>测试任务</h1><p>统一关联环境、Branch、Jenkins Job 和执行参数。</p></div><div class="head-actions"><button class="btn" @click="openAiTask()">✦ AI 创建</button><button class="btn" @click="openNewTask()">＋ 新建测试任务</button><button class="btn primary" :disabled="executableTasks.length === 0" @click="openRunModal()">▶ 立即执行</button></div></div>
          <div class="card"><div class="card-body table-wrap"><table><thead><tr><th>测试任务</th><th>类型</th><th>自动化项目</th><th>Jenkins Job</th><th>状态</th><th></th></tr></thead><tbody><tr v-for="task in automationTasks" :key="task.id"><td><strong>{{ task.name }}</strong><br><span class="muted">{{ task.parametersJson ? '含执行参数' : '无执行参数' }}</span></td><td><span class="tag blue">{{ task.taskType }}</span></td><td>{{ automationProjects.find((item) => item.id === task.automationProjectId)?.name || '--' }}</td><td>{{ task.jenkinsJob }}</td><td><span class="tag" :class="task.status === 'ENABLED' ? 'green' : 'gray'">{{ task.status === 'ENABLED' ? '启用' : '禁用' }}</span></td><td><div class="head-actions"><button v-if="task.status === 'ENABLED'" class="btn" @click="openRunModal(task)">执行</button><button v-if="taskActiveExecution(task)" class="btn" @click="stopTask(task)">停止</button><button class="btn" @click="editTask(task)">编辑</button><button class="btn" @click="toggleTaskStatus(task)">{{ task.status === 'ENABLED' ? '禁用' : '启用' }}</button><button class="btn danger-text" @click="deleteTask(task)">删除</button></div></td></tr><tr v-if="automationTasks.length === 0"><td colspan="6" class="muted">暂无测试任务</td></tr></tbody></table></div></div>
        </template>
        <template v-else-if="activeProjectTab === 'reports'">
          <div class="page-head compact"><div><h1>项目测试报告</h1><p>查看 Allure 详情、历史执行、失败日志与 AI 分析。</p></div><div class="head-actions"><button v-if="executionRecords[0]?.report?.allureUrl" class="btn primary" @click="openAllure(executionRecords[0])">打开 Allure</button><button v-if="executionRecords[0]?.report?.artifactRef === 'OFFLINE_ALLURE'" class="btn" @click="downloadAllure(executionRecords[0])">下载 ZIP</button></div></div>
          <div class="report-kpis"><div class="card report-kpi"><span>Total</span><strong>{{ projectExecutionSummary.total }}</strong></div><div class="card report-kpi"><span>Passed</span><strong class="trend-up">{{ projectExecutionSummary.passed }}</strong></div><div class="card report-kpi"><span>Failed</span><strong class="danger-text">{{ projectExecutionSummary.failed }}</strong></div><div class="card report-kpi"><span>Broken</span><strong class="warning-text">{{ projectExecutionSummary.broken }}</strong></div><div class="card report-kpi"><span>Skipped</span><strong>{{ projectExecutionSummary.skipped }}</strong></div></div>
          <div class="card report-callout"><div class="card-body"><div class="ai-callout"><strong>失败分析</strong><p class="muted">{{ failedReports.length ? `已持久化 ${failedReports.length} 条失败执行的基础分析，可从下方历史报告打开日志和资产。` : '当前没有失败执行。同步 Jenkins 结果后会生成基础失败分析。' }}</p></div></div></div>
          <div class="card"><div class="card-head"><h3>历史报告</h3><span class="muted">每次执行独立沉淀</span></div><div class="card-body table-wrap"><table><thead><tr><th>Execution</th><th>测试任务</th><th>Build</th><th>结果</th><th>Duration</th><th>Allure</th><th></th></tr></thead><tbody><tr v-for="execution in executionRecords" :key="execution.id"><td>#{{ execution.id }}<br><span class="muted">{{ execution.triggeredBy || '系统' }} · {{ execution.triggerMethod || 'MANUAL' }}</span></td><td>{{ automationTasks.find((item) => item.id === execution.testTaskId)?.name || '未知任务' }}</td><td>{{ execution.buildNumber || '队列中' }}</td><td><span class="tag" :class="execution.status === 'PASSED' ? 'green' : execution.status === 'FAILED' ? 'red' : 'orange'">{{ execution.status }}</span></td><td>{{ execution.durationMs ? `${Math.round(execution.durationMs / 1000)}s` : '--' }}</td><td><div class="head-actions"><button v-if="execution.report?.allureUrl" class="link-btn" @click="openAllure(execution)">打开</button><button v-if="execution.report?.artifactRef === 'OFFLINE_ALLURE'" class="btn" @click="downloadAllure(execution)">下载 ZIP</button><span v-if="!execution.report?.allureUrl && !execution.report?.artifactRef">--</span></div></td><td><div class="head-actions"><button class="btn" :disabled="loadingExecutionDetailId === execution.id" @click="openExecutionDetail(execution)">详情</button><button class="btn" @click="syncExecution(execution)">同步</button><button v-if="['FAILED', 'ABORTED', 'TIMEOUT'].includes(execution.status)" class="btn primary" :disabled="analyzingExecutionId === execution.id" @click="analyzeExecution(execution)"><span v-if="analyzingExecutionId === execution.id" class="spinner"></span>{{ analyzingExecutionId === execution.id ? '分析中...' : 'AI 分析' }}</button><button v-if="execution.status === 'FAILED' || execution.status === 'ABORTED' || execution.status === 'TIMEOUT'" class="btn" @click="retryExecution(execution)">重试</button><button class="btn danger-text" @click="deleteExecution(execution)">删除</button></div></td></tr><tr v-if="executionRecords.length === 0"><td colspan="7" class="muted">暂无执行记录</td></tr></tbody></table></div></div>
        </template>
        <template v-else>
          <div class="page-head compact"><div><h1>项目设置</h1><p>管理项目成员、权限、代码仓库和测试用例字段规范。</p></div><div v-if="canManageCurrentProject" class="head-actions"><button class="btn" @click="deleteCurrentProject">删除项目</button><button class="btn primary" @click="saveProjectSettings">保存设置</button></div></div>
          <div class="section-grid grid"><div class="card"><div class="card-head"><h3>基本信息</h3></div><div class="card-body"><div class="field"><label>项目名称</label><input v-model="projectForm.name" :disabled="!canManageCurrentProject" /></div><div class="field"><label>项目标识</label><input :value="currentProject.code" disabled /></div><div class="field"><label>项目状态</label><select v-model="projectForm.status" :disabled="!canManageCurrentProject"><option value="RUNNING">运行中</option><option value="TESTING">测试中</option></select></div><div class="field"><label>项目描述</label><textarea v-model="projectForm.description" :disabled="!canManageCurrentProject"></textarea></div></div></div><div class="card"><div class="card-head"><h3>项目成员</h3><button v-if="canManageCurrentProject" class="btn" @click="openAddMember">＋ 添加成员</button></div><div class="card-body table-wrap"><table><thead><tr><th>成员</th><th>邮箱</th><th>角色</th><th></th></tr></thead><tbody><tr v-for="member in projectMembers" :key="member.userId"><td><strong>{{ member.displayName }}</strong><br><span class="muted">{{ member.username }}</span></td><td>{{ member.email || '--' }}</td><td><select v-if="member.role !== 'OWNER' && canManageCurrentProject" :value="member.role" @change="updateProjectMemberRole(member, $event)" class="role-select"><option value="MANAGER">项目管理员</option><option value="TEST_DEVELOPER">测试开发</option><option value="VIEWER">只读成员</option></select><span v-else class="role-chip">{{ member.role }}</span></td><td><button v-if="member.role !== 'OWNER' && canManageCurrentProject" class="btn" @click="removeProjectMember(member)">移除</button></td></tr><tr v-if="projectMembers.length === 0"><td colspan="4" class="muted">暂无成员</td></tr></tbody></table></div></div></div>
        </template>
      </div>
    </div>
  </section>
</template>
