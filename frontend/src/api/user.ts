import { request } from './client'

export interface ApiUser {
  id: number
  username: string
  displayName: string
  email: string
  role: string
  status?: string
  lastLoginAt?: string
  createdAt?: string
  updatedAt?: string
}

export const users = {
  list: () => request<ApiUser[]>('/users'),
  create: (payload: {
    username: string
    password: string
    displayName: string
    email?: string
    role: string
  }) => request<ApiUser>('/users', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  update: (userId: number, payload: {
    displayName: string
    email?: string
    role: string
  }) => request<ApiUser>(`/users/${userId}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  }),
  changeStatus: (userId: number, status: string) =>
    request<ApiUser>(`/users/${userId}/status`, {
      method: 'POST',
      body: JSON.stringify({ status })
    }),
  resetPassword: (userId: number, password: string) =>
    request<void>(`/users/${userId}/password`, {
      method: 'POST',
      body: JSON.stringify({ password })
    })
}
