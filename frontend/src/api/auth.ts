import { request } from './client'
import type { ApiUser } from './user'

interface LoginResponse {
  accessToken: string
  user: ApiUser
}

export const auth = {
  login: (username: string, password: string) => request<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password })
  }),
  me: () => request<ApiUser>('/auth/me'),
  logout: () => request<void>('/auth/logout', { method: 'POST' })
}
