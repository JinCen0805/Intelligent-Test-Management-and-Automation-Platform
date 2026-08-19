<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useReportStore } from '@/stores/report'
import BaseModal from '@/components/common/BaseModal.vue'

const ui = useUiStore()
const { closeModal } = ui
const { analysisResult } = storeToRefs(useReportStore())
</script>

<template>
  <BaseModal title="AI 失败分析">
    <div class="modal-body analysis-body">
      <div v-if="!analysisResult" class="muted">点击「AI 分析」后在此查看失败根因分析结果。</div>
      <template v-else>
        <div v-if="analysisResult.status === 'FAILED'" class="auth-error">分析失败：{{ analysisResult.errorMessage || 'AI 服务调用失败' }}</div>
        <div class="detail-grid">
          <div><span class="muted">执行</span><strong>#{{ analysisResult.executionId }} · {{ analysisResult.projectName }}</strong></div>
          <div><span class="muted">任务</span><strong>{{ analysisResult.taskName }}</strong></div>
          <div><span class="muted">Build / Branch</span><strong>{{ analysisResult.buildNumber || '--' }} / {{ analysisResult.branch || '--' }}</strong></div>
          <div><span class="muted">Commit</span><strong>{{ analysisResult.commitSha || '--' }}</strong></div>
          <div><span class="muted">模型</span><strong>{{ analysisResult.modelName }} · {{ analysisResult.provider }}</strong></div>
          <div><span class="muted">Prompt</span><strong>{{ analysisResult.promptName }} {{ analysisResult.promptVersion }}</strong></div>
        </div>
        <div class="analysis-category"><span class="tag orange">{{ analysisResult.categoryLabel }}</span></div>
        <div v-if="analysisResult.possibleCauses.length" class="analysis-section"><h4>可能原因</h4><ul><li v-for="item in analysisResult.possibleCauses" :key="item">{{ item }}</li></ul></div>
        <div v-if="analysisResult.relatedErrors.length" class="analysis-section"><h4>相关报错</h4><pre v-for="item in analysisResult.relatedErrors" :key="item">{{ item }}</pre></div>
        <div v-if="analysisResult.checkLocations.length" class="analysis-section"><h4>排查位置</h4><ul><li v-for="item in analysisResult.checkLocations" :key="item">{{ item }}</li></ul></div>
        <div v-if="analysisResult.fixDirections.length" class="analysis-section"><h4>修复方向</h4><ul><li v-for="item in analysisResult.fixDirections" :key="item">{{ item }}</li></ul></div>
        <div v-if="analysisResult.riskWarnings.length" class="analysis-section"><h4>风险提示</h4><ul><li v-for="item in analysisResult.riskWarnings" :key="item">{{ item }}</li></ul></div>
        <p class="muted">{{ analysisResult.disclaimer }}</p>
      </template>
      <div class="modal-foot"><button class="btn" @click="closeModal">关闭</button></div>
    </div>
  </BaseModal>
</template>
