import { storage } from '@/storage'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

const apiBase = import.meta.env.VITE_API_BASE_URL ?? '/api'
const REQUEST_TIMEOUT_MS = 30_000

async function fetchWithTimeout(url: string, init: RequestInit): Promise<Response> {
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS)
  if (init.signal) {
    if (init.signal.aborted) controller.abort()
    else init.signal.addEventListener('abort', () => controller.abort(), { once: true })
  }
  try {
    return await fetch(url, { ...init, signal: controller.signal })
  } finally {
    window.clearTimeout(timeout)
  }
}

async function parseJson<T>(response: Response): Promise<ApiResponse<T> | null> {
  if (response.status === 204 || response.headers.get('content-length') === '0') return null
  const contentType = response.headers.get('content-type') || ''
  if (!contentType.includes('json')) return null
  try {
    return await response.json() as ApiResponse<T>
  } catch {
    return null
  }
}

export const UNAUTHORIZED_EVENT = 'testpilot:unauthorized'

function handleUnauthorized(response: Response) {
  // 登录接口失败时没有 token，不能触发跳转，否则登录错误提示会被整页刷新吞掉。
  if (response.status !== 401 || !storage.token) return
  storage.token = null
  storage.userId = null
  // Login is rendered by App.vue rather than a dedicated route; App.vue listens for
  // this event and switches the shell back to the login view without a full reload.
  window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT))
}

function networkError(error: unknown): Error {
  if (error instanceof DOMException && error.name === 'AbortError') return new Error('请求超时，请稍后重试')
  return error instanceof Error ? error : new Error('网络请求失败，请检查服务连接')
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = storage.token
  const headers = new Headers(init.headers)
  headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  let response: Response
  try {
    response = await fetchWithTimeout(`${apiBase}${path}`, { ...init, headers })
  } catch (error) {
    throw networkError(error)
  }
  handleUnauthorized(response)
  const payload = await parseJson<T>(response)
  if (!response.ok || !payload || payload.code !== 0) {
    throw new Error(payload?.message || `请求失败 (${response.status})`)
  }
  return payload?.data as T
}

export async function download(path: string, init: RequestInit = {}): Promise<Blob> {
  const token = storage.token
  const headers = new Headers(init.headers)
  if (token) headers.set('Authorization', `Bearer ${token}`)
  let response: Response
  try {
    response = await fetchWithTimeout(`${apiBase}${path}`, { ...init, headers })
  } catch (error) {
    throw networkError(error)
  }
  handleUnauthorized(response)
  if (!response.ok) {
    let message = `请求失败 (${response.status})`
    try {
      const payload = await response.json() as ApiResponse<unknown>
      message = payload.message || message
    } catch {
    }
    throw new Error(message)
  }
  return response.blob()
}

export async function requestFormData<T>(path: string, formData: FormData): Promise<T> {
  const token = storage.token
  const headers = new Headers()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  let response: Response
  try {
    response = await fetchWithTimeout(`${apiBase}${path}`, {
      method: 'POST',
      headers,
      body: formData
    })
  } catch (error) {
    throw networkError(error)
  }
  handleUnauthorized(response)
  const payload = await parseJson<T>(response)
  if (!response.ok || !payload || payload.code !== 0) {
    throw new Error(payload?.message || `上传失败 (${response.status})`)
  }
  return payload?.data as T
}
