import { describe, it, expect, beforeEach, vi } from 'vitest'
import { request, UNAUTHORIZED_EVENT } from './client'
import { storage } from '@/storage'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' }
  })
}

describe('client 401 处理', () => {
  beforeEach(() => {
    storage.token = null
    storage.userId = null
    vi.restoreAllMocks()
  })

  it('登录失败（无 token）不派发 unauthorized 事件，错误交给调用方显示', async () => {
    const listener = vi.fn()
    window.addEventListener(UNAUTHORIZED_EVENT, listener)
    vi.stubGlobal('fetch', vi.fn(async () => jsonResponse(401, {
      code: 40101,
      message: '用户名或密码错误',
      data: null
    })))

    await expect(request('/auth/login', { method: 'POST' })).rejects.toThrow('用户名或密码错误')
    expect(listener).not.toHaveBeenCalled()
    window.removeEventListener(UNAUTHORIZED_EVENT, listener)
  })

  it('携带 token 的 401 会清空 token 并派发 unauthorized 事件', async () => {
    storage.token = 'expired-token'
    const listener = vi.fn()
    window.addEventListener(UNAUTHORIZED_EVENT, listener)
    vi.stubGlobal('fetch', vi.fn(async () => jsonResponse(401, {
      code: 40100,
      message: '登录已失效',
      data: null
    })))

    await expect(request('/projects')).rejects.toThrow('登录已失效')
    expect(storage.token).toBeNull()
    expect(listener).toHaveBeenCalledTimes(1)
    window.removeEventListener(UNAUTHORIZED_EVENT, listener)
  })
})
