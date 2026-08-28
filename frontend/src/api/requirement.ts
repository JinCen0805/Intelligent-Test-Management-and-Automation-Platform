import { request, requestFormData } from './client'

export interface ApiRequirement {
  id: number
  projectId: number
  name: string
  fileType: string
  version: string
  parseStatus: string
  contentLength: number
  uploadedBy: number
  createdAt: string
  updatedAt: string
}

export const requirements = {
  list: (projectId: number) => request<ApiRequirement[]>(`/projects/${projectId}/files`),
  files: (projectId: number) => request<ApiRequirement[]>(`/projects/${projectId}/files`),
  upload: async (projectId: number, file: File, version: string) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('version', version)
    return requestFormData<ApiRequirement>(`/projects/${projectId}/files/upload`, formData)
  },
  parse: (projectId: number, requirementId: number) => request<ApiRequirement>(
    `/projects/${projectId}/files/${requirementId}/parse`,
    { method: 'POST' }
  ),
  delete: (projectId: number, requirementId: number) => request<void>(
    `/projects/${projectId}/files/${requirementId}`,
    { method: 'DELETE' }
  ),
  detail: (projectId: number, requirementId: number) => request<{
    id: number
    projectId: number
    name: string
    fileType: string
    version: string
    parseStatus: string
    parseError?: string
    contentText?: string
    uploadedBy: number
    createdAt: string
    updatedAt: string
  }>(`/projects/${projectId}/files/${requirementId}`)
}
