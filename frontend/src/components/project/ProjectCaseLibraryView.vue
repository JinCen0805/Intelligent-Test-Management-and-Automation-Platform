<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useProjectStore } from '@/stores/project'
import { useTestcaseStore } from '@/stores/testcase'
import { useUiStore } from '@/stores/ui'
import { api } from '@/api'
import CaseSuiteToggle from '@/components/common/CaseSuiteToggle.vue'

const ui = useUiStore()
const projectStore = useProjectStore()
const testcaseStore = useTestcaseStore()

const { currentProject } = storeToRefs(projectStore)
const { casePriorityFilter, caseTypeFilter, search } = storeToRefs(ui)
const { loadTestCases, casesForSuite, caseStatusLabel, editCase } = testcaseStore
const { casePagination } = storeToRefs(testcaseStore)
const activeSuiteType = computed(() => ui.activeCaseSuiteType)
const suiteLabel = computed(() => activeSuiteType.value === 'API' ? '接口测试' : '功能测试')
const selectedCaseIds = ref<number[]>([])
const batchWorking = ref(false)
let filterTimer: number | undefined

const libraryCases = computed(() => casesForSuite(activeSuiteType.value))

const extraFieldColumns = computed(() => {
  const names = new Set<string>()
  libraryCases.value.forEach((testCase) => {
    Object.keys(testCase.extraFields ?? {}).forEach((name) => names.add(name))
  })
  return [...names]
})

function formatExtraField(value: unknown): string {
  if (value === null || value === undefined) return ''
  if (Array.isArray(value)) return value.map((item) => formatExtraField(item)).join(', ')
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

const allCasesSelected = computed(() => libraryCases.value.length > 0 && libraryCases.value.every((item) => selectedCaseIds.value.includes(item.id)))

function toggleSelectedCase(caseId: number) {
  const index = selectedCaseIds.value.indexOf(caseId)
  if (index >= 0) {
    selectedCaseIds.value.splice(index, 1)
  } else {
    selectedCaseIds.value.push(caseId)
  }
}

function toggleAllCases() {
  selectedCaseIds.value = allCasesSelected.value ? [] : libraryCases.value.map((item) => item.id)
}

function editSelectedCase() {
  if (selectedCaseIds.value.length !== 1) return
  const selected = libraryCases.value.find((item) => item.id === selectedCaseIds.value[0])
  if (selected) editCase(selected)
}

async function deleteSelectedCases() {
  const projectId = currentProject.value.id
  const items = libraryCases.value.filter((item) => selectedCaseIds.value.includes(item.id))
  if (!projectId || items.length === 0 || batchWorking.value) return
  if (!window.confirm(`确认删除当前 ${items.length} 条用例吗？`)) return
  batchWorking.value = true
  try {
    const results = await Promise.allSettled(items.map((item) => api.testCases.remove(projectId, item.id)))
    await loadPage()
    // 当前页删空后回退一页，避免停在空页。
    if (libraryCases.value.length === 0 && casePagination.value.page > 0) {
      await loadPage(casePagination.value.page - 1)
    }
    selectedCaseIds.value = []
    const successCount = results.filter((item) => item.status === 'fulfilled').length
    ui.showToast(successCount === items.length ? `已删除 ${successCount} 条用例` : `已删除 ${successCount} 条用例，${items.length - successCount} 条失败`)
  } catch (error) {
    ui.showToast(error instanceof Error ? error.message : '批量删除失败')
  } finally {
    batchWorking.value = false
  }
}

async function loadPage(page = casePagination.value.page) {
  await loadTestCases({
    source: 'AI',
    status: 'APPROVED',
    suiteType: activeSuiteType.value,
    page,
    size: casePagination.value.size
  })
}

function goPage(page: number) {
  selectedCaseIds.value = []
  void loadPage(page)
}

function refreshPage() {
  void loadPage()
}

function mapCaseType(value: string) {
  return {
    功能: 'FUNCTIONAL',
    异常: 'EXCEPTION',
    边界: 'BOUNDARY'
  }[value] ?? value
}

async function exportExcel() {
  const projectId = currentProject.value.id
  if (!projectId) return
  await api.testCases.exportExcel(projectId, {
    status: 'APPROVED',
    keyword: search.value || undefined,
    priority: casePriorityFilter.value || undefined,
    caseType: caseTypeFilter.value ? mapCaseType(caseTypeFilter.value) : undefined,
    source: 'AI',
    suiteType: activeSuiteType.value
  })
}

watch(activeSuiteType, () => {
  selectedCaseIds.value = []
  void loadPage(0)
})
watch([search, casePriorityFilter, caseTypeFilter], () => {
  window.clearTimeout(filterTimer)
  filterTimer = window.setTimeout(() => {
    selectedCaseIds.value = []
    void loadPage(0)
  }, 300)
})
watch(libraryCases, () => {
  selectedCaseIds.value = selectedCaseIds.value.filter((id) => libraryCases.value.some((item) => item.id === id))
})

onMounted(() => {
  void loadPage(0)
})
</script>

<template>
  <section class="case-library-page">
    <div class="page-head compact">
      <div class="head-actions case-suite-head">
        <CaseSuiteToggle v-model="ui.activeCaseSuiteType" />
        <button class="btn" @click="refreshPage">刷新</button>
        <button class="btn" @click="exportExcel">导出 Excel</button>
      </div>
    </div>

    <div class="card">
      <div class="card-head">
        <div class="head-actions case-filters">
          <select class="toolbar-select" v-model="casePriorityFilter" aria-label="按优先级筛选">
            <option value="">全部优先级</option>
            <option>P0</option>
            <option>P1</option>
            <option>P2</option>
            <option>P3</option>
          </select>
          <select class="toolbar-select" v-model="caseTypeFilter" aria-label="按类型筛选">
            <option value="">全部类型</option>
            <option value="功能">功能</option>
            <option value="异常">异常</option>
            <option value="边界">边界</option>
            <option value="其他">其他</option>
          </select>
        </div>
        <div class="head-actions">
          <button class="btn" :disabled="selectedCaseIds.length !== 1 || batchWorking" @click="editSelectedCase">编辑</button>
          <button class="btn danger-text" :disabled="selectedCaseIds.length === 0 || batchWorking" @click="deleteSelectedCases">删除</button>
        </div>
      </div>
      <div class="card-body table-wrap">
        <table>
          <thead>
            <tr>
              <th><input type="checkbox" :checked="allCasesSelected" aria-label="全选用例" @change="toggleAllCases"></th>
              <th>编号</th>
              <th>模块</th>
              <th>标题</th>
              <th>前置条件</th>
              <th>测试步骤</th>
              <th>预期结果</th>
              <th>优先级</th>
              <th>类型</th>
              <th>状态</th>
              <th v-for="field in extraFieldColumns" :key="field">{{ field }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="testCase in libraryCases" :key="testCase.id">
              <td>
                <input
                  type="checkbox"
                  :checked="selectedCaseIds.includes(testCase.id)"
                  :aria-label="`选择 ${testCase.no}`"
                  @change="toggleSelectedCase(testCase.id)"
                >
              </td>
              <td><strong>{{ testCase.no }}</strong></td>
              <td>{{ testCase.module }}</td>
              <td class="case-title-cell">{{ testCase.title }}</td>
              <td class="case-content-cell">{{ testCase.precondition || '--' }}</td>
              <td class="case-content-cell">{{ testCase.steps || '--' }}</td>
              <td class="case-content-cell">{{ testCase.expectedResult || '--' }}</td>
              <td><span class="tag" :class="testCase.priority === 'P0' ? 'red' : 'orange'">{{ testCase.priority }}</span></td>
              <td>{{ testCase.type }}</td>
              <td><span class="tag" :class="testCase.status === 'APPROVED' ? 'green' : testCase.status === 'REVIEW' ? 'orange' : testCase.status === 'REJECTED' ? 'red' : 'gray'">{{ caseStatusLabel(testCase.status) }}</span></td>
              <td v-for="field in extraFieldColumns" :key="field" class="case-extra-field-cell">{{ formatExtraField(testCase.extraFields?.[field]) }}</td>
            </tr>
            <tr v-if="libraryCases.length === 0">
              <td :colspan="10 + extraFieldColumns.length" class="muted">暂无 {{ suiteLabel }}用例</td>
            </tr>
          </tbody>
        </table>
        <div v-if="casePagination.totalPages > 1" class="head-actions" style="justify-content: flex-end; padding-top: 10px">
          <button class="btn" :disabled="casePagination.page <= 0" @click="goPage(casePagination.page - 1)">上一页</button>
          <span class="muted">第 {{ casePagination.page + 1 }} / {{ casePagination.totalPages }} 页 · 共 {{ casePagination.totalElements }} 条</span>
          <button class="btn" :disabled="casePagination.page + 1 >= casePagination.totalPages" @click="goPage(casePagination.page + 1)">下一页</button>
        </div>
      </div>
    </div>
  </section>
</template>
