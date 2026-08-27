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

function submitUpload() {
  return uploadRequirement()
}
</script>

<template>
  <BaseModal title="添加项目文件">
    <form class="modal-body" @submit.prevent="submitUpload">
      <div class="field"><label>上传到当前项目</label><input :value="currentProject.name" disabled /></div>
      <div class="dropzone">
        <div class="upload-icon">⇧</div>
        <strong>{{ requirementFile?.name || '选择项目文件' }}</strong>
        <p>支持 PDF、Word、Excel、PPT、文本、数据、图片和压缩包 · 单文件不超过 10MB</p>
        <label class="btn file-picker">选择文件<input type="file" accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.markdown,.csv,.tsv,.json,.xml,.html,.htm,.yaml,.yml,.sql,.log,.rtf,.zip,.rar,.7z,.png,.jpg,.jpeg,.gif,.webp" @change="handleRequirementFile" /></label>
      </div>
      <div class="field"><label>文件备注（可选）</label><input v-model="requirementVersion" placeholder="例如：登录流程说明" /></div>
      <div class="modal-foot">
        <button type="button" class="btn" @click="closeModal">取消</button>
        <button class="btn primary">保存文件</button>
      </div>
    </form>
  </BaseModal>
</template>
