<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import type { ProjectTab } from '@/types'
import ApiWorkbenchView from '@/components/ApiWorkbenchView.vue'
import ProjectCaseAiView from '@/components/project/ProjectCaseAiView.vue'
import ProjectCaseLibraryView from '@/components/project/ProjectCaseLibraryView.vue'
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
const { allTestCases } = storeToRefs(testcaseStore)
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
const { loadRequirements, openRequirementDetail } = requirementStore
const { loadTestCases } = testcaseStore
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
const { loadAiConfig } = aiStore
const { loadCredentialGrants } = adminStore

const caseTabs: { key: Extract<ProjectTab, 'case-ai' | 'case-library'>; label: string; icon: string }[] = [
  { key: 'case-ai', label: 'AI 生成测试用例', icon: '✦' },
  { key: 'case-library', label: '用例库', icon: '▤' }
]

const projectTabs: { key: ProjectTab; label: string; icon: string }[] = [
  { key: 'overview', label: '项目概览', icon: '▦' },
  { key: 'requirements', label: '项目文件管理', icon: '◫' },
  { key: 'api-workbench', label: '接口测试工作台', icon: '⌁' },
  { key: 'api-code', label: '接口自动化代码', icon: '⌘' },
  { key: 'ui', label: 'UI 自动化', icon: '⌗' },
  { key: 'tasks', label: '测试任务', icon: '▶' },
  { key: 'reports', label: '测试报告', icon: '▥' },
  { key: 'settings', label: '项目设置', icon: '⚙' }
]

const PROJECT_TAB_KEYS = [...projectTabs.map((tab) => tab.key), ...caseTabs.map((tab) => tab.key)]
const caseMenuOpen = ref(false)

function isProjectTab(value: unknown): value is ProjectTab {
  return typeof value === 'string' && (PROJECT_TAB_KEYS as string[]).includes(value)
}

function isCaseTab(value: unknown): value is Extract<ProjectTab, 'case-ai' | 'case-library'> {
  return typeof value === 'string' && (caseTabs.map((tab) => tab.key) as string[]).includes(value)
}

function selectTab(key: ProjectTab) {
  activeProjectTab.value = key
  if (isCaseTab(key)) {
    caseMenuOpen.value = true
  }
  if (route.query.tab !== key) {
    void router.replace({ query: { ...route.query, tab: key } })
  }
}

// ==================== 项目数据加载 ====================
async function loadCurrentProjectData() {
  await Promise.all([
    loadRequirements(),
    loadTestCases({ source: 'AI', status: '', keyword: '', priority: '', caseType: '', suiteType: ui.activeCaseSuiteType }),
    testcaseStore.loadAllTestCases(),
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
      caseMenuOpen.value = isCaseTab(tab)
    }
  },
  { immediate: true }
)

watch(activeProjectTab, (tab) => {
  if (isCaseTab(tab)) {
    caseMenuOpen.value = true
  }
})
</script>

<template>
  <section class="view">
    <div class="project-top"><div class="project-title"><div class="project-logo">{{ currentProject.name.slice(0, 1) }}</div><div><h2>{{ currentProject.name }} <span class="role-chip">{{ currentProject.code }}</span></h2><p>负责人 {{ currentProject.owner }} · {{ currentProject.members }} 位项目成员 · 创建于 {{ currentProject.createdAt ? new Date(currentProject.createdAt).toLocaleDateString('zh-CN') : '--' }}</p></div></div></div>
    <div class="project-layout">
      <nav class="project-nav card">
        <template v-for="tab in projectTabs" :key="tab.key">
          <button :class="{ active: activeProjectTab === tab.key }" @click="selectTab(tab.key)"><span>{{ tab.icon }}</span>{{ tab.label }}</button>
          <div v-if="tab.key === 'requirements'" class="project-nav-group" :class="{ active: isCaseTab(activeProjectTab), open: caseMenuOpen }">
            <button class="project-nav-parent" type="button" @click="caseMenuOpen = !caseMenuOpen">
              <span>▤</span>
              测试用例
              <i>⌄</i>
            </button>
            <div v-show="caseMenuOpen" class="project-nav-submenu">
              <button v-for="caseTab in caseTabs" :key="caseTab.key" :class="{ active: activeProjectTab === caseTab.key }" @click="selectTab(caseTab.key)">
                <span>{{ caseTab.icon }}</span>{{ caseTab.label }}
              </button>
            </div>
          </div>
        </template>
      </nav>
      <div class="project-content">
        <template v-if="activeProjectTab === 'overview'">
          <div class="page-head compact"><div><h1>项目概览</h1><p>查看项目质量趋势、AI 分析和最近执行。</p></div></div>
          <div class="stats-grid grid"><div class="card stat"><span class="stat-label">项目文件</span><strong class="stat-value">{{ requirements.length }}</strong><span class="stat-foot">集中存储的项目资料</span></div><div class="card stat"><span class="stat-label">测试用例</span><strong class="stat-value">{{ allTestCases.length }}</strong><span class="stat-foot trend-up">{{ allTestCases.filter((item) => item.source === 'AI' && item.status !== 'APPROVED').length }} 条待处理</span></div><div class="card stat"><span class="stat-label">接口自动化</span><strong class="stat-value">{{ automationProjects.filter((item) => item.automationType === 'API').length }}</strong><span class="stat-foot">来自自动化项目配置</span></div><div class="card stat"><span class="stat-label">UI 自动化</span><strong class="stat-value">{{ automationProjects.filter((item) => item.automationType === 'UI').length }}</strong><span class="stat-foot">来自自动化项目配置</span></div></div>
          <div class="section-grid grid"><div class="card"><div class="card-head"><h3>AI 工作流</h3></div><div class="card-body"><div class="flow compact-flow"><span class="done">1 创建项目</span><i>→</i><span class="done">2 添加文件</span><i>→</i><span class="done">3 生成用例</span><i>→</i><span class="current">4 人工确认</span><i>→</i><span>5 自动化执行</span></div><div class="ai-callout"><strong>建议动作</strong><p>当前有 {{ allTestCases.filter((item) => item.source === 'AI' && item.status !== 'APPROVED').length }} 条 AI 用例待处理，完成后可以关联接口自动化项目。</p><button class="btn" @click="selectTab('case-ai')">去 AI 生成用例</button></div></div></div><div class="card"><div class="card-head"><h3>最近失败</h3><span class="muted">执行报告</span></div><div class="card-body"><div v-if="failedReports[0]" class="failure-box"><strong>Execution #{{ failedReports[0].id }} · {{ failedReports[0].status }}</strong><p>{{ failedReports[0].report?.failureDetail || failedReports[0].report?.aiAnalysis || '暂无失败详情' }}</p><button class="btn" @click="selectTab('reports')">查看执行报告</button></div><div v-else class="muted">当前没有失败执行。</div></div></div></div>
        </template>
        <template v-else-if="activeProjectTab === 'requirements'">
          <div class="page-head compact"><div><h1>项目文件管理</h1><p>集中保存当前项目使用的文件，供团队和 AI 用例生成选择。</p></div><div class="head-actions"><button class="btn primary" @click="openModal('requirement')">＋ 添加文件</button></div></div>
           <div class="card"><div class="card-body table-wrap"><table><thead><tr><th>文件名称</th><th>备注</th><th>格式</th><th>存储状态</th><th>更新时间</th><th></th></tr></thead><tbody><tr v-for="requirement in requirements" :key="requirement.id || requirement.name"><td><strong>{{ requirement.name }}</strong></td><td>{{ requirement.version || '--' }}</td><td><span class="tag blue">{{ requirement.type }}</span></td><td><span class="tag green">已存储</span></td><td>{{ requirement.updated }}</td><td><button class="btn" @click="openRequirementDetail(requirement)">查看信息</button></td></tr><tr v-if="requirements.length === 0"><td colspan="6" class="muted">暂无项目文件</td></tr></tbody></table></div></div>
        </template>
        <template v-else-if="activeProjectTab === 'case-ai'">
          <ProjectCaseAiView />
        </template>
        <template v-else-if="activeProjectTab === 'case-library'">
          <ProjectCaseLibraryView />
        </template>
        <template v-else-if="activeProjectTab === 'api-workbench'">
          <ApiWorkbenchView />
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
