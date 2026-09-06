<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useProjectStore } from '@/stores/project'
import { useAdminStore } from '@/stores/admin'

const auth = useAuthStore()
const projectStore = useProjectStore()
const adminStore = useAdminStore()

const { authState } = storeToRefs(auth)
const { currentProject } = storeToRefs(projectStore)
const {
  users,
  credentialGrantForm,
  credentialGrants,
  jenkinsInstances,
  testingJenkinsInstanceId
} = storeToRefs(adminStore)
const { isCurrentUser } = auth
const {
  loadUsers,
  loadCredentialGrants,
  loadJenkinsInstances,
  openNewUser,
  editUser,
  toggleUserStatus,
  createCredentialGrant,
  revokeCredentialGrant,
  openNewJenkinsInstance,
  testJenkinsConnection,
  toggleJenkinsInstanceStatus,
  editJenkinsInstance,
  deleteJenkinsInstance
} = adminStore

</script>

<template>
  <section class="view">
    <div class="page-head"><div><h1>系统管理</h1><p>管理用户账号、项目凭证授权与 Jenkins 实例。</p></div><button v-if="authState.role === 'ADMIN'" class="btn primary" @click="openNewUser">＋ 添加用户</button></div>
    <div v-if="authState.role !== 'ADMIN'" class="card"><div class="card-body"><p class="muted">当前账号没有系统管理权限。</p></div></div>
    <div v-else class="section-grid grid"><div class="card"><div class="card-head"><h3>用户与角色</h3><span class="muted">{{ users.length }} 个账号</span></div><div class="card-body table-wrap"><table><thead><tr><th>用户</th><th>邮箱</th><th>角色</th><th>状态</th><th>最近登录</th><th></th></tr></thead><tbody><tr v-for="user in users" :key="user.id"><td><strong>{{ user.displayName }}</strong><br><span class="muted">{{ user.username }}</span></td><td>{{ user.email || '--' }}</td><td><span class="role-chip">{{ user.role }}</span></td><td><span class="tag" :class="user.status === 'DISABLED' ? 'gray' : 'green'">{{ user.status === 'DISABLED' ? '已禁用' : '启用' }}</span></td><td>{{ user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString('zh-CN') : '--' }}</td><td><div class="head-actions"><button class="btn" @click="editUser(user)">编辑</button><button class="btn" :disabled="isCurrentUser(user)" :title="isCurrentUser(user) ? '不能禁用当前登录用户' : ''" @click="toggleUserStatus(user)">{{ user.status === 'DISABLED' ? '启用' : '禁用' }}</button></div></td></tr><tr v-if="users.length === 0"><td colspan="6" class="muted">暂无用户</td></tr></tbody></table></div></div><div class="card"><div class="card-head"><h3>项目凭证授权</h3><span class="muted">{{ currentProject.name }}</span></div><div class="card-body"><form class="two-col" @submit.prevent="createCredentialGrant"><div class="field"><label>Credential ID</label><input v-model="credentialGrantForm.credentialRef" placeholder="例如 git-order-api" /></div><div class="field"><label>凭证作用域</label><input v-model="credentialGrantForm.credentialScopeRef" placeholder="例如 git" /></div><button class="btn primary" type="submit">授权凭证</button></form><div class="table-wrap"><table><thead><tr><th>Credential ID</th><th>作用域</th><th></th></tr></thead><tbody><tr v-for="grant in credentialGrants" :key="grant.id"><td>{{ grant.credentialRef }}</td><td>{{ grant.credentialScopeRef || '--' }}</td><td><button class="btn" @click="revokeCredentialGrant(grant)">撤销</button></td></tr><tr v-if="credentialGrants.length === 0"><td colspan="3" class="muted">当前项目暂无授权凭证</td></tr></tbody></table></div></div></div><div class="card"><div class="card-head"><h3>Jenkins 实例</h3><span class="muted">{{ jenkinsInstances.length }} 个实例</span><button class="btn primary" @click="openNewJenkinsInstance">＋ 新增实例</button></div><div class="card-body table-wrap"><table><thead><tr><th>实例</th><th>服务账号</th><th>Token</th><th>状态</th><th>连接状态</th><th></th></tr></thead><tbody><tr v-for="instance in jenkinsInstances" :key="instance.id"><td><strong>{{ instance.name }}</strong><br><span class="muted">{{ instance.baseUrl }}</span></td><td>{{ instance.username }}</td><td><span class="tag" :class="instance.tokenConfigured ? 'green' : 'gray'">{{ instance.tokenConfigured ? '已配置' : '未配置' }}</span></td><td><span class="tag" :class="instance.status === 'ENABLED' ? 'green' : 'gray'">{{ instance.status === 'ENABLED' ? '启用' : '禁用' }}</span></td><td><span class="tag" :class="instance.connectionStatus === 'CONNECTED' ? 'green' : instance.connectionStatus === 'FAILED' ? 'red' : 'gray'">{{ instance.connectionStatus === 'CONNECTED' ? '已连接' : instance.connectionStatus === 'FAILED' ? '连接失败' : '未知' }}</span><small v-if="instance.lastError" class="muted">：{{ instance.lastError }}</small></td><td><div class="head-actions"><button class="btn" :disabled="testingJenkinsInstanceId === instance.id" @click="testJenkinsConnection(instance)">{{ testingJenkinsInstanceId === instance.id ? '测试中...' : '测试连接' }}</button><button class="btn" @click="toggleJenkinsInstanceStatus(instance)">{{ instance.status === 'ENABLED' ? '禁用' : '启用' }}</button><button class="btn" @click="editJenkinsInstance(instance)">编辑</button><button class="btn" @click="deleteJenkinsInstance(instance)">删除</button></div></td></tr><tr v-if="jenkinsInstances.length === 0"><td colspan="6" class="muted">暂无 Jenkins 实例，点击右上角新增。</td></tr></tbody></table></div></div></div>
  </section>
</template>
