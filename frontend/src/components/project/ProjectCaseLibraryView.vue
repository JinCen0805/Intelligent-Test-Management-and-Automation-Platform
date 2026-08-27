<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
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
const { loadTestCases, loadAllTestCases, casesForSuite, editCase, deleteCase } = testcaseStore
const activeSuiteType = computed(() => ui.activeCaseSuiteType)
const suiteLabel = computed(() => activeSuiteType.value === 'API' ? '接口测试' : '功能测试')

const libraryCases = computed(() => {
  const term = search.value.trim().toLowerCase()
  return casesForSuite(activeSuiteType.value).filter((item) => {
    const matchesSource = item.source === 'AI'
    const matchesStatus = item.status === 'APPROVED'
    const matchesSuite = item.suiteType === suiteLabel.value
    const matchesSearch = !term || `${item.no}${item.title}${item.module}`.toLowerCase().includes(term)
    const matchesPriority = !casePriorityFilter.value || item.priority === casePriorityFilter.value
    const matchesType = !caseTypeFilter.value || item.type === caseTypeFilter.value
    return matchesSource && matchesStatus && matchesSuite && matchesSearch && matchesPriority && matchesType
  })
})

function refreshPage() {
  void Promise.all([
    loadTestCases({
      source: 'AI',
      status: 'APPROVED',
      keyword: '',
      priority: '',
      caseType: '',
      suiteType: activeSuiteType.value
    }),
    loadAllTestCases()
  ])
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

watch(activeSuiteType, refreshPage)

onMounted(() => {
  refreshPage()
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
      </div>
      <div class="card-body table-wrap">
        <table>
          <thead>
            <tr>
              <th>编号</th>
              <th>标题</th>
              <th>模块</th>
              <th>优先级</th>
              <th>类型</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="testCase in libraryCases" :key="testCase.id">
              <td><strong>{{ testCase.no }}</strong></td>
              <td class="case-title-cell">{{ testCase.title }}</td>
              <td>{{ testCase.module }}</td>
              <td><span class="tag" :class="testCase.priority === 'P0' ? 'red' : 'orange'">{{ testCase.priority }}</span></td>
              <td>{{ testCase.type }}</td>
              <td>
                <div class="head-actions">
                  <button class="btn" @click="editCase(testCase)">编辑</button>
                  <button class="btn" @click="deleteCase(testCase)">删除</button>
                </div>
              </td>
            </tr>
            <tr v-if="libraryCases.length === 0">
              <td colspan="6" class="muted">暂无 {{ suiteLabel }}用例</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>
