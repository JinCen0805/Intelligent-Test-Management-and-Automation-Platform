import { ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/api'
import type { ApiUser, ApiJenkinsInstance, ApiCredentialGrant } from '@/api'
import { useUiStore } from './ui'
import { useAuthStore } from './auth'
import { useProjectStore } from './project'
import { useAutomationStore } from './automation'
import { storage } from '@/storage'

export const useAdminStore = defineStore('admin', () => {
  const ui = useUiStore()
  const auth = useAuthStore()
  const projectStore = useProjectStore()

  const users = ref<ApiUser[]>([])
  const memberCandidates = ref<ApiUser[]>([])
  const editingUserId = ref<number | null>(null)
  const userForm = ref({
    username: '',
    password: '',
    displayName: '',
    email: '',
    role: 'TEST_DEVELOPER'
  })
  const credentialGrantForm = ref({ credentialRef: '', credentialScopeRef: '' })
  const credentialGrants = ref<ApiCredentialGrant[]>([])
  const jenkinsInstances = ref<ApiJenkinsInstance[]>([])
  const jenkinsInstanceForm = ref({
    name: '',
    baseUrl: '',
    username: '',
    apiToken: '',
    connectTimeoutMs: 5000,
    readTimeoutMs: 15000,
    pollIntervalMs: 10000,
    maxWaitMinutes: 30
  })
  const editingJenkinsInstanceId = ref<number | null>(null)
  const testingJenkinsInstanceId = ref<number | null>(null)

  async function loadUsers() {
    if (auth.authState.role !== 'ADMIN') return
    try {
      users.value.splice(0, users.value.length, ...(await api.users.list()))
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '用户列表加载失败')
    }
  }

  async function loadMemberCandidates() {
    const projectId = projectStore.currentProject.id
    if (!projectId) return
    try {
      memberCandidates.value.splice(0, memberCandidates.value.length, ...(await api.projects.memberCandidates(projectId)))
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '候选用户列表加载失败')
    }
  }

  async function loadCredentialGrants() {
    const projectId = projectStore.currentProject.id
    if (!projectId) return
    try {
      credentialGrants.value.splice(0, credentialGrants.value.length, ...(await api.jenkins.credentialGrants(projectId)))
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '凭证授权列表加载失败')
    }
  }

  function openNewUser() {
    editingUserId.value = null
    Object.assign(userForm.value, {
      username: '',
      password: '',
      displayName: '',
      email: '',
      role: 'TEST_DEVELOPER'
    })
    ui.openModal('user')
  }

  function editUser(user: ApiUser) {
    editingUserId.value = user.id
    Object.assign(userForm.value, {
      username: user.username,
      password: '',
      displayName: user.displayName,
      email: user.email || '',
      role: user.role
    })
    ui.openModal('user')
  }

  let savingUser = false

  async function saveUser() {
    if (auth.authState.role !== 'ADMIN' || !userForm.value.username.trim() || !userForm.value.displayName.trim()) {
      ui.showToast('请填写用户名和用户显示名称')
      return
    }
    if (savingUser) return
    savingUser = true
    try {
      if (editingUserId.value) {
        const saved = await api.users.update(editingUserId.value, {
          username: userForm.value.username,
          displayName: userForm.value.displayName,
          email: userForm.value.email,
          role: userForm.value.role
        })
        const index = users.value.findIndex((item) => item.id === saved.id)
        if (index >= 0) users.value[index] = saved
        if (userForm.value.password.trim()) {
          await api.users.resetPassword(saved.id, userForm.value.password)
        }
        ui.showToast('用户信息已更新')
      } else {
        if (!userForm.value.password.trim()) {
          ui.showToast('新建用户需要填写用户名和初始密码')
          return
        }
        const saved = await api.users.create(userForm.value)
        users.value.unshift(saved)
        ui.showToast('用户已创建')
      }
      ui.closeModal()
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '用户保存失败')
    } finally {
      savingUser = false
    }
  }

  async function toggleUserStatus(user: ApiUser) {
    if (user.id === storage.userId) return
    try {
      const saved = await api.users.changeStatus(user.id, user.status === 'DISABLED' ? 'ENABLED' : 'DISABLED')
      const index = users.value.findIndex((item) => item.id === saved.id)
      if (index >= 0) users.value[index] = saved
      ui.showToast(saved.status === 'DISABLED' ? '用户已禁用' : '用户已启用')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '用户状态更新失败')
    }
  }

  async function createCredentialGrant() {
    const projectId = projectStore.currentProject.id
    if (!projectId || !credentialGrantForm.value.credentialRef.trim()) {
      ui.showToast('请输入 Jenkins Credential ID')
      return
    }
    if (auth.authState.role !== 'ADMIN') {
      ui.showToast('只有管理员可以授权凭证')
      return
    }
    try {
      const saved = await api.jenkins.createCredentialGrant(projectId, {
        credentialRef: credentialGrantForm.value.credentialRef.trim(),
        credentialScopeRef: credentialGrantForm.value.credentialScopeRef.trim() || undefined
      })
      credentialGrants.value.push(saved)
      credentialGrantForm.value.credentialRef = ''
      credentialGrantForm.value.credentialScopeRef = ''
      ui.showToast('凭证已授权给当前项目')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '凭证授权失败')
    }
  }

  async function revokeCredentialGrant(grant: ApiCredentialGrant) {
    const projectId = projectStore.currentProject.id
    if (!projectId || auth.authState.role !== 'ADMIN') return
    if (!window.confirm(`确认撤销凭证“${grant.credentialRef}”的项目授权吗？`)) return
    try {
      await api.jenkins.revokeCredentialGrant(projectId, grant.id)
      const index = credentialGrants.value.findIndex((item) => item.id === grant.id)
      if (index >= 0) credentialGrants.value.splice(index, 1)
      const automation = useAutomationStore()
      if (automation.automationProjectForm.credentialRef === grant.credentialRef) {
        automation.automationProjectForm.credentialRef = ''
      }
      ui.showToast('凭证授权已撤销')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '凭证撤销失败')
    }
  }

  async function loadJenkinsInstances() {
    try {
      jenkinsInstances.value.splice(0, jenkinsInstances.value.length, ...(await api.jenkins.instances()))
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : 'Jenkins 实例列表加载失败')
    }
  }

  function openNewJenkinsInstance() {
    editingJenkinsInstanceId.value = null
    Object.assign(jenkinsInstanceForm.value, {
      name: '',
      baseUrl: '',
      username: '',
      apiToken: '',
      connectTimeoutMs: 5000,
      readTimeoutMs: 15000,
      pollIntervalMs: 10000,
      maxWaitMinutes: 30
    })
    ui.openModal('jenkinsInstance')
  }

  function editJenkinsInstance(instance: ApiJenkinsInstance) {
    editingJenkinsInstanceId.value = instance.id
    Object.assign(jenkinsInstanceForm.value, {
      name: instance.name,
      baseUrl: instance.baseUrl,
      username: instance.username,
      apiToken: '',
      connectTimeoutMs: instance.connectTimeoutMs,
      readTimeoutMs: instance.readTimeoutMs,
      pollIntervalMs: instance.pollIntervalMs,
      maxWaitMinutes: instance.maxWaitMinutes
    })
    ui.openModal('jenkinsInstance')
  }

  async function saveJenkinsInstance() {
    if (auth.authState.role !== 'ADMIN') return
    if (!jenkinsInstanceForm.value.name.trim() || !jenkinsInstanceForm.value.baseUrl.trim() || !jenkinsInstanceForm.value.username.trim()) {
      ui.showToast('请填写实例名称、地址和服务账号')
      return
    }
    if (!editingJenkinsInstanceId.value && !jenkinsInstanceForm.value.apiToken.trim()) {
      ui.showToast('新建实例需要填写 API Token')
      return
    }
    try {
      const payload = {
        ...jenkinsInstanceForm.value,
        apiToken: jenkinsInstanceForm.value.apiToken.trim() || undefined
      }
      const saved = editingJenkinsInstanceId.value
        ? await api.jenkins.updateInstance(editingJenkinsInstanceId.value, payload)
        : await api.jenkins.createInstance(payload)
      const index = jenkinsInstances.value.findIndex((item) => item.id === saved.id)
      if (index >= 0) jenkinsInstances.value[index] = saved
      else jenkinsInstances.value.unshift(saved)
      ui.closeModal()
      ui.showToast('Jenkins 实例已保存')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : 'Jenkins 实例保存失败')
    }
  }

  async function testJenkinsConnection(instance: ApiJenkinsInstance) {
    testingJenkinsInstanceId.value = instance.id
    try {
      const saved = await api.jenkins.testConnection(instance.id)
      const index = jenkinsInstances.value.findIndex((item) => item.id === saved.id)
      if (index >= 0) jenkinsInstances.value[index] = saved
      ui.showToast(saved.connectionStatus === 'CONNECTED' ? '连接成功' : `连接失败：${saved.lastError || '未知错误'}`)
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '连接测试失败')
    } finally {
      testingJenkinsInstanceId.value = null
    }
  }

  async function toggleJenkinsInstanceStatus(instance: ApiJenkinsInstance) {
    const next = instance.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
    try {
      const saved = await api.jenkins.changeStatus(instance.id, next)
      const index = jenkinsInstances.value.findIndex((item) => item.id === saved.id)
      if (index >= 0) jenkinsInstances.value[index] = saved
      ui.showToast(saved.status === 'ENABLED' ? '实例已启用' : '实例已禁用')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '实例状态更新失败')
    }
  }

  async function deleteJenkinsInstance(instance: ApiJenkinsInstance) {
    if (!window.confirm(`确认删除 Jenkins 实例“${instance.name}”吗？删除后该实例将被禁用。`)) return
    try {
      await api.jenkins.deleteInstance(instance.id)
      await loadJenkinsInstances()
      ui.showToast('Jenkins 实例已删除（禁用）')
    } catch (error) {
      ui.showToast(error instanceof Error ? error.message : '实例删除失败')
    }
  }

  return {
    users,
    memberCandidates,
    editingUserId,
    userForm,
    credentialGrantForm,
    credentialGrants,
    jenkinsInstances,
    jenkinsInstanceForm,
    editingJenkinsInstanceId,
    testingJenkinsInstanceId,
    loadUsers,
    loadMemberCandidates,
    loadCredentialGrants,
    openNewUser,
    editUser,
    saveUser,
    toggleUserStatus,
    createCredentialGrant,
    revokeCredentialGrant,
    loadJenkinsInstances,
    openNewJenkinsInstance,
    editJenkinsInstance,
    saveJenkinsInstance,
    testJenkinsConnection,
    toggleJenkinsInstanceStatus,
    deleteJenkinsInstance
  }
})
