<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useAiStore } from '@/stores/ai'
import BaseModal from '@/components/common/BaseModal.vue'

const ui = useUiStore()
const { closeModal } = ui
const aiStore = useAiStore()
const { promptForm, editingPromptId } = storeToRefs(aiStore)
const { savePrompt } = aiStore

const title = computed(() => (editingPromptId.value ? '编辑 Prompt 模板' : '新建 Prompt 模板'))
const taskTypeOptions = [
  { value: 'TEST_CASE_GENERATION', label: 'AI 生成测试用例' },
  { value: 'TEST_CASE_CHAT', label: 'AI 用例对话' },
  { value: 'FAILURE_ANALYSIS', label: '失败分析' },
  { value: 'AUTOMATION_TASK_GENERATION', label: '自动化任务生成' }
]
</script>

<template>
  <BaseModal :title="title">
    <form class="modal-body" @submit.prevent="savePrompt">
      <div class="two-col">
        <div class="field"><label>Prompt 名称 *</label><input v-model="promptForm.name" /></div>
        <div class="field"><label>应用板块 *</label><select v-model="promptForm.taskType" required><option value="" disabled>请选择 Prompt 应用板块</option><option v-if="promptForm.taskType && !taskTypeOptions.some((option) => option.value === promptForm.taskType)" :value="promptForm.taskType">{{ promptForm.taskType }}</option><option v-for="option in taskTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option></select></div>
      </div>
      <div class="field"><label>版本</label><input v-model="promptForm.version" /></div>
      <div class="field"><label>Prompt 内容 *</label><textarea v-model="promptForm.content" rows="8"></textarea></div>
      <div class="modal-foot">
        <button type="button" class="btn" @click="closeModal">取消</button>
        <button class="btn primary">保存 Prompt</button>
      </div>
    </form>
  </BaseModal>
</template>
