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
</script>

<template>
  <BaseModal :title="title">
    <form class="modal-body" @submit.prevent="savePrompt">
      <div class="two-col">
        <div class="field"><label>Prompt 名称 *</label><input v-model="promptForm.name" /></div>
        <div class="field"><label>任务类型</label><input v-model="promptForm.taskType" /></div>
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
