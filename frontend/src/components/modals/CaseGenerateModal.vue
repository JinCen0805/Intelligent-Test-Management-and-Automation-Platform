<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useRequirementStore } from '@/stores/requirement'
import { useAiStore } from '@/stores/ai'
import BaseModal from '@/components/common/BaseModal.vue'

const ui = useUiStore()
const { closeModal } = ui
const { requirements } = storeToRefs(useRequirementStore())
const aiStore = useAiStore()
const { generationForm, aiModels, generatedCaseList, generationDone, isGenerating } = storeToRefs(aiStore)
const { generateTestCases } = aiStore
</script>

<template>
  <BaseModal title="AI 测试用例生成">
    <form class="modal-body" @submit.prevent="generateTestCases">
      <div class="flow compact-flow"><span class="done">1 上传 PRD</span><i>→</i><span class="done">2 解析文档</span><i>→</i><span class="current">3 生成 Draft</span><i>→</i><span>4 Review</span></div>
      <div class="two-col">
        <div class="field"><label>需求文档</label><select v-model.number="generationForm.requirementId"><option v-for="requirement in requirements.filter((item) => item.id && item.status === '已解析')" :key="requirement.id" :value="requirement.id">{{ requirement.name }} {{ requirement.version }}</option></select></div>
        <div class="field"><label>AI 模型</label><select v-model.number="generationForm.modelId"><option v-for="model in aiModels" :key="model.id" :value="model.id">{{ model.name }} · {{ model.provider }}</option><option v-if="aiModels.length === 0" :value="null">使用项目默认模型</option></select></div>
      </div>
      <div class="field"><label>测试规则</label><input v-model="generationForm.rules" /></div>
      <div class="draft-preview">
        <div class="card-head"><h3>生成结果 Draft</h3><span class="tag orange">{{ generationDone ? `${generatedCaseList.length} 条待 Review` : '等待生成' }}</span></div>
        <div class="card-body table-wrap">
          <table>
            <thead><tr><th>编号</th><th>标题</th><th>状态</th></tr></thead>
            <tbody>
              <tr v-for="item in generatedCaseList" :key="item.id"><td>{{ item.caseNo }}</td><td>{{ item.title }}</td><td><span class="tag orange">{{ item.status }}</span></td></tr>
              <tr v-if="generatedCaseList.length === 0"><td colspan="3" class="muted">生成后显示真实 Draft 结果</td></tr>
            </tbody>
          </table>
        </div>
      </div>
      <div class="modal-foot">
        <button type="button" class="btn" @click="closeModal">取消</button>
        <button class="btn primary" :disabled="isGenerating"><span v-if="isGenerating" class="spinner"></span>{{ isGenerating ? '生成中...' : '生成并进入 Review' }}</button>
      </div>
    </form>
  </BaseModal>
</template>
