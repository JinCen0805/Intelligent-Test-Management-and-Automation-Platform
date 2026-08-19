import { reactive, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { api } from '@/api'
import type {
  ApiCollection,
  ApiWorkbenchRequest,
  ApiEnvironmentVariable,
  ApiRequestHistory
} from '@/api'
import { useProjectStore } from '@/stores/project'
import { useUiStore } from '@/stores/ui'

/**
 * 接口测试工作台：请求 / 断言 / 环境变量 / 执行历史，全部由后端持久化。
 * 仅在项目详情页的「接口测试工作台」Tab 内使用。
 */
export function useApiWorkbench() {
  const projectStore = useProjectStore()
  const ui = useUiStore()
  const { currentProject } = storeToRefs(projectStore)

  const apiCollections = reactive<ApiCollection[]>([])
  const apiRequests = reactive<ApiWorkbenchRequest[]>([])
  const apiVariables = reactive<ApiEnvironmentVariable[]>([])
  const apiHistory = reactive<ApiRequestHistory[]>([])
  const selectedApiCollectionId = ref<number | null>(null)
  const selectedApiRequestId = ref<number | null>(null)
  const apiResponse = ref<ApiRequestHistory | null>(null)
  const apiCollectionForm = reactive({ name: '' })
  const apiVariableForm = reactive({
    environment: 'TEST',
    name: '',
    value: '',
    sensitive: false,
    source: '环境配置'
  })
  const apiRequestForm = reactive({
    name: '创建订单',
    method: 'POST',
    url: '{{base_url}}/orders',
    paramsJson: '{\n  "userId": "{{user_id}}",\n  "skuId": "{{sku_id}}"\n}',
    headersJson: '{\n  "Authorization": "Bearer {{token}}",\n  "Content-Type": "application/json"\n}',
    body: '{\n  "userId": "{{user_id}}",\n  "skuId": "{{sku_id}}",\n  "couponCode": "NEW_USER_20"\n}',
    assertionsJson: '[\n  { "type": "status", "expected": "200", "enabled": true },\n  { "type": "json", "path": "$.data.orderStatus", "expected": "CREATED", "enabled": true }\n]',
    preRequestScript: '',
    postRequestScript: ''
  })

  async function loadApiWorkbench() {
    const projectId = currentProject.value?.id
    if (!projectId) return
    try {
      const [collections, variables, history] = await Promise.all([
        api.apiWorkbench.collections(projectId),
        api.apiWorkbench.variables(projectId, apiVariableForm.environment),
        api.apiWorkbench.history(projectId)
      ])
      apiCollections.splice(0, apiCollections.length, ...collections)
      apiVariables.splice(0, apiVariables.length, ...variables)
      const historyContent = Array.isArray(history)
        ? history
        : (history?.content ?? [])
      apiHistory.splice(0, apiHistory.length, ...historyContent)
      selectedApiCollectionId.value = collections.find((item) => item.id === selectedApiCollectionId.value)?.id
        ?? collections[0]?.id
        ?? null
      if (selectedApiCollectionId.value) {
        await loadApiRequests(selectedApiCollectionId.value)
      } else {
        apiRequests.splice(0, apiRequests.length)
        selectedApiRequestId.value = null
      }
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '接口工作台加载失败')
    }
  }

  async function loadApiRequests(collectionId: number) {
    const projectId = currentProject.value?.id
    if (!projectId) return
    const requests = await api.apiWorkbench.requests(projectId, collectionId)
    apiRequests.splice(0, apiRequests.length, ...requests)
    const selected = requests.find((item) => item.id === selectedApiRequestId.value) ?? requests[0]
    selectedApiRequestId.value = selected?.id ?? null
    if (selected) applyApiRequest(selected)
  }

  function applyApiRequest(request: ApiWorkbenchRequest) {
    selectedApiRequestId.value = request.id
    apiRequestForm.name = request.name
    apiRequestForm.method = request.method
    apiRequestForm.url = request.url
    apiRequestForm.paramsJson = request.paramsJson || '{}'
    apiRequestForm.headersJson = request.headersJson || '{}'
    apiRequestForm.body = request.body || ''
    apiRequestForm.assertionsJson = request.assertionsJson || '[]'
    apiRequestForm.preRequestScript = request.preRequestScript || ''
    apiRequestForm.postRequestScript = request.postRequestScript || ''
    apiResponse.value = apiHistory.find((item) => item.requestId === request.id) || null
  }

  async function createApiCollection() {
    const projectId = currentProject.value?.id
    if (!projectId || !apiCollectionForm.name.trim()) {
      ui.showToast('请输入集合名称')
      return
    }
    try {
      const collection = await api.apiWorkbench.createCollection(projectId, apiCollectionForm)
      apiCollections.push(collection)
      apiCollectionForm.name = ''
      selectedApiCollectionId.value = collection.id
      apiRequests.splice(0, apiRequests.length)
      selectedApiRequestId.value = null
      ui.showToast('接口集合已创建')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '集合创建失败')
    }
  }

  async function saveApiRequest() {
    const projectId = currentProject.value?.id
    const collectionId = selectedApiCollectionId.value
    if (!projectId || !collectionId || !apiRequestForm.name.trim()) {
      ui.showToast('请选择集合并填写请求名称')
      return
    }
    try {
      const payload = { collectionId, ...apiRequestForm }
      const saved = selectedApiRequestId.value
        ? await api.apiWorkbench.updateRequest(projectId, selectedApiRequestId.value, payload)
        : await api.apiWorkbench.createRequest(projectId, payload)
      const index = apiRequests.findIndex((item) => item.id === saved.id)
      if (index >= 0) apiRequests[index] = saved
      else apiRequests.push(saved)
      applyApiRequest(saved)
      ui.showToast('请求已保存')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '请求保存失败')
    }
  }

  async function sendApiRequest() {
    const projectId = currentProject.value?.id
    const collectionId = selectedApiCollectionId.value
    if (!projectId || !collectionId) {
      ui.showToast('请先创建并选择接口集合')
      return
    }
    try {
      if (!selectedApiRequestId.value) {
        await saveApiRequest()
      }
      if (!selectedApiRequestId.value) return
      const result = await api.apiWorkbench.execute(projectId, selectedApiRequestId.value, apiVariableForm.environment)
      apiResponse.value = result
      apiHistory.unshift(result)
      ui.showToast(result.status === 'PASSED' ? '请求执行成功，断言通过' : '请求已执行，请检查响应和断言')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '接口执行失败')
    }
  }

  async function executeApiCollection() {
    const projectId = currentProject.value?.id
    const collectionId = selectedApiCollectionId.value
    if (!projectId || !collectionId) {
      ui.showToast('请先选择接口集合')
      return
    }
    try {
      const result = await api.apiWorkbench.executeCollection(
        projectId,
        collectionId,
        apiVariableForm.environment,
        true
      )
      apiHistory.unshift(...result.results)
      apiResponse.value = result.results.at(-1) || null
      ui.showToast(result.status === 'PASSED' ? '集合执行完成' : '集合执行已停止，请检查失败请求')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '集合执行失败')
    }
  }

  async function saveApiVariable() {
    const projectId = currentProject.value?.id
    if (!projectId || !apiVariableForm.name.trim()) {
      ui.showToast('请输入变量名')
      return
    }
    try {
      const saved = await api.apiWorkbench.saveVariable(projectId, apiVariableForm)
      const index = apiVariables.findIndex((item) => item.id === saved.id)
      if (index >= 0) apiVariables[index] = saved
      else apiVariables.push(saved)
      apiVariableForm.name = ''
      apiVariableForm.value = ''
      ui.showToast('环境变量已保存')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '环境变量保存失败')
    }
  }

  function variableToken(name: string) {
    return `{{${name}}}`
  }

  return {
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
  }
}
