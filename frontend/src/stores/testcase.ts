import { computed, reactive, ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/api'
import type { ApiTestCase } from '@/api'
import type { TestCase } from '@/types'
import { useUiStore } from './ui'
import { useProjectStore } from './project'
import { useRequirementStore } from './requirement'

export const useTestcaseStore = defineStore('testcase', () => {
  const ui = useUiStore()
  const projectStore = useProjectStore()
  const requirementStore = useRequirementStore()

  const casesBySuite = reactive<Record<'FUNCTIONAL' | 'API', TestCase[]>>({
    FUNCTIONAL: [],
    API: []
  })
  const testCases = computed(() => [...casesBySuite.FUNCTIONAL, ...casesBySuite.API])
  const allTestCases = ref<TestCase[]>([])
  const caseForm = ref({
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
    suiteType: 'FUNCTIONAL',
    tags: ''
  })
  const editingCaseId = ref<number | null>(null)
  let loadSequence = 0

  const filteredCases = computed(() => {
    const term = ui.search.trim().toLowerCase()
    return testCases.value.filter((item) => {
      const matchesSearch = !term || `${item.no}${item.title}${item.module}`.toLowerCase().includes(term)
      const matchesStatus = !ui.caseStatusFilter || item.status === ui.caseStatusFilter
      const matchesPriority = !ui.casePriorityFilter || item.priority === ui.casePriorityFilter
      const matchesType = !ui.caseTypeFilter || item.type === ui.caseTypeFilter
      return matchesSearch && matchesStatus && matchesPriority && matchesType
    })
  })

  function caseStatusLabel(status: TestCase['status']) {
    return {
      DRAFT: '草稿',
      REVIEW: '待处理',
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

  function caseTypeKey(type: string) {
    return {
      功能: 'FUNCTIONAL',
      异常: 'EXCEPTION',
      边界: 'BOUNDARY'
    }[type] ?? type
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
      suiteType: testCase.suiteType === 'API' ? '接口测试' : '功能测试',
      status: testCase.status,
      source: testCase.source,
      precondition: testCase.precondition || '',
      steps: testCase.steps || '',
      testData: testCase.testData || '',
      expectedResult: testCase.expectedResult || '',
      tags: testCase.tags || '',
      extraFields: testCase.extraFields || {}
    }
  }

  async function loadTestCases(filters: {
    status?: string | null
    keyword?: string | null
    priority?: string | null
    caseType?: string | null
    source?: string | null
    suiteType?: string | null
    page?: number
    size?: number
  } = {}) {
    const projectId = projectStore.currentProject.id
    if (!projectId) return
    const sequence = ++loadSequence
    const status = Object.prototype.hasOwnProperty.call(filters, 'status') ? filters.status || undefined : undefined
    const keyword = Object.prototype.hasOwnProperty.call(filters, 'keyword') ? filters.keyword || undefined : ui.search || undefined
    const priority = Object.prototype.hasOwnProperty.call(filters, 'priority') ? filters.priority || undefined : ui.casePriorityFilter || undefined
    const caseType = Object.prototype.hasOwnProperty.call(filters, 'caseType')
      ? filters.caseType ? caseTypeKey(filters.caseType) : undefined
      : ui.caseTypeFilter ? caseTypeKey(ui.caseTypeFilter) : undefined
    const source = Object.prototype.hasOwnProperty.call(filters, 'source') ? filters.source || undefined : undefined
    const suiteType = Object.prototype.hasOwnProperty.call(filters, 'suiteType')
      ? filters.suiteType || undefined
      : ui.activeCaseSuiteType
    const result = await api.testCases.list(projectId, {
      status,
      keyword,
      priority,
      caseType,
      source,
      suiteType,
      page: filters.page,
      size: filters.size
    })
    if (sequence !== loadSequence) return
    const mappedCases = result.content.map(fromApiTestCase)
    const requestedSuiteType = Object.prototype.hasOwnProperty.call(filters, 'suiteType')
      ? filters.suiteType
      : ui.activeCaseSuiteType
    if (requestedSuiteType === 'FUNCTIONAL' || requestedSuiteType === 'API') {
      casesBySuite[requestedSuiteType].splice(0, casesBySuite[requestedSuiteType].length, ...mappedCases)
    } else {
      casesBySuite.FUNCTIONAL.splice(
        0,
        casesBySuite.FUNCTIONAL.length,
        ...mappedCases.filter((item) => item.suiteType === '功能测试')
      )
      casesBySuite.API.splice(
        0,
        casesBySuite.API.length,
        ...mappedCases.filter((item) => item.suiteType === '接口测试')
      )
    }
  }

  function casesForSuite(suiteType: 'FUNCTIONAL' | 'API') {
    return casesBySuite[suiteType]
  }

  async function loadAllTestCases() {
    const projectId = projectStore.currentProject.id
    if (!projectId) return
    const all: TestCase[] = []
    let page = 0
    let totalPages = 1
    do {
      const result = await api.testCases.list(projectId, {
        status: '',
        keyword: '',
        priority: '',
        caseType: '',
        source: '',
        suiteType: '',
        page,
        size: 100
      })
      all.push(...result.content.map(fromApiTestCase))
      totalPages = Math.max(result.totalPages, 1)
      page += 1
    } while (page < totalPages)
    allTestCases.value.splice(0, allTestCases.value.length, ...all)
  }

  function openNewCase() {
    editingCaseId.value = null
    Object.assign(caseForm.value, {
      requirementId: requirementStore.selectedRequirementId,
      caseNo: '',
      title: '',
      moduleName: '',
      precondition: '',
      steps: '',
      testData: '',
      expectedResult: '',
      priority: 'P1',
      caseType: 'FUNCTIONAL',
      suiteType: ui.activeCaseSuiteType,
      tags: ''
    })
    ui.openModal('caseEdit')
  }

  function editCase(testCase: TestCase) {
    editingCaseId.value = testCase.id
    Object.assign(caseForm.value, {
      requirementId: testCase.requirementId ?? null,
      caseNo: testCase.no,
      title: testCase.title,
      moduleName: testCase.module,
      precondition: testCase.precondition,
      steps: testCase.steps,
      testData: testCase.testData,
      expectedResult: testCase.expectedResult,
      priority: testCase.priority,
      caseType: caseTypeKey(testCase.type),
      suiteType: testCase.suiteType === '接口测试' ? 'API' : 'FUNCTIONAL',
      tags: testCase.tags
    })
    ui.openModal('caseEdit')
  }

  async function saveCase() {
    const projectId = projectStore.currentProject.id
    if (!projectId || !caseForm.value.title.trim()) {
      ui.showToast('请填写测试用例标题')
      return
    }
    try {
      if (editingCaseId.value) {
        await api.testCases.update(projectId, editingCaseId.value, caseForm.value)
      } else {
        await api.testCases.create(projectId, caseForm.value)
      }
      await loadTestCases()
      await loadAllTestCases()
      const wasEditing = Boolean(editingCaseId.value)
      ui.closeModal()
      ui.showToast(wasEditing ? '测试用例已更新' : '测试用例已创建')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '测试用例保存失败')
    }
  }

  async function changeCaseStatus(testCase: TestCase, status: TestCase['status']) {
    const projectId = projectStore.currentProject.id
    if (!projectId) return
    try {
      await api.testCases.changeStatus(projectId, testCase.id, status)
      await loadTestCases()
      await loadAllTestCases()
      ui.showToast(`已将 ${testCase.no} 更新为${caseStatusLabel(status)}`)
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '状态更新失败')
    }
  }

  async function deleteCase(testCase: TestCase) {
    const projectId = projectStore.currentProject.id
    if (!projectId || !window.confirm(`确认删除 ${testCase.no} 吗？`)) return
    try {
      await api.testCases.remove(projectId, testCase.id)
      await loadTestCases()
      await loadAllTestCases()
      ui.showToast('测试用例已删除')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '测试用例删除失败')
    }
  }

  return {
    testCases,
    casesBySuite,
    casesForSuite,
    allTestCases,
    caseForm,
    editingCaseId,
    filteredCases,
    caseStatusLabel,
    caseTypeLabel,
    loadTestCases,
    loadAllTestCases,
    openNewCase,
    editCase,
    saveCase,
    changeCaseStatus,
    deleteCase
  }
})
