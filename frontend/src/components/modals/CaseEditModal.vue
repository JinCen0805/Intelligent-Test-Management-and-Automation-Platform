<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useTestcaseStore } from '@/stores/testcase'
import BaseModal from '@/components/common/BaseModal.vue'

const ui = useUiStore()
const { closeModal } = ui
const testcaseStore = useTestcaseStore()
const { caseForm, editingCaseId } = storeToRefs(testcaseStore)
const { saveCase } = testcaseStore

const title = computed(() => (editingCaseId.value ? '编辑测试用例' : '新建测试用例'))
</script>

<template>
  <BaseModal :title="title">
    <form class="modal-body" @submit.prevent="saveCase">
      <div class="two-col">
        <div class="field"><label>用例编号</label><input v-model="caseForm.caseNo" placeholder="留空自动生成" /></div>
        <div class="field"><label>优先级</label><select v-model="caseForm.priority"><option>P0</option><option>P1</option><option>P2</option><option>P3</option></select></div>
      </div>
      <div class="field"><label>用例标题 *</label><input v-model="caseForm.title" placeholder="例如：创建订单成功" /></div>
      <div class="two-col">
        <div class="field"><label>模块</label><input v-model="caseForm.moduleName" placeholder="例如：订单中心" /></div>
        <div class="field"><label>测试类型</label><select v-model="caseForm.caseType"><option value="FUNCTIONAL">功能</option><option value="EXCEPTION">异常</option><option value="BOUNDARY">边界</option><option value="其他">其他</option></select></div>
      </div>
      <div class="field"><label>前置条件</label><textarea v-model="caseForm.precondition"></textarea></div>
      <div class="field"><label>测试步骤</label><textarea v-model="caseForm.steps"></textarea></div>
      <div class="field"><label>测试数据</label><textarea v-model="caseForm.testData"></textarea></div>
      <div class="field"><label>预期结果</label><textarea v-model="caseForm.expectedResult"></textarea></div>
      <div class="field"><label>标签</label><input v-model="caseForm.tags" placeholder="多个标签用逗号分隔" /></div>
      <div class="modal-foot">
        <button type="button" class="btn" @click="closeModal">取消</button>
        <button class="btn primary">{{ editingCaseId ? '保存修改' : '创建用例' }}</button>
      </div>
    </form>
  </BaseModal>
</template>
