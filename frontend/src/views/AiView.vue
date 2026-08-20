<script setup lang="ts">
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useAuthStore } from '@/stores/auth'
import { useAiStore } from '@/stores/ai'

const ui = useUiStore()
const auth = useAuthStore()
const aiStore = useAiStore()

const { activeAiTab } = storeToRefs(ui)
const { authState } = storeToRefs(auth)
const { aiModels, promptTemplates, testingModelId } = storeToRefs(aiStore)
const {
  loadAiConfig,
  openModelEditor,
  toggleModelEnabled,
  testModel,
  deleteModel,
  openPromptEditor,
  togglePromptEnabled,
  publishPrompt,
  activatePrompt
} = aiStore

onMounted(() => { void loadAiConfig() })
</script>

<template>
  <section class="view">
    <div class="page-head"><div><h1>AI 能力</h1><p>管理模型与 Prompt 模板，AI 服务于用例生成和失败分析。</p></div><div class="head-actions"><button v-if="authState.role === 'ADMIN' && activeAiTab === 'prompts'" class="btn" @click="openPromptEditor(null)">＋ 新建 Prompt</button><button v-if="authState.role === 'ADMIN' && activeAiTab === 'models'" class="btn primary" @click="openModelEditor(null)">＋ 新建模型</button></div></div>
    <div v-if="authState.role !== 'ADMIN'" class="card"><div class="card-body"><p class="muted">当前账号没有 AI 配置管理权限，测试用例生成仍可使用项目默认模型。</p></div></div>
    <div v-else class="card"><div class="card-body"><div class="subtabs"><button :class="{ active: activeAiTab === 'models' }" @click="activeAiTab = 'models'">模型管理</button><button :class="{ active: activeAiTab === 'prompts' }" @click="activeAiTab = 'prompts'">Prompt 模板</button></div><div class="table-wrap"><table v-if="activeAiTab === 'models'"><thead><tr><th>模型</th><th>服务商类型</th><th>AI 请求地址</th><th>API Key</th><th>状态</th><th></th></tr></thead><tbody><tr v-for="model in aiModels" :key="model.id"><td><strong>{{ model.name }}</strong></td><td>{{ model.provider }}</td><td>{{ model.endpoint || '本地演示' }}</td><td>{{ model.hasApiKey ? '已配置' : '未配置' }}</td><td><span class="tag" :class="model.enabled ? 'green' : 'gray'">{{ model.enabled ? '启用' : '停用' }}</span></td><td><div class="head-actions"><button class="btn" :disabled="testingModelId === model.id" @click="testModel(model)">{{ testingModelId === model.id ? '测试中...' : '测试连接' }}</button><button class="btn" @click="openModelEditor(model)">编辑</button><button class="btn" @click="toggleModelEnabled(model)">{{ model.enabled ? '停用' : '启用' }}</button><button class="btn danger-text" @click="deleteModel(model)">删除</button></div></td></tr><tr v-if="aiModels.length === 0"><td colspan="6" class="muted">暂无 AI 模型</td></tr></tbody></table><table v-else-if="activeAiTab === 'prompts'"><thead><tr><th>Prompt 名称</th><th>任务类型</th><th>版本</th><th>状态</th><th></th></tr></thead><tbody><tr v-for="prompt in promptTemplates" :key="prompt.id"><td><strong>{{ prompt.name }}</strong></td><td>{{ prompt.taskType }}</td><td>{{ prompt.version }}</td><td><span class="tag" :class="prompt.enabled ? 'green' : 'gray'">{{ prompt.enabled ? '启用' : '停用' }}</span></td><td><div class="head-actions"><button class="btn" @click="openPromptEditor(prompt)">编辑</button><button class="btn" @click="togglePromptEnabled(prompt)">{{ prompt.enabled ? '停用' : '启用' }}</button><button class="btn" @click="publishPrompt(prompt)">发布</button><button v-if="!prompt.enabled" class="btn" @click="activatePrompt(prompt)">启用此版本</button></div></td></tr><tr v-if="promptTemplates.length === 0"><td colspan="5" class="muted">暂无 Prompt 模板</td></tr></tbody></table></div></div></div>
  </section>
</template>
