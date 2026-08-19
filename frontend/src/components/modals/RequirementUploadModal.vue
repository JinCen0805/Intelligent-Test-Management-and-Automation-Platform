<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useProjectStore } from '@/stores/project'
import { useRequirementStore } from '@/stores/requirement'
import BaseModal from '@/components/common/BaseModal.vue'

const ui = useUiStore()
const { closeModal } = ui
const { currentProject } = storeToRefs(useProjectStore())
const requirementStore = useRequirementStore()
const { requirementFile, requirementVersion } = storeToRefs(requirementStore)
const { uploadRequirement, handleRequirementFile } = requirementStore
</script>

<template>
  <BaseModal title="上传需求文档">
    <form class="modal-body" @submit.prevent="uploadRequirement">
      <div class="field"><label>上传到当前项目</label><input :value="currentProject.name" disabled /></div>
      <div class="dropzone">
        <div class="upload-icon">⇧</div>
        <strong>{{ requirementFile?.name || '选择需求文件' }}</strong>
        <p>支持 Markdown、PDF、DOCX、TXT · 单文件不超过 10MB</p>
        <label class="btn file-picker">选择文件<input type="file" accept=".md,.markdown,.pdf,.docx,.txt" @change="handleRequirementFile" /></label>
      </div>
      <div class="field"><label>文档版本说明</label><input v-model="requirementVersion" placeholder="例如：V1.2" /></div>
      <div class="modal-foot">
        <button type="button" class="btn" @click="closeModal">取消</button>
        <button class="btn primary">上传并解析</button>
      </div>
    </form>
  </BaseModal>
</template>
