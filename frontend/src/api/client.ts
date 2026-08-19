import { storage } from '@/storage'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

const apiBase = import.meta.env.VITE_API_BASE_URL ?? '/api'

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = storage.token
  const headers = new Headers(init.headers)
  headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(`${apiBase}${path}`, { ...init, headers })
  const payload = await response.json() as ApiResponse<T>
  if (!response.ok || payload.code !== 0) {
    throw new Error(payload.message || `请求失败 (${response.status})`)
  }
  return payload.data
}

export async function requestFormData<T>(path: string, formData: FormData): Promise<T> {
  const token = storage.token
  const headers = new Headers()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  const response = await fetch(`${apiBase}${path}`, {
    method: 'POST',
    headers,
    body: formData
  })
  const payload = await response.json() as ApiResponse<T>
  if (!response.ok || payload.code !== 0) {
    throw new Error(payload.message || `上传失败 (${response.status})`)
  }
  return payload.data
}
