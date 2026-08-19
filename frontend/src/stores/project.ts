import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/api'
import type { ApiProject, ApiProjectMember } from '@/api'
import type { Project } from '@/types'
import { useUiStore } from './ui'
import { useAuthStore } from './auth'
import router from '@/router'
import { storage } from '@/storage'

const emptyProject: Project = {
  name: '未选择项目',
  code: '--',
  owner: '--',
  members: 0,
  status: '测试中',
  description: '当前账号还没有可访问的项目。'
}

export const useProjectStore = defineStore('project', () => {
  const ui = useUiStore()
  const auth = useAuthStore()

  const projects = ref<Project[]>([])
  const selectedProjectId = ref<number | null>(storage.projectId)
  const projectMembers = ref<ApiProjectMember[]>([])
  const newProject = ref({ name: '', code: '', description: '' })
  const projectForm = ref({ name: '', description: '', status: 'RUNNING' })
  const memberForm = ref({ userId: null as number | null, role: 'TEST_DEVELOPER' })

  const currentProject = computed<Project>(() => {
    return projects.value.find((project) => project.id === selectedProjectId.value)
      ?? emptyProject
  })

  const canManageCurrentProject = computed(() => {
    const userId = storage.userId
    return auth.authState.role === 'ADMIN'
      || currentProject.value.ownerId === userId
      || projectMembers.value.some((member) => member.userId === userId && member.role === 'MANAGER')
  })

  const visibleProjects = computed(() => {
    const term = ui.search.trim().toLowerCase()
    if (!term) return projects.value
    return projects.value.filter((project) =>
      `${project.name}${project.code}${project.owner}`.toLowerCase().includes(term)
    )
  })

  function fromApiProject(project: ApiProject): Project {
    return {
      id: project.id,
      name: project.name,
      code: project.code,
      ownerId: project.ownerId,
      owner: project.ownerName,
      members: project.memberCount ?? 1,
      status: project.status === 'RUNNING' ? '运行中' : '测试中',
      description: project.description || '新建测试项目',
      createdAt: project.createdAt
    }
  }

  function syncProjectForm() {
    const project = currentProject.value
    if (!project) return
    projectForm.value.name = project.name
    projectForm.value.description = project.description
    projectForm.value.status = project.status === '运行中' ? 'RUNNING' : 'TESTING'
  }

  function openProject(project?: Project) {
    const target = project ?? currentProject.value
    if (!target.id) return
    selectedProjectId.value = target.id
    storage.projectId = target.id
    syncProjectForm()
    void router.push({ name: 'project', params: { id: String(target.id) } })
  }

  async function loadProjects() {
    const apiProjects = await api.projects.list()
    const nextProjects = apiProjects.map(fromApiProject)
    projects.value.splice(0, projects.value.length, ...nextProjects)
    const selected = nextProjects.find((project) => project.id === selectedProjectId.value)
      ?? nextProjects[0]
    selectedProjectId.value = selected?.id ?? null
    if (selected?.id) {
      storage.projectId = selected.id
    } else {
      storage.projectId = null
    }
    syncProjectForm()
  }

  async function loadProjectMembers() {
    const projectId = currentProject.value?.id
    if (!projectId) return
    const members = await api.projects.members(projectId)
    projectMembers.value.splice(0, projectMembers.value.length, ...members)
    const project = projects.value.find((item) => item.id === projectId)
    if (project) project.members = members.length
  }

  async function createProject() {
    if (!newProject.value.name.trim() || !newProject.value.code.trim()) {
      ui.showToast('请填写项目名称和项目标识')
      return
    }
    try {
      const created = await api.projects.create({
        name: newProject.value.name,
        code: newProject.value.code,
        description: newProject.value.description || '新建测试项目'
      })
      projects.value.unshift(fromApiProject(created))
      selectedProjectId.value = created.id
      storage.projectId = created.id
      newProject.value.name = ''
      newProject.value.code = ''
      newProject.value.description = ''
      ui.closeModal()
      ui.showToast('项目创建成功')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '项目创建失败')
    }
  }

  async function saveProjectSettings() {
    const projectId = currentProject.value?.id
    if (!projectId || !projectForm.value.name.trim()) {
      ui.showToast('请填写项目名称')
      return
    }
    try {
      const saved = await api.projects.update(projectId, projectForm.value)
      const index = projects.value.findIndex((item) => item.id === saved.id)
      if (index >= 0) projects.value[index] = fromApiProject(saved)
      syncProjectForm()
      ui.showToast('项目设置已保存')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '项目设置保存失败')
    }
  }

  async function deleteCurrentProject() {
    const projectId = currentProject.value?.id
    if (!projectId || !window.confirm(`确认删除项目“${currentProject.value.name}”吗？`)) return
    try {
      await api.projects.remove(projectId)
      await loadProjects()
      void router.push({ name: 'projects' })
      ui.showToast('项目已删除')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '项目删除失败')
    }
  }

  function openAddMember() {
    memberForm.value.userId = null
    memberForm.value.role = 'TEST_DEVELOPER'
    ui.openModal('member')
  }

  async function addProjectMember() {
    const projectId = currentProject.value?.id
    if (!projectId || !memberForm.value.userId) {
      ui.showToast('请选择项目成员')
      return
    }
    try {
      const saved = await api.projects.addMember(projectId, {
        userId: memberForm.value.userId,
        role: memberForm.value.role
      })
      projectMembers.value.push(saved)
      memberForm.value.userId = null
      ui.closeModal()
      ui.showToast('项目成员已添加')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '项目成员添加失败')
    }
  }

  async function removeProjectMember(member: ApiProjectMember) {
    const projectId = currentProject.value?.id
    if (!projectId || !window.confirm(`确认移除成员“${member.displayName}”吗？`)) return
    try {
      await api.projects.removeMember(projectId, member.userId)
      const index = projectMembers.value.findIndex((item) => item.userId === member.userId)
      if (index >= 0) projectMembers.value.splice(index, 1)
      ui.showToast('项目成员已移除')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '项目成员移除失败')
    }
  }

  async function updateProjectMemberRole(member: ApiProjectMember, event: Event) {
    const projectId = currentProject.value?.id
    const role = (event.target as HTMLSelectElement | null)?.value
    if (!projectId || !role || role === member.role) return
    try {
      const saved = await api.projects.updateMember(projectId, member.userId, { role })
      const index = projectMembers.value.findIndex((item) => item.userId === saved.userId)
      if (index >= 0) projectMembers.value[index] = saved
      ui.showToast('成员角色已更新')
    } catch (error) {
      await loadProjectMembers()
      ui.showToast(error instanceof Error ? error.message : '成员角色更新失败')
    }
  }

  return {
    projects,
    selectedProjectId,
    projectMembers,
    newProject,
    projectForm,
    memberForm,
    currentProject,
    canManageCurrentProject,
    visibleProjects,
    syncProjectForm,
    loadProjects,
    openProject,
    loadProjectMembers,
    createProject,
    saveProjectSettings,
    deleteCurrentProject,
    addProjectMember,
    removeProjectMember,
    updateProjectMemberRole,
    openAddMember
  }
})
