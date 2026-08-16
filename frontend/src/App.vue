<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  api,
  type ApiAiModel,
  type ApiAutomationProject,
  type ApiExecution,
  type ApiGeneratedCase,
  type ApiProjectMember,
  type ApiPromptTemplate,
  type ApiProject,
  type ApiRequirement,
  type ApiTestTask,
  type ApiTestCase,
  type ApiUser
} from './api'

type View = 'home' | 'projects' | 'project' | 'ai' | 'reports' | 'admin'
type ProjectTab =
  | 'overview'
  | 'requirements'
  | 'cases'
  | 'api-workbench'
  | 'api-code'
  | 'ui'
  | 'tasks'
  | 'reports'
  | 'settings'
type ModalName = 'quick' | 'project' | 'requirement' | 'requirementDetail' | 'run' | 'case' | 'caseEdit' | 'analysis' | 'model' | 'prompt' | 'automationProject' | 'task' | 'user' | 'member'

interface Project {
  id?: number
  name: string
  code: string
  ownerId?: number
  owner: string
  members: number
  environment: string
  lastRun: string
  passRate: number
  status: '运行中' | '测试中'
  description: string
}

interface TestCase {
  id: number
  requirementId?: number | null
  no: string
  title: string
  module: string
  priority: 'P0' | 'P1' | 'P2' | 'P3'
  type: '功能' | '异常' | '边界' | '其他'
  status: 'DRAFT' | 'REVIEW' | 'APPROVED' | 'REJECTED'
  source: string
  precondition: string
  steps: string
  testData: string
  expectedResult: string
  tags: string
}

interface Requirement {
  id?: number
  name: string
  version: string
  type: string
  status: '已解析' | '解析中' | '解析失败'
  cases: number
  updated: string
}

interface RequirementDetail {
  name: string
  version: string
  fileType: string
  parseStatus: string
  parseError?: string
  contentText?: string
}

interface Execution {
  run: string
  project: string
  task: string
  type: string
  branch: string
  environment: string
  status: 'PASSED' | 'FAILED' | 'RUNNING'
  passRate: string
  time: string
}

const activeView = ref<View>('home')
const activeProjectTab = ref<ProjectTab>('overview')
const activeAiTab = ref<'models' | 'prompts' | 'logs'>('models')
const activeApiTab = ref<'collections' | 'variables' | 'history'>('collections')
const activeApiRequestTab = ref<'params' | 'headers' | 'body' | 'assertions'>('params')
const modal = ref<ModalName | null>(null)
const toast = ref('')
const search = ref('')
const caseStatusFilter = ref('')
const casePriorityFilter = ref('')
const caseTypeFilter = ref('')
const responseVisible = ref(false)
const generatedCases = ref(false)
const requirementDetail = ref<RequirementDetail | null>(null)
const authState = reactive({
  ready: false,
  authenticated: false,
  loading: false,
  error: '',
  username: 'lin.xiao',
  password: 'demo123',
  displayName: '林晓',
  role: 'TEST_DEVELOPER'
})

const projects = reactive<Project[]>([
  {
    name: '星河电商平台',
    code: 'XH-MALL',
    owner: '林晓',
    members: 8,
    environment: 'TEST',
    lastRun: '今天 10:24',
    passRate: 94.2,
    status: '运行中',
    description: '电商核心交易链路，覆盖用户、商品、订单、支付与售后。'
  },
  {
    name: '供应链中台',
    code: 'SC-CORE',
    owner: '王敏',
    members: 12,
    environment: 'STAGING',
    lastRun: '今天 09:18',
    passRate: 97.6,
    status: '运行中',
    description: '采购、库存和供应商协同测试项目。'
  },
  {
    name: '会员中心重构',
    code: 'MEMBER-V2',
    owner: '赵然',
    members: 5,
    environment: 'DEV',
    lastRun: '昨天 18:02',
    passRate: 88.9,
    status: '测试中',
    description: '会员权益、积分和成长体系重构。'
  }
])
const selectedProjectId = ref<number | null>(
  Number(localStorage.getItem('testpilot.projectId')) || null
)

const requirements = reactive<Requirement[]>([
  { name: '订单中心 PRD', version: 'V1.2', type: 'Markdown', status: '已解析', cases: 86, updated: '今天 09:58' },
  { name: '支付接口说明', version: 'V2.0', type: 'PDF', status: '已解析', cases: 42, updated: '昨天 16:30' },
  { name: '会员权益需求', version: 'V0.9', type: 'DOCX', status: '解析中', cases: 0, updated: '昨天 14:12' }
])

const testCases = reactive<TestCase[]>([])

const executions = reactive<Execution[]>([
  { run: '#104', project: '星河电商平台', task: '订单接口回归', type: '接口自动化', branch: 'main', environment: 'TEST', status: 'PASSED', passRate: '100%', time: '今天 10:24' },
  { run: '#103', project: '星河电商平台', task: 'Web UI Smoke', type: 'UI 自动化', branch: 'release/1.8', environment: 'STAGING', status: 'FAILED', passRate: '96.7%', time: '今天 09:46' },
  { run: '#102', project: '供应链中台', task: '库存回归', type: '接口自动化', branch: 'develop', environment: 'STAGING', status: 'PASSED', passRate: '100%', time: '今天 09:18' }
])
const automationTasks = reactive<ApiTestTask[]>([])
const automationProjects = reactive<ApiAutomationProject[]>([])
const executionRecords = reactive<ApiExecution[]>([])
const aiModels = reactive<ApiAiModel[]>([])
const promptTemplates = reactive<ApiPromptTemplate[]>([])
const users = reactive<ApiUser[]>([])
const projectMembers = reactive<ApiProjectMember[]>([])

const newProject = reactive({ name: '', code: '', description: '', environment: 'TEST' })
const projectForm = reactive({ name: '', description: '', defaultEnvironment: 'TEST', status: 'RUNNING' })
const runForm = reactive({ taskId: null as number | null, environment: 'TEST', branch: 'main' })
const requirementFile = ref<File | null>(null)
const requirementVersion = ref('V1.0')
const editingCaseId = ref<number | null>(null)
const isGenerating = ref(false)
const caseForm = reactive({
  requirementId: null as number | null,
  caseNo: '',
  title: '',
  moduleName: '',
  precondition: '',
  steps: '',
  testData: '',
  expectedResult: '',
  priority: 'P1',
  caseType: 'FUNCTIONAL',
  tags: ''
})
const modelForm = reactive({
  name: 'local-demo-model',
  provider: 'MOCK',
  endpoint: '',
  apiKey: '',
  enabled: true
})
const generationForm = reactive({
  requirementId: null as number | null,
  modelId: null as number | null,
  rules: '覆盖正向、异常、边界场景；输出结构化字段。'
})
const promptForm = reactive({
  name: '',
  taskType: 'TEST_CASE_GENERATION',
  content: '',
  version: 'V1.0',
  enabled: true
})
const automationProjectForm = reactive({
  name: '',
  automationType: 'API',
  repositoryUrl: '',
  branch: 'main',
  credentialRef: '',
  framework: 'pytest',
  testPath: '',
  executeCommand: '',
  environment: 'TEST',
  jenkinsJob: ''
})
const taskForm = reactive({
  name: '',
  taskType: 'REGRESSION',
  automationProjectId: null as number | null,
  environment: 'TEST',
  branch: 'main',
  jenkinsJob: '',
  parametersJson: ''
})
const generatedCaseList = reactive<ApiGeneratedCase[]>([])
const editingAutomationProjectId = ref<number | null>(null)
const editingUserId = ref<number | null>(null)
const userForm = reactive({
  username: '',
  password: '',
  displayName: '',
  email: '',
  role: 'TEST_DEVELOPER'
})
const memberForm = reactive({ userId: null as number | null, role: 'TEST_DEVELOPER' })

const navItems: { key: View; label: string; icon: string; badge?: string }[] = [
  { key: 'home', label: '首页', icon: '⌂' },
  { key: 'projects', label: '项目', icon: '▣' },
  { key: 'ai', label: 'AI 能力', icon: '✦' },
  { key: 'reports', label: '测试报告', icon: '▤', badge: '3' },
  { key: 'admin', label: '系统管理', icon: '⚙' }
]

const emptyProject: Project = {
  name: '未选择项目',
  code: '--',
  owner: '--',
  members: 0,
  environment: 'TEST',
  lastRun: '--',
  passRate: 0,
  status: '测试中',
  description: '当前账号还没有可访问的项目。'
}

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

const currentProject = computed<Project>(() => {
  return projects.find((project) => project.id === selectedProjectId.value)
    ?? projects[0]
    ?? emptyProject
})
const canManageCurrentProject = computed(() => {
  const userId = Number(localStorage.getItem('testpilot.userId'))
  return authState.role === 'ADMIN' || currentProject.value.ownerId === userId
})
const visibleProjects = computed(() => {
  const term = search.value.trim().toLowerCase()
  if (!term) return projects
  return projects.filter((project) => `${project.name}${project.code}${project.owner}`.toLowerCase().includes(term))
})
const activeViewName = computed(() => ({
  home: '平台首页',
  projects: '项目列表',
  project: currentProject.value.name,
  ai: 'AI 能力',
  reports: '测试报告',
  admin: '系统管理'
}[activeView.value]))
const filteredCases = computed(() => {
  const term = search.value.trim().toLowerCase()
  return testCases.filter((item) => {
    const matchesSearch = !term || `${item.no}${item.title}${item.module}`.toLowerCase().includes(term)
    const matchesStatus = !caseStatusFilter.value || item.status === caseStatusFilter.value
    const matchesPriority = !casePriorityFilter.value || item.priority === casePriorityFilter.value
    const matchesType = !caseTypeFilter.value || item.type === caseTypeFilter.value
    return matchesSearch && matchesStatus && matchesPriority && matchesType
  })
})
const selectedRequirementId = computed(() => {
  return requirements.find((item) => item.id && item.status === '已解析')?.id
    ?? requirements.find((item) => item.id)?.id
    ?? null
})
const reportSummary = computed(() => {
  return executionRecords.reduce((summary, execution) => {
    const report = execution.report
    if (!report) return summary
    summary.total += report.totalCount
    summary.passed += report.passedCount
    summary.failed += report.failedCount
    summary.broken += report.brokenCount
    summary.skipped += report.skippedCount
    return summary
  }, { total: 0, passed: 0, failed: 0, broken: 0, skipped: 0 })
})

function caseStatusLabel(status: TestCase['status']) {
  return {
    DRAFT: '草稿',
    REVIEW: '待 Review',
    APPROVED: '已通过',
    REJECTED: '已退回'
  }[status]
}

function caseTypeLabel(type: string) {
  return {
    FUNCTIONAL: '功能',
    EXCEPTION: '异常',
    BOUNDARY: '边界'
  }[type] || type
}

function fromApiProject(project: ApiProject): Project {
  return {
    id: project.id,
    name: project.name,
    code: project.code,
    ownerId: project.ownerId,
    owner: project.ownerName,
    members: project.memberCount ?? 1,
    environment: project.defaultEnvironment,
    lastRun: '尚未执行',
    passRate: 0,
    status: project.status === 'RUNNING' ? '运行中' : '测试中',
    description: project.description || '新建测试项目'
  }
}

function syncProjectForm() {
  const project = currentProject.value
  if (!project) return
  projectForm.name = project.name
  projectForm.description = project.description
  projectForm.defaultEnvironment = project.environment
  projectForm.status = project.status === '运行中' ? 'RUNNING' : 'TESTING'
}

function fromApiRequirement(requirement: ApiRequirement): Requirement {
  const typeMap: Record<string, string> = {
    PDF: 'PDF',
    DOCX: 'DOCX',
    MD: 'Markdown',
    MARKDOWN: 'Markdown',
    TXT: 'TXT'
  }
  const statusMap: Record<string, Requirement['status']> = {
    PARSED: '已解析',
    FAILED: '解析失败',
    PARSING: '解析中'
  }
  return {
    id: requirement.id,
    name: requirement.name,
    version: requirement.version,
    type: typeMap[requirement.fileType] || requirement.fileType,
    status: statusMap[requirement.parseStatus] || '解析中',
    cases: 0,
    updated: new Date(requirement.updatedAt).toLocaleString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
}

function fromApiTestCase(testCase: ApiTestCase): TestCase {
  return {
    id: testCase.id,
    requirementId: testCase.requirementId,
    no: testCase.caseNo,
    title: testCase.title,
    module: testCase.moduleName || '未分类',
    priority: (testCase.priority as TestCase['priority']) || 'P1',
    type: (caseTypeLabel(testCase.caseType) as TestCase['type']) || '其他',
    status: testCase.status,
    source: testCase.source,
    precondition: testCase.precondition || '',
    steps: testCase.steps || '',
    testData: testCase.testData || '',
    expectedResult: testCase.expectedResult || '',
    tags: testCase.tags || ''
  }
}

function fromApiExecution(execution: ApiExecution): Execution {
  const total = execution.report?.totalCount || 0
  const passed = execution.report?.passedCount || 0
  return {
    run: execution.buildNumber ? `Build ${execution.buildNumber}` : `Execution #${execution.id}`,
    project: currentProject.value.name,
    task: automationTasks.find((task) => task.id === execution.testTaskId)?.name || '测试任务',
    type: automationTasks.find((task) => task.id === execution.testTaskId)?.taskType || '自动化',
    branch: automationTasks.find((task) => task.id === execution.testTaskId)?.branch || '--',
    environment: automationTasks.find((task) => task.id === execution.testTaskId)?.environment || '--',
    status: execution.status === 'PASSED' ? 'PASSED' : execution.status === 'FAILED' ? 'FAILED' : 'RUNNING',
    passRate: total > 0 ? `${Math.round((passed / total) * 1000) / 10}%` : '--',
    time: execution.startedAt ? new Date(execution.startedAt).toLocaleString('zh-CN') : '刚刚'
  }
}

async function loadAiConfig() {
  if (authState.role !== 'ADMIN') return
  try {
    const [models, prompts] = await Promise.all([
      api.ai.models(),
      api.ai.prompts()
    ])
    aiModels.splice(0, aiModels.length, ...models)
    promptTemplates.splice(0, promptTemplates.length, ...prompts)
  } catch (error) {
    showToast(error instanceof Error ? error.message : 'AI 配置加载失败')
  }
}

function applyUser(user: ApiUser) {
  localStorage.setItem('testpilot.userId', String(user.id))
  authState.displayName = user.displayName
  authState.role = user.role
}

function isCurrentUser(user: ApiUser) {
  return user.id === Number(localStorage.getItem('testpilot.userId'))
}

async function loadProjects() {
  const apiProjects = await api.projects.list()
  const nextProjects = apiProjects.map(fromApiProject)
  projects.splice(0, projects.length, ...nextProjects)
  const selected = nextProjects.find((project) => project.id === selectedProjectId.value)
    ?? nextProjects[0]
  selectedProjectId.value = selected?.id ?? null
  if (selected?.id) {
    localStorage.setItem('testpilot.projectId', String(selected.id))
  } else {
    localStorage.removeItem('testpilot.projectId')
  }
  syncProjectForm()
}

async function loadCurrentProjectData() {
  await Promise.all([
    loadRequirements(),
    loadTestCases(),
    loadAutomationData(),
    loadProjectMembers()
  ])
}

async function loadRequirements() {
  const projectId = currentProject.value?.id
  if (!projectId) return
  const apiRequirements = await api.requirements.list(projectId)
  requirements.splice(0, requirements.length, ...apiRequirements.map(fromApiRequirement))
}

async function loadTestCases() {
  const projectId = currentProject.value?.id
  if (!projectId) return
  const result = await api.testCases.list(projectId)
  testCases.splice(0, testCases.length, ...result.content.map(fromApiTestCase))
}

async function loadAutomationData() {
  const projectId = currentProject.value?.id
  if (!projectId) return
  const [projectsList, tasks, executionList] = await Promise.all([
    api.automation.projects(projectId),
    api.automation.tasks(projectId),
    api.automation.executions(projectId)
  ])
  automationProjects.splice(0, automationProjects.length, ...projectsList)
  automationTasks.splice(0, automationTasks.length, ...tasks)
  executionRecords.splice(0, executionRecords.length, ...executionList)
  executions.splice(0, executions.length, ...executionList.map(fromApiExecution))
  if (runForm.taskId === null && tasks.length > 0) {
    runForm.taskId = tasks[0].id
    runForm.environment = tasks[0].environment
    runForm.branch = tasks[0].branch
  }
}

async function loadProjectMembers() {
  const projectId = currentProject.value?.id
  if (!projectId) return
  const members = await api.projects.members(projectId)
  projectMembers.splice(0, projectMembers.length, ...members)
  const project = projects.find((item) => item.id === projectId)
  if (project) project.members = members.length
}

async function loadUsers() {
  if (authState.role !== 'ADMIN') return
  try {
    users.splice(0, users.length, ...(await api.users.list()))
  } catch (error) {
    showToast(error instanceof Error ? error.message : '用户列表加载失败')
  }
}

async function bootstrapAuth() {
  const token = localStorage.getItem('testpilot.accessToken')
  if (!token) {
    authState.ready = true
    return
  }
  try {
    const user = await api.auth.me()
    applyUser(user)
    await loadProjects()
    await loadRequirements()
    await loadTestCases()
    await loadAutomationData()
    await loadProjectMembers()
    await loadUsers()
    authState.authenticated = true
  } catch {
    localStorage.removeItem('testpilot.accessToken')
  } finally {
    authState.ready = true
  }
}

async function login() {
  authState.loading = true
  authState.error = ''
  try {
    const result = await api.auth.login(authState.username.trim(), authState.password)
    localStorage.setItem('testpilot.accessToken', result.accessToken)
    applyUser(result.user)
    await loadProjects()
    await loadRequirements()
    await loadTestCases()
    await loadAutomationData()
    await loadProjectMembers()
    await loadUsers()
    authState.authenticated = true
    activeView.value = 'home'
  } catch (error) {
    authState.error = error instanceof Error ? error.message : '登录失败，请稍后重试'
  } finally {
    authState.loading = false
    authState.ready = true
  }
}

async function logout() {
  try {
    await api.auth.logout()
  } catch {
    // Token cleanup still happens when the backend is unavailable.
  }
  localStorage.removeItem('testpilot.accessToken')
  localStorage.removeItem('testpilot.userId')
  authState.authenticated = false
  authState.error = ''
}

function navigate(view: View) {
  activeView.value = view
  if (view === 'project') activeProjectTab.value = 'overview'
  if (view === 'ai') void loadAiConfig()
}

function openProject(project?: Project) {
  const nextProject = project ?? currentProject.value
  if (nextProject.id) {
    selectedProjectId.value = nextProject.id
    localStorage.setItem('testpilot.projectId', String(nextProject.id))
  }
  navigate('project')
  syncProjectForm()
  void loadCurrentProjectData()
}

function openModal(name: ModalName) {
  modal.value = name
}

function openNewUser() {
  editingUserId.value = null
  Object.assign(userForm, {
    username: '',
    password: '',
    displayName: '',
    email: '',
    role: 'TEST_DEVELOPER'
  })
  openModal('user')
}

function openAddMember() {
  memberForm.userId = null
  memberForm.role = 'TEST_DEVELOPER'
  openModal('member')
}

function editUser(user: ApiUser) {
  editingUserId.value = user.id
  Object.assign(userForm, {
    username: user.username,
    password: '',
    displayName: user.displayName,
    email: user.email || '',
    role: user.role
  })
  openModal('user')
}

async function saveUser() {
  if (authState.role !== 'ADMIN' || !userForm.displayName.trim()) {
    showToast('请填写用户显示名称')
    return
  }
  try {
    if (editingUserId.value) {
      const saved = await api.users.update(editingUserId.value, {
        displayName: userForm.displayName,
        email: userForm.email,
        role: userForm.role
      })
      const index = users.findIndex((item) => item.id === saved.id)
      if (index >= 0) users[index] = saved
      if (userForm.password.trim()) {
        await api.users.resetPassword(saved.id, userForm.password)
      }
      showToast('用户信息已更新')
    } else {
      if (!userForm.username.trim() || !userForm.password.trim()) {
        showToast('新建用户需要填写用户名和初始密码')
        return
      }
      const saved = await api.users.create(userForm)
      users.unshift(saved)
      showToast('用户已创建')
    }
    closeModal()
  } catch (error) {
    showToast(error instanceof Error ? error.message : '用户保存失败')
  }
}

async function toggleUserStatus(user: ApiUser) {
  if (user.id === Number(localStorage.getItem('testpilot.userId'))) return
  try {
    const saved = await api.users.changeStatus(user.id, user.status === 'DISABLED' ? 'ENABLED' : 'DISABLED')
    const index = users.findIndex((item) => item.id === saved.id)
    if (index >= 0) users[index] = saved
    showToast(saved.status === 'DISABLED' ? '用户已禁用' : '用户已启用')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '用户状态更新失败')
  }
}

async function saveProjectSettings() {
  const projectId = currentProject.value?.id
  if (!projectId || !projectForm.name.trim()) {
    showToast('请填写项目名称')
    return
  }
  try {
    const saved = await api.projects.update(projectId, projectForm)
    const index = projects.findIndex((item) => item.id === saved.id)
    if (index >= 0) projects[index] = fromApiProject(saved)
    syncProjectForm()
    showToast('项目设置已保存')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '项目设置保存失败')
  }
}

async function deleteCurrentProject() {
  const projectId = currentProject.value?.id
  if (!projectId || !window.confirm(`确认删除项目“${currentProject.value.name}”吗？`)) return
  try {
    await api.projects.remove(projectId)
    await loadProjects()
    navigate('projects')
    showToast('项目已删除')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '项目删除失败')
  }
}

async function addProjectMember() {
  const projectId = currentProject.value?.id
  if (!projectId || !memberForm.userId) {
    showToast('请选择项目成员')
    return
  }
  try {
    const saved = await api.projects.addMember(projectId, {
      userId: memberForm.userId,
      role: memberForm.role
    })
    projectMembers.push(saved)
    memberForm.userId = null
    closeModal()
    showToast('项目成员已添加')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '项目成员添加失败')
  }
}

async function removeProjectMember(member: ApiProjectMember) {
  const projectId = currentProject.value?.id
  if (!projectId || !window.confirm(`确认移除成员“${member.displayName}”吗？`)) return
  try {
    await api.projects.removeMember(projectId, member.userId)
    const index = projectMembers.findIndex((item) => item.userId === member.userId)
    if (index >= 0) projectMembers.splice(index, 1)
    showToast('项目成员已移除')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '项目成员移除失败')
  }
}

async function updateProjectMemberRole(member: ApiProjectMember, event: Event) {
  const projectId = currentProject.value?.id
  const role = (event.target as HTMLSelectElement | null)?.value
  if (!projectId || !role || role === member.role) return
  try {
    const saved = await api.projects.updateMember(projectId, member.userId, { role })
    const index = projectMembers.findIndex((item) => item.userId === saved.userId)
    if (index >= 0) projectMembers[index] = saved
    showToast('成员角色已更新')
  } catch (error) {
    await loadProjectMembers()
    showToast(error instanceof Error ? error.message : '成员角色更新失败')
  }
}

function openNewAutomationProject() {
  editingAutomationProjectId.value = null
  Object.assign(automationProjectForm, {
    name: '',
    automationType: 'API',
    repositoryUrl: '',
    branch: 'main',
    credentialRef: '',
    framework: 'pytest',
    testPath: '',
    executeCommand: '',
    environment: 'TEST',
    jenkinsJob: ''
  })
  openModal('automationProject')
}

function editAutomationProject(project: ApiAutomationProject) {
  editingAutomationProjectId.value = project.id
  Object.assign(automationProjectForm, {
    name: project.name,
    automationType: project.automationType,
    repositoryUrl: project.repositoryUrl,
    branch: project.branch,
    credentialRef: project.credentialRef || '',
    framework: project.framework,
    testPath: project.testPath || '',
    executeCommand: project.executeCommand || '',
    environment: project.environment || 'TEST',
    jenkinsJob: project.jenkinsJob || ''
  })
  openModal('automationProject')
}

function openGenerationModal() {
  generationForm.requirementId = selectedRequirementId.value
  generationForm.modelId = aiModels[0]?.id ?? null
  openModal('case')
}

function closeModal() {
  modal.value = null
  if (modal.value !== 'requirementDetail') {
    requirementDetail.value = null
  }
}

async function openRequirementDetail(requirement: Requirement) {
  const projectId = currentProject.value?.id
  if (!projectId || !requirement.id) return
  try {
    const detail = await api.requirements.detail(projectId, requirement.id)
    requirementDetail.value = {
      name: detail.name,
      version: detail.version,
      fileType: detail.fileType,
      parseStatus: detail.parseStatus,
      parseError: detail.parseError,
      contentText: detail.contentText
    }
    openModal('requirementDetail')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '需求详情加载失败')
  }
}

async function parseRequirement(requirement: Requirement) {
  const projectId = currentProject.value?.id
  if (!projectId || !requirement.id) return
  try {
    await api.requirements.parse(projectId, requirement.id)
    await loadRequirements()
    showToast('需求解析完成')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '需求解析失败')
  }
}

function showToast(message: string) {
  toast.value = message
  window.setTimeout(() => {
    toast.value = ''
  }, 2600)
}

function openReference(url?: string) {
  if (!url) {
    showToast('当前执行还没有可打开的报告链接')
    return
  }
  window.open(url, '_blank', 'noopener,noreferrer')
}

async function saveModel() {
  if (authState.role !== 'ADMIN') {
    showToast('只有管理员可以保存 AI 模型')
    return
  }
  try {
    const saved = await api.ai.createModel(modelForm)
    aiModels.unshift(saved)
    closeModal()
    showToast('AI 模型配置已保存')
  } catch (error) {
    showToast(error instanceof Error ? error.message : 'AI 模型保存失败')
  }
}

async function savePrompt() {
  if (authState.role !== 'ADMIN') {
    showToast('只有管理员可以保存 Prompt')
    return
  }
  if (!promptForm.name.trim() || !promptForm.content.trim()) {
    showToast('请填写 Prompt 名称和内容')
    return
  }
  try {
    const saved = await api.ai.createPrompt(promptForm)
    promptTemplates.unshift(saved)
    closeModal()
    showToast('Prompt 模板已保存')
  } catch (error) {
    showToast(error instanceof Error ? error.message : 'Prompt 保存失败')
  }
}

async function createAutomationProject() {
  const projectId = currentProject.value?.id
  if (!projectId || !automationProjectForm.name.trim() || !automationProjectForm.repositoryUrl.trim()) {
    showToast('请填写自动化项目名称和 Git 仓库地址')
    return
  }
  try {
    const payload = { ...automationProjectForm }
    const saved = editingAutomationProjectId.value
      ? await api.automation.updateProject(projectId, editingAutomationProjectId.value, payload)
      : await api.automation.createProject(projectId, payload)
    const index = automationProjects.findIndex((item) => item.id === saved.id)
    if (index >= 0) {
      automationProjects[index] = saved
    } else {
      automationProjects.unshift(saved)
    }
    const wasEditing = Boolean(editingAutomationProjectId.value)
    editingAutomationProjectId.value = null
    Object.assign(automationProjectForm, {
      name: '',
      automationType: 'API',
      repositoryUrl: '',
      branch: 'main',
      credentialRef: '',
      framework: 'pytest',
      testPath: '',
      executeCommand: '',
      environment: 'TEST',
      jenkinsJob: ''
    })
    closeModal()
    showToast(wasEditing ? '自动化项目已更新' : '自动化项目已创建')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '自动化项目创建失败')
  }
}

async function createTask() {
  const projectId = currentProject.value?.id
  if (!projectId || !taskForm.name.trim() || !taskForm.automationProjectId) {
    showToast('请填写任务名称并选择自动化项目')
    return
  }
  try {
    const saved = await api.automation.createTask(projectId, {
      ...taskForm,
      automationProjectId: taskForm.automationProjectId as number
    })
    automationTasks.unshift(saved)
    runForm.taskId = saved.id
    closeModal()
    showToast('测试任务已创建')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '测试任务创建失败')
  }
}

async function createProject() {
  if (!newProject.name.trim() || !newProject.code.trim()) {
    showToast('请填写项目名称和项目标识')
    return
  }
  try {
    const created = await api.projects.create({
      name: newProject.name,
      code: newProject.code,
      description: newProject.description || '新建测试项目',
      defaultEnvironment: newProject.environment
    })
    projects.unshift(fromApiProject(created))
    selectedProjectId.value = created.id
    localStorage.setItem('testpilot.projectId', String(created.id))
    newProject.name = ''
    newProject.code = ''
    newProject.description = ''
    closeModal()
    showToast('项目创建成功')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '项目创建失败')
  }
}

function handleRequirementFile(event: Event) {
  const input = event.target as HTMLInputElement
  requirementFile.value = input.files?.[0] ?? null
}

async function uploadRequirement() {
  const projectId = currentProject.value?.id
  if (!projectId) {
    showToast('当前项目尚未加载')
    return
  }
  if (!requirementFile.value) {
    showToast('请选择需求文件')
    return
  }
  try {
    const uploaded = await api.requirements.upload(projectId, requirementFile.value, requirementVersion.value)
    requirements.unshift(fromApiRequirement(uploaded))
    requirementFile.value = null
    requirementVersion.value = 'V1.0'
    closeModal()
    activeProjectTab.value = 'requirements'
    showToast('需求已上传')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '需求上传失败')
  }
}

function openNewCase() {
  editingCaseId.value = null
  Object.assign(caseForm, {
    requirementId: selectedRequirementId.value,
    caseNo: '',
    title: '',
    moduleName: '',
    precondition: '',
    steps: '',
    testData: '',
    expectedResult: '',
    priority: 'P1',
    caseType: 'FUNCTIONAL',
    tags: ''
  })
  openModal('caseEdit')
}

function editCase(testCase: TestCase) {
  editingCaseId.value = testCase.id
  Object.assign(caseForm, {
    requirementId: testCase.requirementId ?? null,
    caseNo: testCase.no,
    title: testCase.title,
    moduleName: testCase.module,
    precondition: testCase.precondition,
    steps: testCase.steps,
    testData: testCase.testData,
    expectedResult: testCase.expectedResult,
    priority: testCase.priority,
    caseType: testCase.type === '功能' ? 'FUNCTIONAL' : testCase.type === '异常' ? 'EXCEPTION' : 'BOUNDARY',
    tags: testCase.tags
  })
  openModal('caseEdit')
}

async function saveCase() {
  const projectId = currentProject.value?.id
  if (!projectId || !caseForm.title.trim()) {
    showToast('请填写测试用例标题')
    return
  }
  try {
    if (editingCaseId.value) {
      await api.testCases.update(projectId, editingCaseId.value, caseForm)
    } else {
      await api.testCases.create(projectId, caseForm)
    }
    await loadTestCases()
    const wasEditing = Boolean(editingCaseId.value)
    closeModal()
    showToast(wasEditing ? '测试用例已更新' : '测试用例已创建')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '测试用例保存失败')
  }
}

async function changeCaseStatus(testCase: TestCase, status: TestCase['status']) {
  const projectId = currentProject.value?.id
  if (!projectId) return
  try {
    await api.testCases.changeStatus(projectId, testCase.id, status)
    await loadTestCases()
    showToast(`已将 ${testCase.no} 更新为${caseStatusLabel(status)}`)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '状态更新失败')
  }
}

async function deleteCase(testCase: TestCase) {
  const projectId = currentProject.value?.id
  if (!projectId || !window.confirm(`确认删除 ${testCase.no} 吗？`)) return
  try {
    await api.testCases.remove(projectId, testCase.id)
    await loadTestCases()
    showToast('测试用例已删除')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '测试用例删除失败')
  }
}

async function generateTestCases() {
  const projectId = currentProject.value?.id
  const requirementId = generationForm.requirementId ?? selectedRequirementId.value
  if (!projectId || !requirementId) {
    showToast('请先上传并解析一份需求文档')
    return
  }
  isGenerating.value = true
  try {
    const result = await api.ai.generate(projectId, requirementId, {
      modelId: generationForm.modelId ?? undefined,
      rules: generationForm.rules
    })
    generatedCaseList.splice(0, generatedCaseList.length, ...result.testCases)
    generatedCases.value = true
    await loadTestCases()
    closeModal()
    activeProjectTab.value = 'cases'
    showToast(`AI 已生成 ${result.testCases.length} 条测试用例 Draft`)
  } catch (error) {
    showToast(error instanceof Error ? error.message : 'AI 生成失败')
  } finally {
    isGenerating.value = false
  }
}

async function runTest() {
  const projectId = currentProject.value?.id
  const task = automationTasks.find((item) => item.id === runForm.taskId) || automationTasks[0]
  if (!projectId || !task) {
    showToast('当前项目还没有可执行的自动化任务')
    return
  }
  try {
    const execution = await api.automation.execute(projectId, task.id)
    await loadAutomationData()
    closeModal()
    showToast(`已触发 Jenkins Build ${execution.buildNumber || execution.id}`)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '执行任务失败')
  }
}

async function syncExecution(execution: ApiExecution) {
  const projectId = currentProject.value?.id
  if (!projectId) return
  try {
    await api.automation.sync(projectId, execution.id)
    await loadAutomationData()
    showToast('执行状态已同步')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '执行状态同步失败')
  }
}

async function retryExecution(execution: ApiExecution) {
  const projectId = currentProject.value?.id
  if (!projectId) return
  try {
    await api.automation.retry(projectId, execution.id)
    await loadAutomationData()
    showToast('已重新触发 Jenkins 执行')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '执行重试失败')
  }
}

function sendApiRequest() {
  responseVisible.value = true
  showToast('请求已发送，断言全部通过')
}

onMounted(bootstrapAuth)
</script>

<template>
  <div v-if="!authState.ready" class="app-loading">正在连接 TestPilot 服务...</div>

  <div v-else-if="!authState.authenticated" class="auth-screen">
    <div class="auth-panel">
      <div class="brand-mark auth-brand"><span class="brand-dot">T</span> TestPilot AI</div>
      <p class="auth-kicker">智能测试管理与自动化平台</p>
      <h1>登录工作台</h1>
      <p class="auth-copy">登录后管理项目、需求、测试用例和自动化执行。</p>
      <form class="auth-form" @submit.prevent="login">
        <div class="field"><label for="login-username">用户名</label><input id="login-username" v-model="authState.username" autocomplete="username" /></div>
        <div class="field"><label for="login-password">密码</label><input id="login-password" v-model="authState.password" type="password" autocomplete="current-password" /></div>
        <p v-if="authState.error" class="auth-error">{{ authState.error }}</p>
        <button class="btn primary auth-submit" :disabled="authState.loading">{{ authState.loading ? '登录中...' : '登录' }}</button>
      </form>
      <div class="auth-hint">本地演示账号：lin.xiao / demo123</div>
    </div>
  </div>

  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark"><span class="brand-dot">T</span> TestPilot AI</div>
        <small>智能测试管理与自动化平台</small>
      </div>
      <div class="side-label">工作台</div>
      <nav class="nav">
        <button v-for="item in navItems" :key="item.key" :class="{ active: activeView === item.key }" @click="navigate(item.key)">
          <span class="nav-icon">{{ item.icon }}</span>{{ item.label }}<span v-if="item.badge" class="nav-badge">{{ item.badge }}</span>
        </button>
      </nav>
      <div class="sidebar-footer">
        <div class="account">
          <div class="avatar">{{ authState.displayName.slice(0, 1) }}</div>
          <div><strong>{{ authState.displayName }}</strong><span>{{ authState.role === 'ADMIN' ? '管理员' : '测试开发' }}</span></div>
          <button class="account-more" title="退出登录" @click="logout">↪</button>
        </div>
      </div>
    </aside>

    <main class="main">
      <header class="topbar">
        <div class="crumb"><span>TestPilot</span><b>›</b><strong>{{ activeViewName }}</strong></div>
        <div class="top-actions">
          <label class="search"><span>⌕</span><input v-model="search" placeholder="搜索项目、用例或执行" /></label>
          <button class="icon-btn" title="通知">♢</button>
          <button class="icon-btn" title="快捷创建" @click="openModal('quick')">＋</button>
        </div>
      </header>

      <div class="content">
        <section v-if="activeView === 'home'" class="view">
          <div class="page-head">
            <div><h1>早上好，林晓</h1><p>今天是 2026 年 8 月 16 日，继续完成你的测试闭环。</p></div>
            <div class="head-actions"><button class="btn" @click="openModal('quick')">＋ 快速创建</button><button class="btn primary" @click="openModal('run')">▶ 发起执行</button></div>
          </div>
          <div class="stats-grid grid">
            <div class="card stat"><span class="stat-label">项目数量 <b class="stat-icon blue">▣</b></span><strong class="stat-value">3</strong><span class="stat-foot">参与中的项目</span></div>
            <div class="card stat"><span class="stat-label">测试用例 <b class="stat-icon purple">▤</b></span><strong class="stat-value">342</strong><span class="stat-foot trend-up">↑ 28 条本周新增</span></div>
            <div class="card stat"><span class="stat-label">接口自动化 <b class="stat-icon cyan">⌁</b></span><strong class="stat-value">5</strong><span class="stat-foot">已关联 Git 仓库</span></div>
            <div class="card stat"><span class="stat-label">UI 自动化 <b class="stat-icon orange">⌗</b></span><strong class="stat-value">2</strong><span class="stat-foot">Playwright 项目</span></div>
            <div class="card stat"><span class="stat-label">本周执行 <b class="stat-icon green">▶</b></span><strong class="stat-value">48</strong><span class="stat-foot trend-up">↑ 12.5% 较上周</span></div>
            <div class="card stat"><span class="stat-label">平均通过率 <b class="stat-icon red">%</b></span><strong class="stat-value">94.2%</strong><span class="stat-foot">最近 30 次执行</span></div>
          </div>
          <div class="section-grid grid">
            <div class="card">
              <div class="card-head"><h3>我的项目</h3><button class="link-btn" @click="navigate('projects')">查看全部项目 →</button></div>
              <div class="card-body table-wrap"><table><thead><tr><th>项目名称</th><th>状态</th><th>最近执行</th><th>通过率</th><th></th></tr></thead><tbody>
                <tr v-for="project in projects" :key="project.code"><td><strong>{{ project.name }}</strong><br><span class="muted">{{ project.code }}</span></td><td><span class="tag" :class="project.status === '运行中' ? 'green' : 'orange'">{{ project.status }}</span></td><td>{{ project.lastRun }}</td><td><div class="rate"><b>{{ project.passRate }}%</b><div class="progress"><i :style="{ width: `${project.passRate}%` }"></i></div></div></td><td><button class="btn" @click="openProject(project)">进入 →</button></td></tr>
              </tbody></table></div>
            </div>
            <div class="card">
              <div class="card-head"><h3>AI 测试分析</h3><button class="link-btn" @click="navigate('reports')">查看执行报告 →</button></div>
              <div class="card-body"><div class="ai-callout"><strong>✦ 持久化分析结果</strong><p>当前没有持久化的 AI 失败分析结果。同步 Jenkins 执行记录后，可在报告中心继续处理。</p><button class="btn" @click="navigate('reports')">进入报告中心</button></div><div class="mini-list"><div class="mini-item"><div class="mini-icon">AI</div><div><strong>需求转测试用例</strong><small>由项目 AI 工作流生成 Draft</small></div><span class="tag green">可执行</span></div><div class="mini-item"><div class="mini-icon">AI</div><div><strong>失败结果分析</strong><small>等待真实分析结果</small></div><span class="tag gray">待处理</span></div></div></div>
            </div>
          </div>
          <div class="section-grid grid">
            <div class="card"><div class="card-head"><h3>最近执行</h3><button class="link-btn" @click="navigate('reports')">查看执行中心 →</button></div><div class="card-body table-wrap"><table><thead><tr><th>项目</th><th>类型</th><th>执行时间</th><th>状态</th><th>通过率</th></tr></thead><tbody><tr v-for="execution in executions.slice(0, 3)" :key="execution.run"><td><strong>{{ execution.project }}</strong><br><span class="muted">{{ execution.task }}</span></td><td><span class="tag blue">{{ execution.type }}</span></td><td>{{ execution.time }}</td><td><span class="status" :class="execution.status.toLowerCase()">{{ execution.status }}</span></td><td>{{ execution.passRate }}</td></tr></tbody></table></div></div>
             <div class="card"><div class="card-head"><h3>Jenkins 状态</h3><button class="link-btn" @click="navigate('reports')">查看构建 →</button></div><div class="card-body"><div class="mini-list"><div v-for="execution in executionRecords.slice(0, 3)" :key="execution.id" class="mini-item"><div class="mini-icon" :class="execution.status === 'PASSED' ? 'success' : execution.status === 'FAILED' ? 'failure' : 'warning'">{{ execution.status === 'PASSED' ? '✓' : execution.status === 'FAILED' ? '!' : '…' }}</div><div><strong>{{ execution.jenkinsJob || 'Jenkins Job' }} {{ execution.buildNumber || '队列中' }}</strong><small>{{ execution.startedAt ? new Date(execution.startedAt).toLocaleString('zh-CN') : '尚未开始' }}</small></div><span class="tag" :class="execution.status === 'PASSED' ? 'green' : execution.status === 'FAILED' ? 'red' : 'orange'">{{ execution.status }}</span></div><div v-if="executionRecords.length === 0" class="muted">暂无 Jenkins 执行记录</div></div></div></div>
          </div>
        </section>

        <section v-else-if="activeView === 'projects'" class="view">
          <div class="page-head"><div><h1>项目</h1><p>以项目为中心统一管理需求、用例、自动化、执行与报告。</p></div><div class="head-actions"><button class="btn" @click="showToast('加入项目功能将在后端接入后开放')">＋ 加入项目</button><button class="btn primary" @click="openModal('project')">＋ 创建项目</button></div></div>
          <div class="card"><div class="card-body table-wrap"><table><thead><tr><th>项目名称</th><th>负责人</th><th>成员</th><th>环境</th><th>最近执行</th><th>通过率</th><th>状态</th><th></th></tr></thead><tbody><tr v-for="project in visibleProjects" :key="project.code"><td><div class="project-title"><div class="project-logo">{{ project.name.slice(0, 1) }}</div><div><strong>{{ project.name }}</strong><p>{{ project.code }} · {{ project.description }}</p></div></div></td><td>{{ project.owner }}</td><td>{{ project.members }} 人</td><td><span class="tag blue">{{ project.environment }}</span></td><td>{{ project.lastRun }}</td><td>{{ project.passRate ? `${project.passRate}%` : '--' }}</td><td><span class="tag" :class="project.status === '运行中' ? 'green' : 'orange'">{{ project.status }}</span></td><td><button class="btn" @click="openProject(project)">打开</button></td></tr></tbody></table></div></div>
        </section>

        <section v-else-if="activeView === 'project'" class="view">
          <div class="project-top"><div class="project-title"><div class="project-logo">星</div><div><h2>{{ currentProject.name }} <span class="role-chip">{{ currentProject.code }}</span></h2><p>负责人 {{ currentProject.owner }} · {{ currentProject.members }} 位项目成员 · 创建于 2026-05-12</p></div></div><div class="head-actions"><button class="btn" @click="activeProjectTab = 'settings'">⋯ 项目操作</button><button class="btn primary" @click="openModal('run')">▶ 发起执行</button></div></div>
          <div class="project-layout">
            <nav class="project-nav card">
              <button v-for="tab in projectTabs" :key="tab.key" :class="{ active: activeProjectTab === tab.key }" @click="activeProjectTab = tab.key"><span>{{ tab.icon }}</span>{{ tab.label }}</button>
            </nav>
            <div class="project-content">
              <template v-if="activeProjectTab === 'overview'">
                <div class="page-head compact"><div><h1>项目概览</h1><p>查看项目质量趋势、AI 分析和最近执行。</p></div><button class="btn" @click="openGenerationModal">✦ AI 生成用例</button></div>
                <div class="stats-grid grid"><div class="card stat"><span class="stat-label">需求文档</span><strong class="stat-value">3</strong><span class="stat-foot">2 份已解析</span></div><div class="card stat"><span class="stat-label">测试用例</span><strong class="stat-value">128</strong><span class="stat-foot trend-up">↑ 12 条待 Review</span></div><div class="card stat"><span class="stat-label">接口自动化</span><strong class="stat-value">2</strong><span class="stat-foot">pytest + requests</span></div><div class="card stat"><span class="stat-label">UI 自动化</span><strong class="stat-value">1</strong><span class="stat-foot">Playwright</span></div></div>
                <div class="section-grid grid"><div class="card"><div class="card-head"><h3>AI 工作流</h3><span class="tag green">运行正常</span></div><div class="card-body"><div class="flow compact-flow"><span class="done">1 创建项目</span><i>→</i><span class="done">2 上传 PRD</span><i>→</i><span class="done">3 生成用例</span><i>→</i><span class="current">4 Review</span><i>→</i><span>5 自动化执行</span></div><div class="ai-callout"><strong>建议动作</strong><p>当前有 {{ testCases.filter((item) => item.status === 'DRAFT').length }} 条 AI Draft 等待人工 Review，完成后可以关联接口自动化项目。</p><button class="btn" @click="activeProjectTab = 'cases'">去 Review 用例</button></div></div></div><div class="card"><div class="card-head"><h3>最近失败</h3><span class="muted">执行报告</span></div><div class="card-body"><div class="failure-box"><strong>当前没有持久化的 AI 分析结果</strong><p>先同步 Jenkins 执行记录，再到报告中心查看真实日志和报告引用。</p><button class="btn" @click="activeProjectTab = 'reports'">查看执行报告</button></div></div></div></div>
              </template>
              <template v-else-if="activeProjectTab === 'requirements'">
                <div class="page-head compact"><div><h1>需求管理</h1><p>上传 PRD，跟踪解析状态并沉淀需求到测试用例。</p></div><div class="head-actions"><button class="btn" @click="openModal('requirement')">⇧ 上传需求</button><button class="btn primary" @click="openGenerationModal">✦ AI 生成用例</button></div></div>
                 <div class="card"><div class="card-body table-wrap"><table><thead><tr><th>文档名称</th><th>版本</th><th>格式</th><th>解析状态</th><th>关联用例</th><th>更新时间</th><th></th></tr></thead><tbody><tr v-for="requirement in requirements" :key="requirement.id || requirement.name"><td><strong>{{ requirement.name }}</strong></td><td>{{ requirement.version }}</td><td><span class="tag blue">{{ requirement.type }}</span></td><td><span class="tag" :class="requirement.status === '已解析' ? 'green' : requirement.status === '解析失败' ? 'red' : 'orange'">{{ requirement.status }}</span></td><td>{{ requirement.cases || '--' }}</td><td>{{ requirement.updated }}</td><td><button class="btn" @click="requirement.status === '解析中' || requirement.status === '解析失败' ? parseRequirement(requirement) : openRequirementDetail(requirement)">{{ requirement.status === '解析中' || requirement.status === '解析失败' ? '解析' : '查看' }}</button></td></tr><tr v-if="requirements.length === 0"><td colspan="7" class="muted">暂无需求文档</td></tr></tbody></table></div></div>
              </template>
              <template v-else-if="activeProjectTab === 'cases'">
                <div class="page-head compact"><div><h1>测试用例</h1><p>维护 AI Draft、人工 Review 和正式测试用例。</p></div><div class="head-actions"><button class="btn" @click="openNewCase">＋ 新建用例</button><button class="btn primary" @click="openGenerationModal">✦ AI 生成用例</button></div></div>
                <div class="card"><div class="card-head"><div class="subtabs"><button :class="{ active: !caseStatusFilter }" @click="caseStatusFilter = ''">全部 <span class="count">{{ testCases.length }}</span></button><button :class="{ active: caseStatusFilter === 'REVIEW' }" @click="caseStatusFilter = 'REVIEW'">待 Review <span class="count">{{ testCases.filter((item) => item.status === 'REVIEW').length }}</span></button><button :class="{ active: caseStatusFilter === 'APPROVED' }" @click="caseStatusFilter = 'APPROVED'">已通过 <span class="count">{{ testCases.filter((item) => item.status === 'APPROVED').length }}</span></button></div><div class="head-actions"><select v-model="casePriorityFilter"><option value="">全部优先级</option><option>P0</option><option>P1</option><option>P2</option><option>P3</option></select><select v-model="caseTypeFilter"><option value="">全部类型</option><option value="功能">功能</option><option value="异常">异常</option><option value="边界">边界</option></select><button class="btn" @click="loadTestCases">刷新</button></div></div><div class="card-body table-wrap"><table><thead><tr><th>编号</th><th>标题</th><th>模块</th><th>优先级</th><th>类型</th><th>来源</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="testCase in filteredCases" :key="testCase.id"><td><strong>{{ testCase.no }}</strong></td><td>{{ testCase.title }}</td><td>{{ testCase.module }}</td><td><span class="tag" :class="testCase.priority === 'P0' ? 'red' : 'orange'">{{ testCase.priority }}</span></td><td>{{ testCase.type }}</td><td>{{ testCase.source === 'AI' ? 'AI' : '手工' }}</td><td><span class="tag" :class="testCase.status === 'APPROVED' ? 'green' : testCase.status === 'REVIEW' ? 'orange' : testCase.status === 'REJECTED' ? 'red' : 'gray'">{{ caseStatusLabel(testCase.status) }}</span></td><td><div class="head-actions"><button class="btn" @click="editCase(testCase)">编辑</button><button v-if="testCase.status === 'DRAFT' || testCase.status === 'REJECTED'" class="btn" @click="changeCaseStatus(testCase, 'REVIEW')">送 Review</button><button v-if="testCase.status === 'REVIEW'" class="btn primary" @click="changeCaseStatus(testCase, 'APPROVED')">通过</button><button v-if="testCase.status === 'REVIEW'" class="btn" @click="changeCaseStatus(testCase, 'REJECTED')">退回</button><button class="btn" @click="deleteCase(testCase)">删除</button></div></td></tr><tr v-if="filteredCases.length === 0"><td colspan="8" class="muted">暂无测试用例</td></tr></tbody></table></div></div>
              </template>
              <template v-else-if="activeProjectTab === 'api-workbench'">
                <div class="page-head compact"><div><h1>接口测试工作台</h1><p>轻量接口调试、请求集合与断言验证。</p></div><div class="head-actions"><span class="tag blue">TEST 环境</span><button class="btn" @click="showToast('工作台配置已保存')">工作台设置</button></div></div>
                <div class="card api-toolbar"><div class="api-toolbar-row"><select class="api-method"><option>POST</option><option>GET</option><option>PUT</option><option>DELETE</option></select><input value="&#123;&#123;base_url&#125;&#125;/orders" /><button class="btn primary" @click="sendApiRequest">发送</button><button class="btn" @click="showToast('请求已保存到订单回归集合')">保存请求</button></div></div>
                <div class="api-layout"><div class="card"><div class="card-head"><h3>POST /orders</h3><span class="tag green">可执行</span></div><div class="api-side-list"><button class="active"><span class="method post">POST</span>创建订单</button><button><span class="method get">GET</span>订单详情</button><button><span class="method put">PUT</span>修改订单</button><button><span class="method delete">DEL</span>取消订单</button></div></div><div class="card"><div class="card-head"><div class="subtabs"><button v-for="tab in ['params', 'headers', 'body', 'assertions']" :key="tab" :class="{ active: activeApiRequestTab === tab }" @click="activeApiRequestTab = tab as typeof activeApiRequestTab">{{ { params: '参数', headers: 'Headers', body: 'Body', assertions: '断言' }[tab] }}</button></div></div><div class="card-body"><div v-if="activeApiRequestTab === 'params'" class="param-grid"><div class="field"><label>userId</label><input value="&#123;&#123;user_id&#125;&#125;" /></div><div class="field"><label>skuId</label><input value="&#123;&#123;sku_id&#125;&#125;" /></div><div class="field"><label>couponCode</label><input value="NEW_USER_20" /></div></div><div v-else-if="activeApiRequestTab === 'headers'" class="param-grid"><div class="field"><label>Authorization</label><input value="Bearer &#123;&#123;token&#125;&#125;" /></div><div class="field"><label>Content-Type</label><input value="application/json" /></div></div><div v-else-if="activeApiRequestTab === 'body'" class="code-block">{ "userId": "&#123;&#123;user_id&#125;&#125;", "skuId": "&#123;&#123;sku_id&#125;&#125;", "couponCode": "NEW_USER_20" }</div><div v-else class="assertion-list"><div><span class="tag green">启用</span> Status Code = 200</div><div><span class="tag green">启用</span> $.orderStatus = CREATED</div><div><span class="tag green">启用</span> $.orderId 类型为 String</div></div></div></div></div>
                <div v-if="responseVisible" class="card response-card"><div class="card-head"><h3>响应结果</h3><span class="status passed">200 OK</span></div><div class="card-body"><div class="response-meta"><span>耗时 246 ms</span><span>响应体 1.2 KB</span><span>Run #105 · TEST</span></div><pre>{ "code": 0, "message": "success", "data": { "orderId": "ORD-20260816-0042", "orderStatus": "CREATED" } }</pre></div></div>
                <div class="card api-bottom"><div class="card-body"><div class="subtabs"><button :class="{ active: activeApiTab === 'collections' }" @click="activeApiTab = 'collections'">集合执行</button><button :class="{ active: activeApiTab === 'variables' }" @click="activeApiTab = 'variables'">环境变量</button><button :class="{ active: activeApiTab === 'history' }" @click="activeApiTab = 'history'">执行历史</button></div><div v-if="activeApiTab === 'collections'" class="collection-grid"><div v-for="item in ['订单回归集合', '用户鉴权集合', '支付异常集合']" :key="item" class="collection-item"><span class="mini-icon">O</span><div><strong>{{ item }}</strong><small>12 个接口 · {{ item === '支付异常集合' ? '待补参数' : '可执行' }}</small></div><span class="tag" :class="item === '支付异常集合' ? 'orange' : 'green'">{{ item === '支付异常集合' ? '待补参数' : '可执行' }}</span></div></div><div v-else-if="activeApiTab === 'variables'" class="table-wrap"><table><thead><tr><th>变量名</th><th>TEST</th><th>来源</th><th>敏感</th></tr></thead><tbody><tr><td><strong>&#123;&#123;base_url&#125;&#125;</strong></td><td>https://test-api.xh-mall.cn</td><td>环境配置</td><td>否</td></tr><tr><td><strong>&#123;&#123;token&#125;&#125;</strong></td><td>••••••••</td><td>登录脚本提取</td><td><span class="tag red">是</span></td></tr></tbody></table></div><div v-else class="table-wrap"><table><thead><tr><th>执行时间</th><th>集合</th><th>环境</th><th>Passed</th><th>Failed</th><th>状态</th></tr></thead><tbody><tr><td>今天 10:24</td><td>订单回归集合</td><td>TEST</td><td>11</td><td class="danger-text">1</td><td><span class="status failed">FAILED</span></td></tr><tr><td>今天 08:42</td><td>用户鉴权集合</td><td>TEST</td><td>8</td><td>0</td><td><span class="status passed">PASSED</span></td></tr></tbody></table></div></div></div>
              </template>
              <template v-else-if="activeProjectTab === 'api-code'">
                <div class="page-head compact"><div><h1>接口自动化代码</h1><p>管理 Git 仓库配置，平台通过 Jenkins 执行真实测试项目。</p></div><div class="head-actions"><button class="btn" @click="openNewAutomationProject">＋ 新建自动化项目</button><button class="btn primary" @click="openModal('run')">▶ 立即执行</button></div></div>
                <div v-for="project in automationProjects.filter((item) => item.automationType === 'API')" :key="project.id" class="card">
                  <div class="card-head"><div><h3>{{ project.name }}</h3><span class="muted">{{ project.framework }} · {{ project.status }}</span></div><button class="btn" @click="editAutomationProject(project)">编辑配置</button></div>
                  <div class="card-body config-grid"><div class="field"><label>Git Repository</label><input :value="project.repositoryUrl" disabled /></div><div class="field"><label>Branch</label><input :value="project.branch" disabled /></div><div class="field"><label>Framework</label><input :value="project.framework" disabled /></div><div class="field"><label>Test Path</label><input :value="project.testPath || '--'" disabled /></div><div class="field full"><label>Execute Command</label><input :value="project.executeCommand || '--'" disabled /></div><div class="field"><label>Git Credentials</label><input :value="project.credentialRef || '未配置'" disabled /></div><div class="field"><label>Jenkins Job</label><input :value="project.jenkinsJob || '未配置'" disabled /></div></div>
                </div>
                <div v-if="automationProjects.filter((item) => item.automationType === 'API').length === 0" class="card"><div class="card-body muted">暂无接口自动化项目，请先创建配置。</div></div>
              </template>
              <template v-else-if="activeProjectTab === 'ui'">
                <div class="page-head compact"><div><h1>UI 自动化</h1><p>管理 Playwright 项目、执行环境和失败资产。</p></div><div class="head-actions"><button class="btn" @click="openNewAutomationProject">＋ 新建 UI 项目</button><button class="btn primary" @click="openModal('run')">▶ 立即执行</button></div></div>
                <div v-for="project in automationProjects.filter((item) => item.automationType === 'UI')" :key="project.id" class="card">
                  <div class="card-head"><div><h3>{{ project.name }}</h3><span class="muted">{{ project.framework }} · {{ project.status }}</span></div><button class="btn" @click="editAutomationProject(project)">编辑配置</button></div>
                  <div class="card-body config-grid"><div class="field"><label>Git Repository</label><input :value="project.repositoryUrl" disabled /></div><div class="field"><label>Branch</label><input :value="project.branch" disabled /></div><div class="field"><label>Framework</label><input :value="project.framework" disabled /></div><div class="field"><label>运行环境</label><input :value="project.environment || '未配置'" disabled /></div><div class="field full"><label>Test Command</label><input :value="project.executeCommand || '--'" disabled /></div><div class="field"><label>Git Credentials</label><input :value="project.credentialRef || '未配置'" disabled /></div><div class="field"><label>Jenkins Job</label><input :value="project.jenkinsJob || '未配置'" disabled /></div></div>
                </div>
                <div v-if="automationProjects.filter((item) => item.automationType === 'UI').length === 0" class="card"><div class="card-body muted">暂无 UI 自动化项目，请先创建配置。</div></div>
                <div class="section-grid grid"><div class="card"><div class="card-head"><h3>失败资产</h3><span class="muted">来自最近一次真实执行</span></div><div class="card-body"><div v-if="executionRecords[0]?.consoleLogRef" class="mini-item"><div class="mini-icon">≡</div><div><strong>Jenkins 日志</strong><small>{{ executionRecords[0].consoleLogRef }}</small></div><button class="btn" @click="openReference(executionRecords[0].consoleLogRef)">查看</button></div><div v-else class="muted">同步执行记录后显示日志引用。</div></div></div><div class="card"><div class="card-head"><h3>执行说明</h3></div><div class="card-body"><div class="ai-callout"><strong>Git 是自动化代码唯一事实来源</strong><p>平台保存仓库引用、分支、执行命令和 Jenkins Job，不直接保存测试代码。</p></div></div></div></div>
              </template>
              <template v-else-if="activeProjectTab === 'tasks'">
                <div class="page-head compact"><div><h1>测试任务</h1><p>统一关联环境、Branch、Jenkins Job 和执行参数。</p></div><div class="head-actions"><button class="btn" @click="openModal('task')">＋ 新建测试任务</button><button class="btn primary" @click="openModal('run')">▶ 立即执行</button></div></div>
                <div class="card"><div class="card-body table-wrap"><table><thead><tr><th>测试任务</th><th>类型</th><th>自动化项目</th><th>环境</th><th>Branch</th><th>Jenkins Job</th><th>状态</th><th></th></tr></thead><tbody><tr v-for="task in automationTasks" :key="task.id"><td><strong>{{ task.name }}</strong></td><td><span class="tag blue">{{ task.taskType }}</span></td><td>{{ automationProjects.find((item) => item.id === task.automationProjectId)?.name || '--' }}</td><td>{{ task.environment }}</td><td>{{ task.branch }}</td><td>{{ task.jenkinsJob }}</td><td><span class="tag green">{{ task.status }}</span></td><td><button class="btn" @click="runForm.taskId = task.id; openModal('run')">执行</button></td></tr><tr v-if="automationTasks.length === 0"><td colspan="8" class="muted">暂无测试任务</td></tr></tbody></table></div></div>
              </template>
              <template v-else-if="activeProjectTab === 'reports'">
                <div class="page-head compact"><div><h1>项目测试报告</h1><p>查看 Allure 详情、历史执行、失败日志与 AI 分析。</p></div><button class="btn primary" @click="openReference(executionRecords[0]?.report?.allureUrl)">打开 Allure</button></div>
                <div class="report-kpis"><div class="card report-kpi"><span>Total</span><strong>{{ reportSummary.total }}</strong></div><div class="card report-kpi"><span>Passed</span><strong class="trend-up">{{ reportSummary.passed }}</strong></div><div class="card report-kpi"><span>Failed</span><strong class="danger-text">{{ reportSummary.failed }}</strong></div><div class="card report-kpi"><span>Broken</span><strong class="warning-text">{{ reportSummary.broken }}</strong></div><div class="card report-kpi"><span>Skipped</span><strong>{{ reportSummary.skipped }}</strong></div></div>
                <div class="card report-callout"><div class="card-body"><div class="ai-callout"><strong>AI 失败分析</strong><p class="muted">当前没有持久化的 AI 分析结果。请先同步执行记录，再进入 AI 分析流程。</p></div></div></div>
                <div class="card"><div class="card-head"><h3>历史报告</h3><span class="muted">每次执行独立沉淀</span></div><div class="card-body table-wrap"><table><thead><tr><th>Execution</th><th>Build</th><th>结果</th><th>Duration</th><th>Allure</th><th></th></tr></thead><tbody><tr v-for="execution in executionRecords" :key="execution.id"><td>#{{ execution.id }}</td><td>{{ execution.buildNumber || '队列中' }}</td><td><span class="tag" :class="execution.status === 'PASSED' ? 'green' : execution.status === 'FAILED' ? 'red' : 'orange'">{{ execution.status }}</span></td><td>{{ execution.durationMs ? `${Math.round(execution.durationMs / 1000)}s` : '--' }}</td><td>{{ execution.report?.allureUrl || '--' }}</td><td><div class="head-actions"><button class="btn" @click="syncExecution(execution)">同步</button><button v-if="execution.status === 'FAILED' || execution.status === 'ABORTED'" class="btn" @click="retryExecution(execution)">重试</button></div></td></tr><tr v-if="executionRecords.length === 0"><td colspan="6" class="muted">暂无执行记录</td></tr></tbody></table></div></div>
              </template>
              <template v-else>
                <div class="page-head compact"><div><h1>项目设置</h1><p>管理项目成员、权限、代码仓库和测试用例字段规范。</p></div><div v-if="canManageCurrentProject" class="head-actions"><button class="btn" @click="deleteCurrentProject">删除项目</button><button class="btn primary" @click="saveProjectSettings">保存设置</button></div></div>
                <div class="section-grid grid"><div class="card"><div class="card-head"><h3>基本信息</h3></div><div class="card-body"><div class="field"><label>项目名称</label><input v-model="projectForm.name" :disabled="!canManageCurrentProject" /></div><div class="field"><label>项目标识</label><input :value="currentProject.code" disabled /></div><div class="two-col"><div class="field"><label>默认环境</label><select v-model="projectForm.defaultEnvironment" :disabled="!canManageCurrentProject"><option>DEV</option><option>TEST</option><option>STAGING</option></select></div><div class="field"><label>项目状态</label><select v-model="projectForm.status" :disabled="!canManageCurrentProject"><option value="RUNNING">运行中</option><option value="TESTING">测试中</option></select></div></div><div class="field"><label>项目描述</label><textarea v-model="projectForm.description" :disabled="!canManageCurrentProject"></textarea></div></div></div><div class="card"><div class="card-head"><h3>项目成员</h3><button v-if="canManageCurrentProject" class="btn" @click="openAddMember">＋ 添加成员</button></div><div class="card-body table-wrap"><table><thead><tr><th>成员</th><th>邮箱</th><th>角色</th><th></th></tr></thead><tbody><tr v-for="member in projectMembers" :key="member.userId"><td><strong>{{ member.displayName }}</strong><br><span class="muted">{{ member.username }}</span></td><td>{{ member.email || '--' }}</td><td><select v-if="member.role !== 'OWNER' && canManageCurrentProject" :value="member.role" @change="updateProjectMemberRole(member, $event)" class="role-select"><option value="MANAGER">项目管理员</option><option value="TEST_DEVELOPER">测试开发</option><option value="VIEWER">只读成员</option></select><span v-else class="role-chip">{{ member.role }}</span></td><td><button v-if="member.role !== 'OWNER' && canManageCurrentProject" class="btn" @click="removeProjectMember(member)">移除</button></td></tr><tr v-if="projectMembers.length === 0"><td colspan="4" class="muted">暂无成员</td></tr></tbody></table></div></div></div>
              </template>
            </div>
          </div>
        </section>

        <section v-else-if="activeView === 'ai'" class="view">
          <div class="page-head"><div><h1>AI 能力</h1><p>管理模型与 Prompt 模板，AI 服务于用例生成和失败分析。</p></div><div class="head-actions"><button class="btn" @click="navigate('project')">进入项目 AI 工作流 →</button><button v-if="authState.role === 'ADMIN' && activeAiTab === 'prompts'" class="btn" @click="openModal('prompt')">＋ 新建 Prompt</button><button v-if="authState.role === 'ADMIN' && activeAiTab === 'models'" class="btn primary" @click="openModal('model')">＋ 管理模型</button></div></div>
          <div v-if="authState.role !== 'ADMIN'" class="card"><div class="card-body"><p class="muted">当前账号没有 AI 配置管理权限，测试用例生成仍可使用项目默认模型。</p></div></div>
          <div v-else class="card"><div class="card-body"><div class="subtabs"><button :class="{ active: activeAiTab === 'models' }" @click="activeAiTab = 'models'">模型管理</button><button :class="{ active: activeAiTab === 'prompts' }" @click="activeAiTab = 'prompts'">Prompt 模板</button><button :class="{ active: activeAiTab === 'logs' }" @click="activeAiTab = 'logs'">调用记录</button></div><div class="table-wrap"><table v-if="activeAiTab === 'models'"><thead><tr><th>模型</th><th>Provider</th><th>Endpoint</th><th>API Key</th><th>状态</th><th></th></tr></thead><tbody><tr v-for="model in aiModels" :key="model.id"><td><strong>{{ model.name }}</strong></td><td>{{ model.provider }}</td><td>{{ model.endpoint || '本地演示' }}</td><td>{{ model.hasApiKey ? '已配置' : '未配置' }}</td><td><span class="tag" :class="model.enabled ? 'green' : 'gray'">{{ model.enabled ? '启用' : '停用' }}</span></td><td><button class="btn" @click="modelForm.name = model.name; modelForm.provider = model.provider; modelForm.endpoint = model.endpoint; openModal('model')">编辑</button></td></tr><tr v-if="aiModels.length === 0"><td colspan="6" class="muted">暂无 AI 模型</td></tr></tbody></table><table v-else-if="activeAiTab === 'prompts'"><thead><tr><th>Prompt 名称</th><th>任务类型</th><th>版本</th><th>状态</th></tr></thead><tbody><tr v-for="prompt in promptTemplates" :key="prompt.id"><td><strong>{{ prompt.name }}</strong></td><td>{{ prompt.taskType }}</td><td>{{ prompt.version }}</td><td><span class="tag" :class="prompt.enabled ? 'green' : 'gray'">{{ prompt.enabled ? '启用' : '停用' }}</span></td></tr><tr v-if="promptTemplates.length === 0"><td colspan="4" class="muted">暂无 Prompt 模板</td></tr></tbody></table><div v-else class="card-body"><p class="muted">调用记录接口尚未实现，当前不展示静态数据。</p></div></div></div></div>
        </section>

        <section v-else-if="activeView === 'reports'" class="view">
          <div class="page-head"><div><h1>测试报告中心</h1><p>跨项目查看 Jenkins 构建、Allure 报告、日志、截图、视频与 AI 分析。</p></div><button class="btn primary" @click="openModal('run')">▶ 发起执行</button></div>
          <div class="report-kpis"><div class="card report-kpi"><span>Total</span><strong>{{ reportSummary.total }}</strong></div><div class="card report-kpi"><span>Passed</span><strong class="trend-up">{{ reportSummary.passed }}</strong></div><div class="card report-kpi"><span>Failed</span><strong class="danger-text">{{ reportSummary.failed }}</strong></div><div class="card report-kpi"><span>Broken</span><strong class="warning-text">{{ reportSummary.broken }}</strong></div><div class="card report-kpi"><span>Skipped</span><strong>{{ reportSummary.skipped }}</strong></div></div>
          <div class="card"><div class="card-body table-wrap"><table><thead><tr><th>Execution</th><th>Jenkins Job</th><th>Build</th><th>Commit</th><th>状态</th><th>报告</th><th></th></tr></thead><tbody><tr v-for="execution in executionRecords" :key="execution.id"><td><strong>#{{ execution.id }}</strong><br><span class="muted">{{ execution.startedAt ? new Date(execution.startedAt).toLocaleString('zh-CN') : '--' }}</span></td><td>{{ execution.jenkinsJob || '--' }}</td><td>{{ execution.buildNumber || '队列中' }}</td><td>{{ execution.commitSha || '--' }}</td><td><span class="status" :class="execution.status.toLowerCase()">{{ execution.status }}</span></td><td>{{ execution.report?.allureUrl || '--' }}</td><td><div class="head-actions"><button class="btn" @click="syncExecution(execution)">同步</button><button v-if="execution.status === 'FAILED' || execution.status === 'ABORTED'" class="btn" @click="retryExecution(execution)">重试</button></div></td></tr><tr v-if="executionRecords.length === 0"><td colspan="7" class="muted">暂无执行记录</td></tr></tbody></table></div></div>
          <div class="section-grid grid report-bottom"><div class="card"><div class="card-head"><h3>AI 失败分析</h3><span class="muted">真实分析结果</span></div><div class="card-body"><p class="muted">当前还没有持久化的 AI 失败分析结果。执行日志和报告同步完成后再生成分析。</p></div></div></div>
        </section>

        <section v-else class="view">
          <div class="page-head"><div><h1>系统管理</h1><p>管理员配置用户、AI 模型、Prompt 模板、Git Credentials 与 Jenkins。</p></div><button v-if="authState.role === 'ADMIN'" class="btn primary" @click="openNewUser">＋ 添加用户</button></div>
          <div v-if="authState.role !== 'ADMIN'" class="card"><div class="card-body"><p class="muted">当前账号没有系统管理权限。</p></div></div>
          <div v-else class="section-grid grid"><div class="card"><div class="card-head"><h3>用户与角色</h3><span class="muted">{{ users.length }} 个账号</span></div><div class="card-body table-wrap"><table><thead><tr><th>用户</th><th>邮箱</th><th>角色</th><th>状态</th><th>最近登录</th><th></th></tr></thead><tbody><tr v-for="user in users" :key="user.id"><td><strong>{{ user.displayName }}</strong><br><span class="muted">{{ user.username }}</span></td><td>{{ user.email || '--' }}</td><td><span class="role-chip">{{ user.role }}</span></td><td><span class="tag" :class="user.status === 'DISABLED' ? 'gray' : 'green'">{{ user.status === 'DISABLED' ? '已禁用' : '启用' }}</span></td><td>{{ user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString('zh-CN') : '--' }}</td><td><div class="head-actions"><button class="btn" @click="editUser(user)">编辑</button><button class="btn" :disabled="isCurrentUser(user)" :title="isCurrentUser(user) ? '不能禁用当前登录用户' : ''" @click="toggleUserStatus(user)">{{ user.status === 'DISABLED' ? '启用' : '禁用' }}</button></div></td></tr><tr v-if="users.length === 0"><td colspan="6" class="muted">暂无用户</td></tr></tbody></table></div></div><div class="card"><div class="card-head"><h3>集成与安全</h3><span class="muted">敏感配置由部署环境管理</span></div><div class="card-body"><div class="ai-callout"><strong>当前运行配置</strong><p>Jenkins、Git Credentials 和 AI API Key 通过后端环境变量注入，前端不保存敏感明文。</p></div></div></div></div>
        </section>
      </div>
    </main>

    <div v-if="toast" class="toast">{{ toast }}</div>

    <div v-if="modal" class="modal-backdrop" @click.self="closeModal">
      <div class="modal">
        <div class="modal-head"><h3>{{ modal === 'quick' ? '快速创建' : modal === 'project' ? '创建项目' : modal === 'requirement' ? '上传需求文档' : modal === 'requirementDetail' ? '需求详情' : modal === 'run' ? '执行测试任务' : modal === 'case' ? 'AI 测试用例生成' : modal === 'caseEdit' ? (editingCaseId ? '编辑测试用例' : '新建测试用例') : modal === 'model' ? '管理 AI 模型' : modal === 'prompt' ? '新建 Prompt 模板' : modal === 'automationProject' ? (editingAutomationProjectId ? '编辑自动化项目' : '新建自动化项目') : modal === 'task' ? '新建测试任务' : modal === 'user' ? (editingUserId ? '编辑用户' : '新建用户') : modal === 'member' ? '添加项目成员' : 'AI 失败分析' }}</h3><button class="icon-btn" @click="closeModal">×</button></div>
        <form v-if="modal === 'project'" class="modal-body" @submit.prevent="createProject"><div class="two-col"><div class="field"><label>项目名称 *</label><input v-model="newProject.name" placeholder="例如：星河电商平台" /></div><div class="field"><label>项目标识 *</label><input v-model="newProject.code" placeholder="例如：XH-MALL" /></div></div><div class="field"><label>项目描述</label><textarea v-model="newProject.description" placeholder="描述项目范围、业务目标与测试重点"></textarea></div><div class="field"><label>默认环境</label><select v-model="newProject.environment"><option>TEST</option><option>DEV</option><option>STAGING</option></select></div><div class="modal-foot"><button type="button" class="btn" @click="closeModal">取消</button><button class="btn primary">创建项目</button></div></form>
        <div v-else-if="modal === 'quick'" class="modal-body quick-grid"><button class="btn" @click="openModal('project')">▣ 创建项目</button><button class="btn" @click="openModal('requirement')">◫ 上传需求</button><button class="btn" @click="openGenerationModal">▤ 生成用例</button><button class="btn" @click="openModal('run')">▶ 发起执行</button></div>
        <form v-else-if="modal === 'requirement'" class="modal-body" @submit.prevent="uploadRequirement"><div class="field"><label>上传到当前项目</label><input :value="currentProject.name" disabled /></div><div class="dropzone"><div class="upload-icon">⇧</div><strong>{{ requirementFile?.name || '选择需求文件' }}</strong><p>支持 Markdown、PDF、DOCX、TXT · 单文件不超过 10MB</p><label class="btn file-picker">选择文件<input type="file" accept=".md,.markdown,.pdf,.docx,.txt" @change="handleRequirementFile" /></label></div><div class="field"><label>文档版本说明</label><input v-model="requirementVersion" placeholder="例如：V1.2" /></div><div class="modal-foot"><button type="button" class="btn" @click="closeModal">取消</button><button class="btn primary">上传并解析</button></div></form>
        <div v-else-if="modal === 'requirementDetail'" class="modal-body"><div class="detail-grid"><div><span class="muted">文档名称</span><strong>{{ requirementDetail?.name }}</strong></div><div><span class="muted">版本</span><strong>{{ requirementDetail?.version }}</strong></div><div><span class="muted">格式</span><strong>{{ requirementDetail?.fileType }}</strong></div><div><span class="muted">解析状态</span><strong>{{ requirementDetail?.parseStatus }}</strong></div></div><div v-if="requirementDetail?.parseError" class="auth-error">{{ requirementDetail.parseError }}</div><pre class="requirement-content">{{ requirementDetail?.contentText || '暂无正文内容' }}</pre><div class="modal-foot"><button class="btn" @click="closeModal">关闭</button></div></div>
        <form v-else-if="modal === 'caseEdit'" class="modal-body" @submit.prevent="saveCase"><div class="two-col"><div class="field"><label>用例编号</label><input v-model="caseForm.caseNo" placeholder="留空自动生成" /></div><div class="field"><label>优先级</label><select v-model="caseForm.priority"><option>P0</option><option>P1</option><option>P2</option><option>P3</option></select></div></div><div class="field"><label>用例标题 *</label><input v-model="caseForm.title" placeholder="例如：创建订单成功" /></div><div class="two-col"><div class="field"><label>模块</label><input v-model="caseForm.moduleName" placeholder="例如：订单中心" /></div><div class="field"><label>测试类型</label><select v-model="caseForm.caseType"><option value="FUNCTIONAL">功能</option><option value="EXCEPTION">异常</option><option value="BOUNDARY">边界</option></select></div></div><div class="field"><label>前置条件</label><textarea v-model="caseForm.precondition"></textarea></div><div class="field"><label>测试步骤</label><textarea v-model="caseForm.steps"></textarea></div><div class="field"><label>测试数据</label><textarea v-model="caseForm.testData"></textarea></div><div class="field"><label>预期结果</label><textarea v-model="caseForm.expectedResult"></textarea></div><div class="field"><label>标签</label><input v-model="caseForm.tags" placeholder="多个标签用逗号分隔" /></div><div class="modal-foot"><button type="button" class="btn" @click="closeModal">取消</button><button class="btn primary">{{ editingCaseId ? '保存修改' : '创建用例' }}</button></div></form>
        <form v-else-if="modal === 'run'" class="modal-body" @submit.prevent="runTest"><div class="field"><label>测试任务</label><select v-model.number="runForm.taskId"><option :value="null" disabled>请选择测试任务</option><option v-for="task in automationTasks" :key="task.id" :value="task.id">{{ task.name }} · {{ task.jenkinsJob }}</option></select></div><div class="two-col"><div class="field"><label>执行环境</label><input v-model="runForm.environment" /></div><div class="field"><label>Branch</label><input v-model="runForm.branch" /></div></div><div class="ai-callout"><strong>执行链路</strong><p>平台将通过 Jenkins 触发构建，完成后回收 Build、Commit、日志和 Allure 报告。</p></div><div class="modal-foot"><button type="button" class="btn" @click="closeModal">取消</button><button class="btn primary">立即执行</button></div></form>
        <form v-else-if="modal === 'case'" class="modal-body" @submit.prevent="generateTestCases"><div class="flow compact-flow"><span class="done">1 上传 PRD</span><i>→</i><span class="done">2 解析文档</span><i>→</i><span class="current">3 生成 Draft</span><i>→</i><span>4 Review</span></div><div class="two-col"><div class="field"><label>需求文档</label><select v-model.number="generationForm.requirementId"><option v-for="requirement in requirements.filter((item) => item.id && item.status === '已解析')" :key="requirement.id" :value="requirement.id">{{ requirement.name }} {{ requirement.version }}</option></select></div><div class="field"><label>AI 模型</label><select v-model.number="generationForm.modelId"><option v-for="model in aiModels" :key="model.id" :value="model.id">{{ model.name }} · {{ model.provider }}</option><option v-if="aiModels.length === 0" :value="null">使用项目默认模型</option></select></div></div><div class="field"><label>测试规则</label><input v-model="generationForm.rules" /></div><div class="draft-preview"><div class="card-head"><h3>生成结果 Draft</h3><span class="tag orange">{{ generatedCases ? `${generatedCaseList.length} 条待 Review` : '等待生成' }}</span></div><div class="card-body table-wrap"><table><thead><tr><th>编号</th><th>标题</th><th>状态</th></tr></thead><tbody><tr v-for="item in generatedCaseList" :key="item.id"><td>{{ item.caseNo }}</td><td>{{ item.title }}</td><td><span class="tag orange">{{ item.status }}</span></td></tr><tr v-if="generatedCaseList.length === 0"><td colspan="3" class="muted">生成后显示真实 Draft 结果</td></tr></tbody></table></div></div><div class="modal-foot"><button type="button" class="btn" @click="closeModal">取消</button><button class="btn primary" :disabled="isGenerating">{{ isGenerating ? '生成中...' : '生成并进入 Review' }}</button></div></form>
        <form v-else-if="modal === 'model'" class="modal-body" @submit.prevent="saveModel"><div class="two-col"><div class="field"><label>Provider</label><input v-model="modelForm.provider" placeholder="MOCK / OPENAI" /></div><div class="field"><label>模型名称</label><input v-model="modelForm.name" /></div></div><div class="field"><label>Endpoint</label><input v-model="modelForm.endpoint" placeholder="https://api.example.com/v1" /></div><div class="field"><label>API Key</label><input v-model="modelForm.apiKey" type="password" placeholder="可选" /></div><div class="modal-foot"><button type="button" class="btn" @click="closeModal">取消</button><button class="btn primary">保存模型</button></div></form>
        <form v-else-if="modal === 'prompt'" class="modal-body" @submit.prevent="savePrompt"><div class="two-col"><div class="field"><label>Prompt 名称 *</label><input v-model="promptForm.name" /></div><div class="field"><label>任务类型</label><input v-model="promptForm.taskType" /></div></div><div class="field"><label>版本</label><input v-model="promptForm.version" /></div><div class="field"><label>Prompt 内容 *</label><textarea v-model="promptForm.content" rows="8"></textarea></div><div class="modal-foot"><button type="button" class="btn" @click="closeModal">取消</button><button class="btn primary">保存 Prompt</button></div></form>
        <form v-else-if="modal === 'automationProject'" class="modal-body" @submit.prevent="createAutomationProject"><div class="two-col"><div class="field"><label>名称 *</label><input v-model="automationProjectForm.name" /></div><div class="field"><label>类型</label><select v-model="automationProjectForm.automationType"><option>API</option><option>UI</option></select></div></div><div class="field"><label>Git 仓库地址 *</label><input v-model="automationProjectForm.repositoryUrl" /></div><div class="two-col"><div class="field"><label>Branch</label><input v-model="automationProjectForm.branch" /></div><div class="field"><label>Framework</label><input v-model="automationProjectForm.framework" /></div></div><div class="two-col"><div class="field"><label>Test Path</label><input v-model="automationProjectForm.testPath" /></div><div class="field"><label>Git Credentials 引用</label><input v-model="automationProjectForm.credentialRef" placeholder="例如：gitlab-qa-credentials" /></div></div><div class="two-col"><div class="field"><label>运行环境</label><input v-model="automationProjectForm.environment" /></div><div class="field"><label>Jenkins Job</label><input v-model="automationProjectForm.jenkinsJob" /></div></div><div class="field"><label>执行命令</label><input v-model="automationProjectForm.executeCommand" /></div><div class="modal-foot"><button type="button" class="btn" @click="closeModal">取消</button><button class="btn primary">{{ editingAutomationProjectId ? '保存配置' : '创建自动化项目' }}</button></div></form>
        <form v-else-if="modal === 'task'" class="modal-body" @submit.prevent="createTask"><div class="field"><label>任务名称 *</label><input v-model="taskForm.name" /></div><div class="two-col"><div class="field"><label>自动化项目 *</label><select v-model.number="taskForm.automationProjectId"><option :value="null" disabled>请选择项目</option><option v-for="project in automationProjects" :key="project.id" :value="project.id">{{ project.name }}</option></select></div><div class="field"><label>任务类型</label><input v-model="taskForm.taskType" /></div></div><div class="two-col"><div class="field"><label>环境</label><input v-model="taskForm.environment" /></div><div class="field"><label>Branch</label><input v-model="taskForm.branch" /></div></div><div class="field"><label>Jenkins Job</label><input v-model="taskForm.jenkinsJob" placeholder="留空使用自动化项目配置" /></div><div class="modal-foot"><button type="button" class="btn" @click="closeModal">取消</button><button class="btn primary">创建测试任务</button></div></form>
        <form v-else-if="modal === 'user'" class="modal-body" @submit.prevent="saveUser"><div v-if="!editingUserId" class="field"><label>用户名 *</label><input v-model="userForm.username" autocomplete="off" /></div><div class="two-col"><div class="field"><label>显示名称 *</label><input v-model="userForm.displayName" /></div><div class="field"><label>角色</label><select v-model="userForm.role"><option value="TEST_DEVELOPER">测试开发</option><option value="ADMIN">管理员</option></select></div></div><div class="field"><label>邮箱</label><input v-model="userForm.email" type="email" /></div><div class="field"><label>{{ editingUserId ? '重置密码（可选）' : '初始密码 *' }}</label><input v-model="userForm.password" type="password" autocomplete="new-password" /></div><div class="modal-foot"><button type="button" class="btn" @click="closeModal">取消</button><button class="btn primary">{{ editingUserId ? '保存用户' : '创建用户' }}</button></div></form>
        <form v-else-if="modal === 'member'" class="modal-body" @submit.prevent="addProjectMember"><div class="field"><label>用户</label><select v-model.number="memberForm.userId"><option :value="null" disabled>请选择启用用户</option><option v-for="user in users.filter((item) => item.status !== 'DISABLED' && !projectMembers.some((member) => member.userId === item.id))" :key="user.id" :value="user.id">{{ user.displayName }} · {{ user.username }}</option></select></div><div class="field"><label>项目角色</label><select v-model="memberForm.role"><option value="MANAGER">项目管理员</option><option value="TEST_DEVELOPER">测试开发</option><option value="VIEWER">只读成员</option></select></div><div class="modal-foot"><button type="button" class="btn" @click="closeModal">取消</button><button class="btn primary">添加成员</button></div></form>
        <div v-else class="modal-body"><div class="ai-callout"><strong>暂无持久化 AI 分析结果</strong><p>当前版本先展示 Jenkins 执行状态、日志和报告引用；AI 失败分析将在结果持久化后接入。</p></div><div class="modal-foot"><button class="btn" @click="closeModal">关闭</button></div></div>
      </div>
    </div>
  </div>
</template>
