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
  <BaseModal title="文件信息">
    <div class="modal-body">
      <div class="detail-grid">
        <div><span class="muted">文件名称</span><strong>{{ requirementDetail?.name }}</strong></div>
        <div><span class="muted">备注</span><strong>{{ requirementDetail?.version || '--' }}</strong></div>
        <div><span class="muted">格式</span><strong>{{ requirementDetail?.fileType }}</strong></div>
        <div><span class="muted">存储状态</span><strong>已存储</strong></div>
      </div>
      <p class="muted">文件内容仅在 AI 生成测试用例时按需读取。</p>
      <div class="modal-foot"><button class="btn" @click="closeModal">关闭</button></div>
    </div>
  </BaseModal>
</template>
