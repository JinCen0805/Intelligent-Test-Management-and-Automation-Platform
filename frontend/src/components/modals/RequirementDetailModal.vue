<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useRequirementStore } from '@/stores/requirement'
import BaseModal from '@/components/common/BaseModal.vue'

const ui = useUiStore()
const { closeModal } = ui
const { requirementDetail } = storeToRefs(useRequirementStore())
</script>

<template>
  <BaseModal title="需求详情">
    <div class="modal-body">
      <div class="detail-grid">
        <div><span class="muted">文档名称</span><strong>{{ requirementDetail?.name }}</strong></div>
        <div><span class="muted">版本</span><strong>{{ requirementDetail?.version }}</strong></div>
        <div><span class="muted">格式</span><strong>{{ requirementDetail?.fileType }}</strong></div>
        <div><span class="muted">解析状态</span><strong>{{ requirementDetail?.parseStatus }}</strong></div>
      </div>
      <div v-if="requirementDetail?.parseError" class="auth-error">{{ requirementDetail.parseError }}</div>
      <pre class="requirement-content">{{ requirementDetail?.contentText || '暂无正文内容' }}</pre>
      <div class="modal-foot"><button class="btn" @click="closeModal">关闭</button></div>
    </div>
  </BaseModal>
</template>
