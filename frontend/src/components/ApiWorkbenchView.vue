<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useApiWorkbench } from '@/composables/useApiWorkbench'
import { useUiStore } from '@/stores/ui'
import { useProjectStore } from '@/stores/project'

const ui = useUiStore()
const projectStore = useProjectStore()
const { activeApiRequestTab, activeApiTab } = storeToRefs(ui)
const { currentProject } = storeToRefs(projectStore)
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
  deleteApiRequest,
  deleteApiCollection,
  variableToken
} = useApiWorkbench()

const responseTab = ref<'body' | 'headers' | 'tests' | 'request'>('body')
const bodyMode = ref<'none' | 'json' | 'raw' | 'form'>('json')
const authMode = ref<'none' | 'bearer' | 'basic' | 'apikey'>('none')
const authToken = ref('')
const authUsername = ref('')
const authPassword = ref('')
const apiKeyName = ref('X-API-Key')
const apiKeyValue = ref('')
const searchText = ref('')
const showNewRequest = ref(false)
const showImport = ref(false)
const importText = ref('')
const isSending = ref(false)
const showRequestMenu = ref(false)
const currentFolderId = ref<string | null>(null)
const apiFolders = reactive<ApiFolder[]>([])
const requestFolders = reactive<Record<number, string>>({})
const expandedCollections = reactive<Record<number, boolean>>({})

interface ApiFolder {
  id: string
  collectionId: number
  parentId: string | null
  name: string
}

const visibleRequests = computed(() => {
  const query = searchText.value.trim().toLowerCase()
  if (!query) return apiRequests
  return apiRequests.filter((item) => `${item.name} ${item.method} ${item.url}`.toLowerCase().includes(query))
})

const selectedCollection = computed(() => apiCollections.find((item) => item.id === selectedApiCollectionId.value))
const selectedRequest = computed(() => apiRequests.find((item) => item.id === selectedApiRequestId.value))
function treeNodes(collectionId: number) {
  const nodes: Array<{ kind: 'folder' | 'request'; depth: number; folder?: ApiFolder; request?: typeof apiRequests[number] }> = []
  function walk(parentId: string | null, depth: number) {
    collectionFolders(collectionId, parentId).forEach((folder) => {
      nodes.push({ kind: 'folder', depth, folder })
      walk(folder.id, depth + 1)
    })
    collectionRequests(collectionId, parentId).forEach((request) => {
      nodes.push({ kind: 'request', depth, request })
    })
  }
  walk(null, 0)
  return nodes
}
const responseHeaders = computed(() => {
  if (!apiResponse.value?.responseHeadersJson) return '{}'
  try {
    return JSON.stringify(JSON.parse(apiResponse.value.responseHeadersJson), null, 2)
  } catch {
    return apiResponse.value.responseHeadersJson
  }
})
const formattedResponse = computed(() => {
  const body = apiResponse.value?.responseBody || ''
  if (!body) return ''
  try {
    return JSON.stringify(JSON.parse(body), null, 2)
  } catch {
    return body
  }
})
const testResults = computed(() => {
  const value = apiResponse.value?.assertionResultsJson || '[]'
  try {
    return JSON.parse(value) as Array<{ type: string; path?: string; expected?: string; passed: boolean }>
  } catch {
    return []
  }
})

function pretty(value: string, fallback: string) {
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return fallback
  }
}

function setRequestTab(tab: typeof activeApiRequestTab.value) {
  activeApiRequestTab.value = tab
}

function selectCollection(id: number) {
  selectedApiCollectionId.value = id
  currentFolderId.value = null
  expandedCollections[id] = true
  saveTreeMeta()
  void loadApiRequests(id)
}

function toggleCollection(id: number) {
  expandedCollections[id] = !expandedCollections[id]
  if (expandedCollections[id]) {
    selectedApiCollectionId.value = id
    currentFolderId.value = null
    void loadApiRequests(id)
  }
  saveTreeMeta()
}

function selectFolder(folderId: string) {
  currentFolderId.value = folderId
}

function workspaceMetaKey() {
  return `testpilot:api-workbench-tree:${currentProject.value?.id || 'global'}`
}

function loadTreeMeta() {
  try {
    const parsed = JSON.parse(window.localStorage.getItem(workspaceMetaKey()) || '{}') as {
      folders?: ApiFolder[]
      requestFolders?: Record<string, string>
      expandedCollections?: Record<string, boolean>
    }
    apiFolders.splice(0, apiFolders.length, ...(parsed.folders || []))
    Object.keys(requestFolders).forEach((key) => delete requestFolders[Number(key)])
    Object.entries(parsed.requestFolders || {}).forEach(([requestId, folderId]) => {
      requestFolders[Number(requestId)] = folderId
    })
    Object.keys(expandedCollections).forEach((key) => delete expandedCollections[Number(key)])
    Object.entries(parsed.expandedCollections || {}).forEach(([collectionId, expanded]) => {
      expandedCollections[Number(collectionId)] = expanded
    })
  } catch {
    apiFolders.splice(0, apiFolders.length)
    Object.keys(requestFolders).forEach((key) => delete requestFolders[Number(key)])
    Object.keys(expandedCollections).forEach((key) => delete expandedCollections[Number(key)])
  }
}

function saveTreeMeta() {
  window.localStorage.setItem(workspaceMetaKey(), JSON.stringify({
    folders: apiFolders,
    requestFolders,
    expandedCollections
  }))
}

function collectionFolders(collectionId: number, parentId: string | null) {
  return apiFolders.filter((folder) => folder.collectionId === collectionId && folder.parentId === parentId)
}

function collectionRequests(collectionId: number, folderId: string | null) {
  return visibleRequests.value.filter((request) => {
    if (request.collectionId !== collectionId) return false
    const assignedFolder = requestFolders[request.id] || null
    return assignedFolder === folderId
  })
}

function addFolder(collectionId: number, parentId: string | null = null) {
  const name = window.prompt('文件夹名称')
  if (!name?.trim()) return
  apiFolders.push({
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    collectionId,
    parentId,
    name: name.trim()
  })
  saveTreeMeta()
}

function newRequest(folderId: string | null = null, collectionId = selectedApiCollectionId.value) {
  if (collectionId) selectedApiCollectionId.value = collectionId
  currentFolderId.value = folderId
  selectedApiRequestId.value = null
  apiRequestForm.name = 'Untitled Request'
  apiRequestForm.method = 'GET'
  apiRequestForm.url = ''
  apiRequestForm.paramsJson = '{}'
  apiRequestForm.headersJson = '{}'
  apiRequestForm.body = ''
  apiRequestForm.assertionsJson = '[]'
  apiRequestForm.preRequestScript = ''
  apiRequestForm.postRequestScript = ''
  apiResponse.value = null
  bodyMode.value = 'none'
  authMode.value = 'none'
  showNewRequest.value = false
  showRequestMenu.value = false
  activeApiRequestTab.value = 'params'
}

function normalizeJson(value: string, fallback: string) {
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return fallback
  }
}

function parseAuthFromHeaders() {
  let headers: Record<string, unknown> = {}
  try {
    headers = JSON.parse(apiRequestForm.headersJson || '{}')
  } catch {
    return
  }
  const authorization = headers.Authorization ?? headers.authorization
  if (typeof authorization === 'string' && authorization.toLowerCase().startsWith('bearer ')) {
    authMode.value = 'bearer'
    authToken.value = authorization.slice(7).trim()
  }
}

function base64Utf8(value: string): string {
  const bytes = new TextEncoder().encode(value)
  let binary = ''
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte)
  })
  return btoa(binary)
}

function applyAuth() {
  let headers: Record<string, string> = {}
  try {
    headers = JSON.parse(apiRequestForm.headersJson || '{}')
  } catch {
    headers = {}
  }
  // 只在用户显式填写时覆盖请求头，避免把已保存的 Authorization 静默删掉。
  if (authMode.value === 'bearer' && authToken.value.trim()) {
    headers.Authorization = `Bearer ${authToken.value.trim()}`
  } else if (authMode.value === 'basic' && (authUsername.value || authPassword.value)) {
    headers.Authorization = `Basic ${base64Utf8(`${authUsername.value}:${authPassword.value}`)}`
  } else if (authMode.value === 'apikey' && apiKeyName.value.trim() && apiKeyValue.value.trim()) {
    headers[apiKeyName.value.trim()] = apiKeyValue.value.trim()
  }
  apiRequestForm.headersJson = JSON.stringify(headers, null, 2)
}

watch(() => apiRequestForm.headersJson, parseAuthFromHeaders)

function syncBodyMode() {
  if (bodyMode.value === 'json' && !apiRequestForm.body.trim()) {
    apiRequestForm.body = '{\n  \n}'
  }
  if (bodyMode.value === 'none') apiRequestForm.body = ''
}

async function send() {
  isSending.value = true
  try {
    applyAuth()
    await sendApiRequest()
    responseTab.value = 'body'
  } catch (error) {
    ui.showToast(error instanceof Error ? error.message : '请求发送失败')
  } finally {
    isSending.value = false
  }
}

async function save() {
  applyAuth()
  await saveApiRequest()
  if (selectedApiRequestId.value) {
    if (currentFolderId.value) requestFolders[selectedApiRequestId.value] = currentFolderId.value
    else delete requestFolders[selectedApiRequestId.value]
    saveTreeMeta()
  }
}

async function removeRequest() {
  if (!selectedRequest.value) return
  if (!window.confirm(`删除请求“${selectedRequest.value.name}”？`)) return
  await deleteApiRequest(selectedRequest.value.id)
  showRequestMenu.value = false
  newRequest()
}

async function removeCollection() {
  if (!selectedCollection.value) return
  if (!window.confirm(`删除集合“${selectedCollection.value.name}”及其请求？`)) return
  await deleteApiCollection(selectedCollection.value.id)
}

function duplicateRequest() {
  const current = selectedRequest.value
  if (!current) return
  newRequest(currentFolderId.value, current.collectionId)
  apiRequestForm.name = `${current.name} Copy`
  apiRequestForm.method = current.method
  apiRequestForm.url = current.url
  apiRequestForm.paramsJson = current.paramsJson || '{}'
  apiRequestForm.headersJson = current.headersJson || '{}'
  apiRequestForm.body = current.body || ''
  apiRequestForm.assertionsJson = current.assertionsJson || '[]'
  apiRequestForm.preRequestScript = current.preRequestScript || ''
  apiRequestForm.postRequestScript = current.postRequestScript || ''
  showRequestMenu.value = false
}

function exportCurl() {
  const method = apiRequestForm.method.toUpperCase()
  let headers: Record<string, string> = {}
  try { headers = JSON.parse(apiRequestForm.headersJson || '{}') } catch { /* server validates on save */ }
  const headerText = Object.entries(headers).map(([key, value]) => ` -H ${JSON.stringify(`${key}: ${value}`)}`).join('')
  const body = apiRequestForm.body.trim() ? ` --data-raw ${JSON.stringify(apiRequestForm.body)}` : ''
  const curl = `curl --request ${method} ${JSON.stringify(apiRequestForm.url.trim() || 'https://example.com')}${headerText}${body}`
  void navigator.clipboard?.writeText(curl)
  ui.showToast('cURL 已复制')
}

function parseCurl(value: string) {
  const text = value.trim()
  const url = text.match(/(?:curl\s+.*?)(?:--url\s+|-X\s+\w+\s+)?(['"]?https?:\/\/[^'"\s]+['"]?)/i)?.[1]
  if (!url) {
    ui.showToast('没有识别到有效的 HTTP URL')
    return
  }
  const method = text.match(/(?:-X|--request)\s+([A-Z]+)/i)?.[1]?.toUpperCase() || (text.includes('--data') ? 'POST' : 'GET')
  const headers: Record<string, string> = {}
  for (const match of text.matchAll(/(?:-H|--header)\s+['"]([^'"]+)['"]/gi)) {
    const separator = match[1].indexOf(':')
    if (separator > 0) headers[match[1].slice(0, separator).trim()] = match[1].slice(separator + 1).trim()
  }
  const body = text.match(/(?:--data-raw|--data|--data-binary|-d)\s+['"]([\s\S]*?)['"]/i)?.[1] || ''
  newRequest(currentFolderId.value)
  apiRequestForm.method = method
  apiRequestForm.url = url.replace(/^['"]|['"]$/g, '')
  apiRequestForm.headersJson = JSON.stringify(headers, null, 2)
  apiRequestForm.body = body
  bodyMode.value = body ? 'raw' : 'none'
  showImport.value = false
  importText.value = ''
  ui.showToast('cURL 已导入，请保存请求')
}

function onKeydown(event: KeyboardEvent) {
  if (event.ctrlKey && event.key === 'Enter') {
    event.preventDefault()
    void send()
  } else if (event.ctrlKey && event.key.toLowerCase() === 's') {
    event.preventDefault()
    void save()
  }
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
  loadTreeMeta()
  void loadApiWorkbench()
})
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
watch(() => currentProject.value?.id, () => loadTreeMeta())
</script>

<template>
  <section class="api-workbench-shell">
    <header class="api-workbench-head">
      <div>
        <div class="eyebrow">API WORKBENCH</div>
        <h1>接口测试工作台</h1>
      </div>
      <div class="api-workbench-actions">
        <select v-model="apiVariableForm.environment" class="api-environment-select" title="当前环境" @change="loadApiWorkbench">
          <option>DEV</option>
          <option>TEST</option>
          <option>STAGING</option>
        </select>
        <button class="btn" title="导入 cURL" @click="showImport = true">导入</button>
        <button class="btn" title="刷新工作台" @click="loadApiWorkbench">刷新</button>
      </div>
    </header>

    <div class="api-workbench">
      <aside class="api-explorer">
        <div class="api-explorer-head">
          <strong>Collections</strong>
          <div>
            <button class="icon-btn api-add-action" title="新建用例集" @click="showNewRequest = true">＋</button>
          </div>
        </div>
        <label class="api-search">
          <span>⌕</span>
          <input v-model="searchText" placeholder="筛选请求" />
        </label>
        <div class="api-tree">
          <div v-for="collection in apiCollections" :key="collection.id" class="api-tree-group">
            <div class="api-tree-collection-row">
              <button class="api-tree-collection" :class="{ active: selectedApiCollectionId === collection.id }" @click="selectCollection(collection.id)">
                <span class="api-tree-chevron" :class="{ collapsed: !expandedCollections[collection.id] }" @click.stop="toggleCollection(collection.id)">▾</span><span class="collection-symbol">▤</span><strong>{{ collection.name }}</strong>
              </button>
              <div class="api-tree-node-actions">
                <button title="新建文件夹" @click.stop="addFolder(collection.id)">▰</button>
                <button title="新建 Request" @click.stop="newRequest(null, collection.id)">＋</button>
              </div>
            </div>
            <div v-if="expandedCollections[collection.id]" class="api-tree-requests">
              <template v-for="node in treeNodes(collection.id)" :key="node.kind === 'folder' ? node.folder?.id : node.request?.id">
                <div v-if="node.kind === 'folder'" class="api-tree-folder-row" :style="{ paddingLeft: `${node.depth * 14}px` }">
                  <button class="api-tree-folder" :class="{ active: currentFolderId === node.folder?.id }" @click="selectFolder(node.folder!.id)">
                    <span>▾</span><span class="folder-symbol">▰</span><span class="request-name">{{ node.folder?.name }}</span>
                  </button>
                  <div class="api-tree-node-actions">
                    <button title="新建子文件夹" @click.stop="addFolder(collection.id, node.folder!.id)">▰</button>
                    <button title="新建 Request" @click.stop="newRequest(node.folder!.id, collection.id)">＋</button>
                  </div>
                </div>
                <button v-else class="api-tree-request" :style="{ paddingLeft: `${node.depth * 14 + 7}px` }" :class="{ active: selectedApiRequestId === node.request?.id }" @click="currentFolderId = requestFolders[node.request!.id] || null; applyApiRequest(node.request!)">
                  <span class="method" :class="node.request?.method.toLowerCase()">{{ node.request?.method }}</span>
                  <span class="request-name">{{ node.request?.name }}</span>
                </button>
              </template>
              <span v-if="treeNodes(collection.id).length === 0" class="api-tree-empty">暂无请求或文件夹</span>
            </div>
          </div>
          <div v-if="apiCollections.length === 0" class="api-tree-empty">先创建一个集合</div>
        </div>
        <form class="api-new-collection" @submit.prevent="createApiCollection">
          <input v-model="apiCollectionForm.name" placeholder="新建集合名称" />
          <button class="btn" :disabled="!apiCollectionForm.name.trim()">创建</button>
        </form>
      </aside>

      <main class="api-workbench-main">
        <div class="api-request-tabs">
          <div class="api-request-tab active">
            <span class="method" :class="apiRequestForm.method.toLowerCase()">{{ apiRequestForm.method }}</span>
            <span>{{ apiRequestForm.name || 'Untitled Request' }}</span>
            <button title="关闭当前请求" @click="newRequest()">×</button>
          </div>
          <button class="api-add-tab" title="新建请求" @click="newRequest(currentFolderId)">＋</button>
        </div>

        <div class="api-request-toolbar">
          <select v-model="apiRequestForm.method" class="api-method-select" aria-label="请求方法">
            <option>GET</option><option>POST</option><option>PUT</option><option>PATCH</option><option>DELETE</option><option>HEAD</option>
          </select>
          <input v-model="apiRequestForm.url" class="api-url-input" placeholder="Enter request URL" />
          <button class="btn primary send-btn" :disabled="!selectedApiCollectionId || isSending" @click="send">
            <span v-if="isSending" class="spinner"></span>{{ isSending ? '发送中' : '发送' }} <small>Ctrl↵</small>
          </button>
          <button class="btn" title="保存请求" @click="save">保存</button>
          <div class="api-more-menu">
            <button class="icon-btn" title="更多操作" @click="showRequestMenu = !showRequestMenu">⋯</button>
            <div v-if="showRequestMenu" class="api-more-popover">
              <button @click="duplicateRequest">复制请求</button>
              <button @click="exportCurl; showRequestMenu = false">复制为 cURL</button>
              <button class="danger-text" @click="removeRequest">删除请求</button>
            </div>
          </div>
        </div>

        <nav class="api-editor-tabs">
          <button :class="{ active: activeApiRequestTab === 'params' }" @click="setRequestTab('params')">Params <span>{{ apiRequestForm.paramsJson !== '{}' ? '1' : '0' }}</span></button>
          <button :class="{ active: activeApiRequestTab === 'authorization' }" @click="setRequestTab('authorization')">Authorization</button>
          <button :class="{ active: activeApiRequestTab === 'headers' }" @click="setRequestTab('headers')">Headers</button>
          <button :class="{ active: activeApiRequestTab === 'body' }" @click="setRequestTab('body')">Body</button>
          <button :class="{ active: activeApiRequestTab === 'assertions' }" @click="setRequestTab('assertions')">Tests</button>
          <button :class="{ active: activeApiRequestTab === 'pre-script' }" @click="setRequestTab('pre-script')">Pre-request</button>
          <button :class="{ active: activeApiRequestTab === 'post-script' }" @click="setRequestTab('post-script')">Post-request</button>
        </nav>

        <div class="api-editor-panel">
          <div v-if="activeApiRequestTab === 'params'" class="api-editor-content">
            <div class="api-panel-title"><strong>Query Params</strong><span>变量格式：&#123;&#123;base_url&#125;&#125;</span></div>
            <textarea v-model="apiRequestForm.paramsJson" class="api-code-editor" spellcheck="false" placeholder="{&#10;  &quot;page&quot;: &quot;1&quot;&#10;}"></textarea>
            <button class="btn" @click="apiRequestForm.paramsJson = normalizeJson(apiRequestForm.paramsJson, '{}')">格式化 JSON</button>
          </div>
          <div v-else-if="activeApiRequestTab === 'authorization'" class="api-editor-content">
            <div class="api-panel-title"><strong>Authorization</strong><span>发送时会自动写入 Headers</span></div>
            <div class="auth-grid">
              <label class="field"><span>类型</span><select v-model="authMode"><option value="none">No Auth</option><option value="bearer">Bearer Token</option><option value="basic">Basic Auth</option><option value="apikey">API Key</option></select></label>
              <label v-if="authMode === 'bearer'" class="field"><span>Token</span><input v-model="authToken" type="password" placeholder="token" /></label>
              <template v-if="authMode === 'basic'"><label class="field"><span>Username</span><input v-model="authUsername" /></label><label class="field"><span>Password</span><input v-model="authPassword" type="password" /></label></template>
              <template v-if="authMode === 'apikey'"><label class="field"><span>Key</span><input v-model="apiKeyName" /></label><label class="field"><span>Value</span><input v-model="apiKeyValue" type="password" /></label></template>
            </div>
            <button class="btn" @click="applyAuth">应用到 Headers</button>
          </div>
          <div v-else-if="activeApiRequestTab === 'headers'" class="api-editor-content">
            <div class="api-panel-title"><strong>Request Headers</strong><span>{{ apiRequestForm.headersJson.length }} chars</span></div>
            <textarea v-model="apiRequestForm.headersJson" class="api-code-editor" spellcheck="false" placeholder="{&#10;  &quot;Content-Type&quot;: &quot;application/json&quot;&#10;}"></textarea>
            <button class="btn" @click="apiRequestForm.headersJson = normalizeJson(apiRequestForm.headersJson, '{}')">格式化 JSON</button>
          </div>
          <div v-else-if="activeApiRequestTab === 'body'" class="api-editor-content">
            <div class="api-body-mode">
              <label><input v-model="bodyMode" type="radio" value="none" @change="syncBodyMode" /> none</label>
              <label><input v-model="bodyMode" type="radio" value="json" @change="syncBodyMode" /> raw JSON</label>
              <label><input v-model="bodyMode" type="radio" value="raw" /> raw</label>
              <label><input v-model="bodyMode" type="radio" value="form" /> form-data</label>
            </div>
            <textarea v-model="apiRequestForm.body" class="api-code-editor api-body-editor" spellcheck="false" :disabled="bodyMode === 'none'" placeholder="{&#10;  &quot;name&quot;: &quot;demo&quot;&#10;}"></textarea>
            <button class="btn" @click="apiRequestForm.body = normalizeJson(apiRequestForm.body, apiRequestForm.body)">格式化 JSON</button>
          </div>
          <div v-else-if="activeApiRequestTab === 'assertions'" class="api-editor-content">
            <div class="api-panel-title"><strong>Tests / Assertions</strong><span>status、json、field、contains、header</span></div>
            <textarea v-model="apiRequestForm.assertionsJson" class="api-code-editor" spellcheck="false" placeholder="[{ &quot;type&quot;: &quot;status&quot;, &quot;expected&quot;: &quot;200&quot;, &quot;enabled&quot;: true }]"></textarea>
            <button class="btn" @click="apiRequestForm.assertionsJson = normalizeJson(apiRequestForm.assertionsJson, '[]')">格式化 JSON</button>
          </div>
          <div v-else class="api-editor-content">
            <div class="api-panel-title"><strong>{{ activeApiRequestTab === 'pre-script' ? 'Pre-request Script' : 'Post-request Script' }}</strong><span>支持 set / extract</span></div>
            <textarea v-if="activeApiRequestTab === 'pre-script'" v-model="apiRequestForm.preRequestScript" class="api-code-editor script-editor" spellcheck="false" placeholder="set token=demo"></textarea>
            <textarea v-else v-model="apiRequestForm.postRequestScript" class="api-code-editor script-editor" spellcheck="false" placeholder="extract order_id=$.data.id"></textarea>
          </div>
        </div>

        <section class="api-response-panel">
          <div class="api-response-head">
            <div class="api-response-title"><strong>Response</strong><span v-if="apiResponse" class="status" :class="apiResponse.status === 'PASSED' ? 'passed' : 'failed'">{{ apiResponse.statusCode || 'ERR' }} · {{ apiResponse.durationMs || 0 }} ms</span></div>
            <span v-if="apiResponse" class="muted">{{ apiResponse.environment }}</span>
          </div>
          <div v-if="apiResponse" class="api-response-tabs">
            <button :class="{ active: responseTab === 'body' }" @click="responseTab = 'body'">Body</button>
            <button :class="{ active: responseTab === 'headers' }" @click="responseTab = 'headers'">Headers</button>
            <button :class="{ active: responseTab === 'tests' }" @click="responseTab = 'tests'">Test Results <span>{{ testResults.length }}</span></button>
            <button :class="{ active: responseTab === 'request' }" @click="responseTab = 'request'">Request</button>
          </div>
          <div v-if="apiResponse" class="api-response-content">
            <pre v-if="responseTab === 'body'">{{ formattedResponse || 'No response body' }}</pre>
            <pre v-else-if="responseTab === 'headers'">{{ responseHeaders }}</pre>
            <pre v-else-if="responseTab === 'request'">{{ pretty(apiResponse.requestSnapshotJson || '{}', '{}') }}</pre>
            <div v-else class="api-test-results">
              <div v-for="(result, index) in testResults" :key="`${result.type}-${index}`" class="api-test-row">
                <span :class="result.passed ? 'test-pass' : 'test-fail'">{{ result.passed ? '✓' : '×' }}</span>
                <strong>{{ result.type }}</strong><span>{{ result.path || result.expected || 'assertion' }}</span>
              </div>
              <span v-if="testResults.length === 0" class="muted">没有配置断言</span>
            </div>
          </div>
          <div v-else class="api-response-empty">
            <strong>响应将在这里显示</strong>
            <span>发送请求后查看状态码、响应体、Headers 和断言结果</span>
          </div>
        </section>
      </main>

      <aside class="api-inspector">
        <div class="api-inspector-tabs">
          <button :class="{ active: activeApiTab === 'collections' }" @click="activeApiTab = 'collections'">集合</button>
          <button :class="{ active: activeApiTab === 'variables' }" @click="activeApiTab = 'variables'; loadApiWorkbench()">环境</button>
          <button :class="{ active: activeApiTab === 'history' }" @click="activeApiTab = 'history'">历史</button>
        </div>
        <div v-if="activeApiTab === 'collections'" class="api-inspector-content">
          <div class="inspector-heading"><strong>{{ selectedCollection?.name || '当前集合' }}</strong><button class="link-btn danger-text" @click="removeCollection">删除</button></div>
          <p class="muted">集合执行会按请求顺序运行，失败后停止。</p>
          <button class="btn primary full-btn" :disabled="!selectedApiCollectionId" @click="executeApiCollection">运行集合</button>
          <div class="inspector-section"><span class="inspector-label">当前请求</span><strong>{{ apiRequests.length }} 个请求</strong></div>
        </div>
        <div v-else-if="activeApiTab === 'variables'" class="api-inspector-content">
          <form class="variable-form-compact" @submit.prevent="saveApiVariable">
            <input v-model="apiVariableForm.name" placeholder="变量名" />
            <input v-model="apiVariableForm.value" placeholder="变量值" />
            <input v-model="apiVariableForm.source" placeholder="来源" />
            <label><input v-model="apiVariableForm.sensitive" type="checkbox" />敏感变量</label>
            <button class="btn primary full-btn">保存变量</button>
          </form>
          <div class="api-variable-list">
            <div v-for="variable in apiVariables" :key="variable.id" class="api-variable-row"><strong>{{ variableToken(variable.name) }}</strong><span>{{ variable.value || '••••••••' }}</span></div>
            <span v-if="apiVariables.length === 0" class="muted">当前环境暂无变量</span>
          </div>
        </div>
        <div v-else class="api-inspector-content history-list">
          <div v-for="history in apiHistory" :key="history.id" class="history-row" @click="apiResponse = history">
            <span class="method" :class="(apiRequests.find((item) => item.id === history.requestId)?.method || 'GET').toLowerCase()">{{ apiRequests.find((item) => item.id === history.requestId)?.method || 'REQ' }}</span>
            <div><strong>{{ history.requestName }}</strong><span>{{ history.statusCode || 'ERR' }} · {{ history.environment }}</span></div>
            <span class="status" :class="history.status === 'PASSED' ? 'passed' : 'failed'">{{ history.status }}</span>
          </div>
          <span v-if="apiHistory.length === 0" class="muted">暂无执行历史</span>
        </div>
      </aside>
    </div>

    <div v-if="showImport" class="modal-backdrop" @click.self="showImport = false">
      <div class="modal api-import-modal">
        <div class="modal-head"><h3>导入 cURL</h3><button class="icon-btn" @click="showImport = false">×</button></div>
        <div class="modal-body"><textarea v-model="importText" rows="10" placeholder="curl --request GET 'https://api.example.com/users'&#10;  --header 'Authorization: Bearer {{token}}'"></textarea></div>
        <div class="modal-foot"><button class="btn" @click="showImport = false">取消</button><button class="btn primary" @click="parseCurl(importText)">导入请求</button></div>
      </div>
    </div>

    <div v-if="showNewRequest" class="modal-backdrop" @click.self="showNewRequest = false">
      <div class="modal api-import-modal">
        <div class="modal-head"><h3>新建用例集</h3><button class="icon-btn" @click="showNewRequest = false">×</button></div>
        <form class="modal-body" @submit.prevent="createApiCollection(); showNewRequest = false"><div class="field"><label>集合名称</label><input v-model="apiCollectionForm.name" autofocus placeholder="例如：订单 API" /></div><div class="field"><label>描述</label><input v-model="apiCollectionForm.description" placeholder="可选" /></div><div class="modal-foot"><button type="button" class="btn" @click="showNewRequest = false">取消</button><button class="btn primary" type="submit">创建集合</button></div></form>
      </div>
    </div>
  </section>
</template>
