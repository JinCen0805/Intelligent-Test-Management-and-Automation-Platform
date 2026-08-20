<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useAiStore } from '@/stores/ai'
import BaseModal from '@/components/common/BaseModal.vue'

const ui = useUiStore()
const { closeModal } = ui
const aiStore = useAiStore()
const { modelForm, editingModelId } = storeToRefs(aiStore)
const { saveModel } = aiStore

const title = computed(() => (editingModelId.value ? '编辑 AI 模型' : '新建 AI 模型'))
</script>

<template>
  <BaseModal :title="title">
    <form class="modal-body" @submit.prevent="saveModel">
      <div class="two-col">
        <div class="field"><label>服务商类型</label><input v-model="modelForm.provider" placeholder="例如：DEEPSEEK、OPENAI" /></div>
        <div class="field"><label>模型名称</label><input v-model="modelForm.name" /></div>
      </div>
      <div class="field"><label>AI 请求地址</label><input v-model="modelForm.endpoint" placeholder="https://api.example.com/v1" /><span class="muted">填写服务商提供的 API 根地址，不要包含 /chat/completions。</span></div>
      <div class="field"><label>API Key</label><input v-model="modelForm.apiKey" type="password" /></div>
      <div class="modal-foot">
        <button type="button" class="btn" @click="closeModal">取消</button>
        <button class="btn primary">保存模型</button>
      </div>
    </form>
  </BaseModal>
</template>
