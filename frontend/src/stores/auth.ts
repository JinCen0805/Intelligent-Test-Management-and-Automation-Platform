import { ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/api'
import type { ApiUser } from '@/api'
import { useProjectStore } from './project'
import { useRequirementStore } from './requirement'
import { useTestcaseStore } from './testcase'
import { useAutomationStore } from './automation'
import { useReportStore } from './report'
import { useAdminStore } from './admin'
import router from '@/router'
import { storage } from '@/storage'

export const useAuthStore = defineStore('auth', () => {
  const authState = ref({
    ready: false,
    authenticated: false,
    loading: false,
    error: '',
    username: '',
    password: '',
    displayName: '林晓',
    role: 'TEST_DEVELOPER'
  })

  function applyUser(user: ApiUser) {
    storage.userId = user.id
    authState.value.displayName = user.displayName
    authState.value.role = user.role
  }

  function isCurrentUser(user: ApiUser) {
    return user.id === storage.userId
  }

  async function bootstrapAuth() {
    const token = storage.token
    if (!token) {
      authState.value.ready = true
      return
    }
    try {
      const user = await api.auth.me()
      applyUser(user)
      authState.value.authenticated = true
      await hydrateSession()
    } catch {
      storage.token = null
    } finally {
      authState.value.ready = true
    }
  }

  async function hydrateSession() {
    const project = useProjectStore()
    await project.loadProjects()
    await Promise.allSettled([
      useRequirementStore().loadRequirements(),
      useTestcaseStore().loadTestCases(),
      useAutomationStore().loadAutomationData(),
      project.loadProjectMembers(),
      useAdminStore().loadCredentialGrants(),
      useReportStore().loadReportCenter(),
      useAdminStore().loadUsers()
    ])
  }

  async function login() {
    authState.value.loading = true
    authState.value.error = ''
    try {
      const result = await api.auth.login(authState.value.username.trim(), authState.value.password)
      storage.token = result.accessToken
      applyUser(result.user)
      authState.value.authenticated = true
      void router.push({ name: 'home' })
      await hydrateSession()
    } catch (error) {
      authState.value.error = error instanceof Error ? error.message : '登录失败，请稍后重试'
    } finally {
      authState.value.loading = false
      authState.value.ready = true
    }
  }

  async function logout() {
    try {
      await api.auth.logout()
    } catch {
      // Token cleanup still happens when the backend is unavailable.
    }
    storage.token = null
    storage.userId = null
    authState.value.authenticated = false
    authState.value.error = ''
  }

  return { authState, applyUser, isCurrentUser, bootstrapAuth, hydrateSession, login, logout }
})
