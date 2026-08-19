import { request } from './client'
import type { ApiUser } from './user'

export interface ApiProjectMember {
  membershipId?: number
  userId: number
  username: string
  displayName: string
  email?: string
  role: string
  joinedAt?: string
}

export interface ApiProject {
  id: number
  name: string
  code: string
  description: string
  ownerId: number
  ownerName: string
  status: string
  memberCount: number
  createdAt: string
  updatedAt: string
}

export const projects = {
  list: () => request<ApiProject[]>('/projects'),
  create: (payload: {
    name: string
    code: string
    description: string
  }) => request<ApiProject>('/projects', {
    method: 'POST',
    body: JSON.stringify(payload)
  }),
  update: (projectId: number, payload: {
    name: string
    description?: string
    status?: string
  }) => request<ApiProject>(`/projects/${projectId}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  }),
  remove: (projectId: number) => request<void>(`/projects/${projectId}`, {
    method: 'DELETE'
  }),
  members: (projectId: number) => request<ApiProjectMember[]>(`/projects/${projectId}/members`),
  memberCandidates: (projectId: number) => request<ApiUser[]>(`/projects/${projectId}/members/candidates`),
  addMember: (projectId: number, payload: { userId: number; role: string }) =>
    request<ApiProjectMember>(`/projects/${projectId}/members`, {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  updateMember: (projectId: number, userId: number, payload: { role: string }) =>
    request<ApiProjectMember>(`/projects/${projectId}/members/${userId}`, {
      method: 'PUT',
      body: JSON.stringify({ userId, ...payload })
    }),
  removeMember: (projectId: number, userId: number) =>
    request<void>(`/projects/${projectId}/members/${userId}`, { method: 'DELETE' })
}
