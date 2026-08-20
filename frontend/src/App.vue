<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useAuthStore } from '@/stores/auth'
import { useProjectStore } from '@/stores/project'
import ModalHost from '@/components/ModalHost.vue'

const route = useRoute()
const router = useRouter()
const ui = useUiStore()
const auth = useAuthStore()
const projectStore = useProjectStore()

const { toast, processing, search } = storeToRefs(ui)
const { authState } = storeToRefs(auth)
const { login, logout, bootstrapAuth } = auth
const { currentProject } = storeToRefs(projectStore)

const navItems: { key: string; label: string; icon: string; badge?: string }[] = [
  { key: 'home', label: '首页', icon: '⌂' },
  { key: 'projects', label: '项目', icon: '▣' },
  { key: 'ai', label: 'AI 能力', icon: '✦' },
  { key: 'reports', label: '测试报告', icon: '▤' },
  { key: 'admin', label: '系统管理', icon: '⚙' }
]

const activeNavKey = computed(() => route.name === 'project' ? 'projects' : (typeof route.name === 'string' ? route.name : ''))

const activeViewName = computed(() => {
  if (route.name === 'project') return currentProject.value.name
  return route.meta.title ?? ''
})

function navigate(view: string) {
  void router.push({ name: view })
}

onMounted(() => { void bootstrapAuth() })
</script>

<template>
  <div v-if="!authState.ready" class="app-loading">正在连接 TestPilot 服务...</div>

  <div v-else-if="!authState.authenticated" class="auth-screen">
    <div class="auth-panel">
      <div class="brand-mark auth-brand"><span class="brand-dot">T</span> TestPilot AI</div>
      <p class="auth-kicker">智能测试管理与自动化平台</p>
      <h1>登录工作台</h1>
      <p class="auth-copy">登录后管理项目、需求、测试用例和自动化执行。</p>
      <form class="auth-form" @submit.prevent="login">
        <div class="field"><label for="login-username">用户名</label><input id="login-username" v-model="authState.username" autocomplete="username" /></div>
        <div class="field"><label for="login-password">密码</label><input id="login-password" v-model="authState.password" type="password" autocomplete="current-password" /></div>
        <p v-if="authState.error" class="auth-error">{{ authState.error }}</p>
        <button class="btn primary auth-submit" :disabled="authState.loading">{{ authState.loading ? '登录中...' : '登录' }}</button>
      </form>
    </div>
  </div>

  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark"><span class="brand-dot">T</span> TestPilot AI</div>
        <small>智能测试管理与自动化平台</small>
      </div>
      <div class="side-label">工作台</div>
      <nav class="nav">
        <button v-for="item in navItems" :key="item.key" :class="{ active: activeNavKey === item.key }" @click="navigate(item.key)">
          <span class="nav-icon">{{ item.icon }}</span><span class="nav-text">{{ item.label }}</span><span v-if="item.badge" class="nav-badge">{{ item.badge }}</span>
        </button>
      </nav>
      <div class="side-label side-label-secondary">系统连接</div>
      <div class="side-status"><small>平台运行状态</small><strong><i></i> 正常运行</strong></div>
      <div class="sidebar-footer">
        <div class="account">
          <div class="avatar">{{ authState.displayName.slice(0, 1) }}</div>
          <div class="account-info"><strong>{{ authState.displayName }}</strong><span>{{ authState.role === 'ADMIN' ? '管理员' : '测试开发' }}</span></div>
          <button class="account-more" title="退出登录" aria-label="退出登录" @click="logout">↪</button>
        </div>
      </div>
    </aside>

    <main class="main">
      <header class="topbar">
        <div class="crumb"><span>TestPilot AI</span><b>/</b><strong>{{ activeViewName }}</strong></div>
        <div class="top-actions">
          <label v-if="route.name === 'projects' || route.name === 'project'" class="search"><span>⌕</span><input v-model="search" placeholder="搜索项目或用例" /></label>
          <span class="top-status"><i></i> DEV 环境正常</span>
          <span class="top-date">{{ new Date().toLocaleDateString('zh-CN') }}</span>
        </div>
      </header>

      <div class="content">
        <router-view />
      </div>
    </main>

    <div v-if="toast" class="toast">{{ toast }}</div>
    <div v-if="processing" class="processing-banner"><span class="spinner"></span><div><span>{{ processing.message }}</span><small>{{ processing.hint }}</small></div></div>
    <ModalHost />
  </div>
</template>
