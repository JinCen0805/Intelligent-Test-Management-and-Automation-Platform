<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { api } from '@/api'
import { useUiStore } from '@/stores/ui'
import { useAiStore } from '@/stores/ai'
import { useProjectStore } from '@/stores/project'
import { useRequirementStore } from '@/stores/requirement'
import { useTestcaseStore } from '@/stores/testcase'
import CaseSuiteToggle from '@/components/common/CaseSuiteToggle.vue'

const ui = useUiStore()
const aiStore = useAiStore()
const projectStore = useProjectStore()
const requirementStore = useRequirementStore()
const testcaseStore = useTestcaseStore()

const { requirements, selectedRequirementId } = storeToRefs(requirementStore)
const { aiModels, generationForm, caseChatMessages, caseChatInput, caseChatFile, isChatting } = storeToRefs(aiStore)
const { loadRequirements } = requirementStore
const { loadTestCases, caseStatusLabel, casesForSuite, loadAllTestCases } = testcaseStore
const { loadAiConfig, loadCaseChatHistory, resetCaseChat, sendCaseChat, setCaseChatFile } = aiStore

const PAGE_SIZE = 10
const batchWorking = ref(false)
const currentPage = ref(1)
const selectedCaseIds = ref<number[]>([])
const localFileInput = ref<HTMLInputElement | null>(null)
const caseChatTextarea = ref<HTMLTextAreaElement | null>(null)
const caseChatStream = ref<HTMLDivElement | null>(null)
const chatCollapsed = ref(false)
const activeSuiteType = computed(() => ui.activeCaseSuiteType)
const suiteLabel = computed(() => activeSuiteType.value === 'API' ? '接口测试' : '功能测试')
const aiCases = computed(() => casesForSuite(activeSuiteType.value)
  .filter((item) => item.source === 'AI' && item.status !== 'APPROVED' && item.suiteType === suiteLabel.value))
const totalPages = computed(() => Math.max(1, Math.ceil(aiCases.value.length / PAGE_SIZE)))
const pagedAiCases = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return aiCases.value.slice(start, start + PAGE_SIZE)
})
const allCasesSelected = computed(() => aiCases.value.length > 0 && aiCases.value.every((item) => selectedCaseIds.value.includes(item.id)))
const extraFieldColumns = computed(() => {
  const names = new Set<string>()
  aiCases.value.forEach((testCase) => {
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

function refreshPage() {
  void Promise.all([
    loadRequirements(),
    loadTestCases({ source: 'AI', status: '', keyword: '', priority: '', caseType: '', suiteType: activeSuiteType.value }),
    loadAllTestCases(),
    loadAiConfig(),
    loadCaseChatHistory(true)
  ])
}

function resizeChatInput() {
  const textarea = caseChatTextarea.value
  if (!textarea) return
  textarea.style.height = 'auto'
  const maxHeight = 220
  const nextHeight = Math.min(Math.max(textarea.scrollHeight, 76), maxHeight)
  textarea.style.height = `${nextHeight}px`
  textarea.style.overflowY = textarea.scrollHeight > maxHeight ? 'auto' : 'hidden'
}

function scrollChatToBottom() {
  void nextTick(() => {
    requestAnimationFrame(() => {
      const stream = caseChatStream.value
      if (!stream) return
      stream.scrollTo({ top: stream.scrollHeight, behavior: 'smooth' })
    })
  })
}

function handleCaseChatKeydown(event: KeyboardEvent) {
  if (event.key !== 'Enter' || event.shiftKey) return
  event.preventDefault()
  if (event.isComposing || isChatting.value) return
  void sendCaseChat()
}

async function uploadLocalFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0] ?? null
  if (!file) return
  setCaseChatFile(file)
  generationForm.value.requirementId = null
  input.value = ''
}

function closeFileMenu(event: Event) {
  (event.currentTarget as HTMLElement | null)?.closest('details')?.removeAttribute('open')
}

function openLocalFilePicker(event: Event) {
  closeFileMenu(event)
  localFileInput.value?.click()
}

function selectProjectFile(fileId: number, event: Event) {
  closeFileMenu(event)
  setCaseChatFile(null)
  generationForm.value.requirementId = fileId
}

function toggleSelectedCase(caseId: number) {
  const index = selectedCaseIds.value.indexOf(caseId)
  if (index >= 0) {
    selectedCaseIds.value.splice(index, 1)
    if (generationForm.value.selectedCaseId === caseId) generationForm.value.selectedCaseId = null
  } else {
    selectedCaseIds.value.push(caseId)
    generationForm.value.selectedCaseId = caseId
  }
}

function toggleAllCases() {
  if (allCasesSelected.value) {
    selectedCaseIds.value = []
    generationForm.value.selectedCaseId = null
    return
  }
  selectedCaseIds.value = aiCases.value.map((item) => item.id)
  generationForm.value.selectedCaseId = selectedCaseIds.value.at(-1) ?? null
}

async function openHistoryModal() {
  await loadCaseChatHistory(false)
  ui.openModal('caseChatHistory')
}

function goToPage(page: number) {
  currentPage.value = Math.min(Math.max(page, 1), totalPages.value)
}

async function approveSelectedCases() {
  const projectId = projectStore.currentProject.id
  const items = aiCases.value.filter((item) => selectedCaseIds.value.includes(item.id))
  if (!projectId || items.length === 0 || batchWorking.value) return
  batchWorking.value = true
  try {
    const results = await Promise.allSettled(items.map((item) => api.testCases.changeStatus(projectId, item.id, 'APPROVED')))
    await loadTestCases({ source: 'AI', status: '', keyword: '', priority: '', caseType: '', suiteType: activeSuiteType.value })
    await loadAllTestCases()
    selectedCaseIds.value = selectedCaseIds.value.filter((id) => !items.some((item) => item.id === id))
    if (items.some((item) => item.id === generationForm.value.selectedCaseId)) {
      generationForm.value.selectedCaseId = null
    }
    refreshAiPageState()
    const successCount = results.filter((item) => item.status === 'fulfilled').length
    ui.showToast(successCount === items.length ? `已通过 ${successCount} 条用例` : `已通过 ${successCount} 条用例，${items.length - successCount} 条失败`)
  } catch (error) {
    ui.showToast(error instanceof Error ? error.message : '批量通过失败')
  } finally {
    batchWorking.value = false
  }
}

async function deleteSelectedCases() {
  const projectId = projectStore.currentProject.id
  const items = aiCases.value.filter((item) => selectedCaseIds.value.includes(item.id))
  if (!projectId || items.length === 0 || batchWorking.value) return
  if (!window.confirm(`确认删除当前 ${items.length} 条 AI 用例吗？`)) return
  batchWorking.value = true
  try {
    const results = await Promise.allSettled(items.map((item) => api.testCases.remove(projectId, item.id)))
    await loadTestCases({ source: 'AI', status: '', keyword: '', priority: '', caseType: '', suiteType: activeSuiteType.value })
    await loadAllTestCases()
    selectedCaseIds.value = selectedCaseIds.value.filter((id) => !items.some((item) => item.id === id))
    if (items.some((item) => item.id === generationForm.value.selectedCaseId)) {
      generationForm.value.selectedCaseId = null
    }
    refreshAiPageState()
    const successCount = results.filter((item) => item.status === 'fulfilled').length
    ui.showToast(successCount === items.length ? `已删除 ${successCount} 条用例` : `已删除 ${successCount} 条用例，${items.length - successCount} 条失败`)
  } catch (error) {
    ui.showToast(error instanceof Error ? error.message : '批量删除失败')
  } finally {
    batchWorking.value = false
  }
}

function refreshAiPageState() {
  if (currentPage.value > totalPages.value) currentPage.value = totalPages.value
  if (currentPage.value < 1) currentPage.value = 1
}

watch(aiCases, () => {
  refreshAiPageState()
  selectedCaseIds.value = selectedCaseIds.value.filter((id) => aiCases.value.some((item) => item.id === id))
})
watch(requirements, () => {
  if (!generationForm.value.requirementId) {
    generationForm.value.requirementId = selectedRequirementId.value
  }
})
watch(activeSuiteType, () => {
  currentPage.value = 1
  refreshPage()
})
watch(caseChatInput, () => { void nextTick(resizeChatInput) })
watch(() => caseChatMessages.value.length, scrollChatToBottom)
watch(isChatting, scrollChatToBottom)

onMounted(() => {
  refreshPage()
  void nextTick(resizeChatInput)
  scrollChatToBottom()
})
</script>

<template>
  <section class="case-ai-page">
    <div class="page-head compact">
      <div class="head-actions case-suite-head">
        <CaseSuiteToggle v-model="ui.activeCaseSuiteType" />
        <button class="btn" @click="refreshPage">刷新</button>
      </div>
    </div>

    <div class="case-ai-grid">
      <div class="card case-ai-list">
        <div class="card-head">
          <div>
            <h3>{{ suiteLabel }}用例</h3>
          </div>
          <div class="head-actions">
            <button class="btn primary" :disabled="batchWorking || selectedCaseIds.length === 0" @click="approveSelectedCases">通过</button>
            <button class="btn danger-text" :disabled="batchWorking || selectedCaseIds.length === 0" @click="deleteSelectedCases">删除</button>
            <span class="tag blue">{{ aiCases.length }} 条</span>
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
                <th>测试类型</th>
                <th>状态</th>
                <th v-for="field in extraFieldColumns" :key="field">{{ field }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="testCase in pagedAiCases" :key="testCase.id">
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
              <tr v-if="aiCases.length === 0">
                <td :colspan="10 + extraFieldColumns.length" class="muted">暂无 {{ suiteLabel }} AI 生成测试用例</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="pagination case-pagination">
          <span class="muted">第 {{ currentPage }} / {{ totalPages }} 页，共 {{ aiCases.length }} 条</span>
          <div class="head-actions">
            <button class="btn" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">上一页</button>
            <button
              v-for="page in totalPages"
              :key="page"
              class="btn page-btn"
              :class="{ active: page === currentPage }"
              :disabled="page === currentPage"
              @click="goToPage(page)"
            >
              {{ page }}
            </button>
            <button class="btn" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">下一页</button>
          </div>
        </div>
      </div>

      <div v-if="!chatCollapsed" class="card case-ai-chat">
        <div class="card-head">
          <h3>AI 对话</h3>
          <div class="head-actions">
            <button class="btn" type="button" @click="resetCaseChat">新建对话</button>
            <button class="btn" type="button" @click="openHistoryModal">历史对话</button>
            <button
              class="btn case-ai-chat-collapse-btn"
              type="button"
              aria-label="向右收起 AI 对话"
              title="向右收起 AI 对话"
              @click="chatCollapsed = true"
            >
              <svg class="case-ai-chat-toggle-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                <path d="M10 8 6 12l4 4" />
                <path d="M6 12h12" />
                <path d="M18 4v16" />
              </svg>
            </button>
          </div>
        </div>
        <div class="card-body case-chat-body">
          <div class="case-chat-shell">
            <div ref="caseChatStream" class="case-chat-stream" aria-live="polite" :aria-busy="isChatting">
              <div v-for="message in caseChatMessages" :key="message.id" class="case-chat-row" :class="message.role">
                <div class="case-chat-avatar">{{ message.role === 'user' ? '你' : 'AI' }}</div>
                <div class="case-chat-message" :class="message.role">
                  <div class="case-chat-meta">
                    <strong>{{ message.role === 'user' ? '你' : 'AI' }}</strong>
                    <span>{{ message.role === 'user' ? '正在编辑指令' : '测试用例对话' }}</span>
                  </div>
                  <p>{{ message.content }}</p>
                </div>
              </div>
              <div v-if="isChatting" class="case-chat-row assistant case-chat-loading-row" role="status" aria-label="AI 正在生成">
                <div class="case-chat-avatar">AI</div>
                <div class="case-chat-message assistant case-chat-loading-message">
                  <div class="case-chat-meta">
                    <strong>AI</strong>
                    <span>正在生成</span>
                  </div>
                  <div class="case-chat-loading-dots" aria-hidden="true"><i></i><i></i><i></i></div>
                </div>
              </div>
            </div>

            <form class="case-chat-form" @submit.prevent="sendCaseChat">
              <div class="case-chat-input-wrap">
                <div v-if="caseChatFile || generationForm.requirementId" class="case-chat-attachment">
                  <span class="muted">已添加文件</span>
                  <strong v-if="caseChatFile">{{ caseChatFile.name }}</strong>
                  <strong v-else>{{ requirements.find((item) => item.id === generationForm.requirementId)?.name || '项目文件' }}</strong>
                  <button class="link-btn" type="button" aria-label="移除附件" @click="setCaseChatFile(null); generationForm.requirementId = null">移除</button>
                </div>
                <textarea ref="caseChatTextarea" v-model="caseChatInput" rows="1" placeholder="输入你的修改指令，例如：补充 5 条登录失败后的异常用例" @keydown="handleCaseChatKeydown"></textarea>
                <div class="case-chat-input-actions">
                  <details class="case-file-menu">
                    <summary class="btn" aria-label="添加文件">＋ 添加文件</summary>
                    <div class="case-file-menu-panel">
                      <span class="case-file-menu-label">项目文件</span>
                      <button v-for="requirement in requirements.filter((item) => item.id)" :key="requirement.id" type="button" :class="{ selected: generationForm.requirementId === requirement.id && !caseChatFile }" @click="selectProjectFile(requirement.id ?? 0, $event)">
                        {{ requirement.name }}{{ requirement.version ? ` · ${requirement.version}` : '' }}
                      </button>
                      <span v-if="requirements.length === 0" class="muted">暂无项目文件</span>
                      <button type="button" class="case-file-upload" @click="openLocalFilePicker">⇧ 从本地上传</button>
                    </div>
                  </details>
                  <input ref="localFileInput" class="case-file-input" type="file" accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.markdown,.csv,.tsv,.json,.xml,.html,.htm,.yaml,.yml,.sql,.log,.rtf,.zip,.rar,.7z,.png,.jpg,.jpeg,.gif,.webp" @change="uploadLocalFile" />
                </div>
              </div>
              <div class="case-chat-foot">
                <select v-model.number="generationForm.modelId" class="toolbar-select case-chat-model">
                  <option :value="null">自动选择模型</option>
                  <option v-for="model in aiModels" :key="model.id" :value="model.id">
                    {{ model.name }} · {{ model.provider }}
                  </option>
                </select>
                <button class="btn primary case-chat-send" :disabled="isChatting">
                  <span v-if="isChatting" class="loading-spinner" aria-hidden="true"></span>
                  {{ isChatting ? '正在生成' : '发送' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
      <button
        v-else
        class="btn case-ai-chat-collapsed-toggle"
        type="button"
        aria-label="展开 AI 对话"
        title="展开 AI 对话"
        @click="chatCollapsed = false"
      >
        <svg class="case-ai-chat-toggle-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
          <path d="m14 8 4 4-4 4" />
          <path d="M18 12H6" />
          <path d="M6 4v16" />
        </svg>
        <span>AI 对话</span>
      </button>
    </div>
  </section>
</template>
