<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useAutomationStore } from '@/stores/automation'
import BaseModal from '@/components/common/BaseModal.vue'

const ui = useUiStore()
const { closeModal } = ui
const automationStore = useAutomationStore()
const { aiTaskForm, aiDraftForm, aiTaskLoading, aiTaskConfirmResult } = storeToRefs(automationStore)
const { openAiTask, resetAiTask, generateAiTask, saveAiDraft, confirmAiDraft } = automationStore

function viewTasks() {
  closeModal()
  ui.activeProjectTab = 'tasks'
}
</script>

<template>
  <BaseModal title="AI 创建自动化任务">
    <div class="modal-body">
      <template v-if="aiTaskConfirmResult">
        <div class="detail-grid">
          <div><span class="muted">自动化项目 #</span><strong>{{ aiTaskConfirmResult.automationProjectId }}</strong></div>
          <div><span class="muted">测试任务 #</span><strong>{{ aiTaskConfirmResult.testTaskId }}</strong></div>
          <div><span class="muted">任务名称</span><strong>{{ aiTaskConfirmResult.task.name }}</strong></div>
          <div><span class="muted">Jenkins Job</span><strong>{{ aiTaskConfirmResult.task.jenkinsJob || '--' }}</strong></div>
        </div>
        <div class="ai-callout"><strong>任务已创建</strong><p>已生成自动化项目与测试任务，Jenkins 将在执行时被调用。{{ aiTaskConfirmResult.disclaimer }}</p></div>
        <div class="modal-foot"><button class="btn" @click="closeModal">关闭</button><button class="btn primary" @click="viewTasks">查看测试任务</button></div>
      </template>
      <template v-else-if="aiDraftForm.id != null">
        <div class="flow compact-flow"><span class="done">1 描述</span><i>→</i><span class="done">2 生成草稿</span><i>→</i><span class="current">3 编辑确认</span><i>→</i><span>4 创建任务</span></div>
        <form @submit.prevent="saveAiDraft">
          <div class="two-col"><div class="field"><label>任务名称 *</label><input v-model="aiDraftForm.name" /></div><div class="field"><label>任务类型</label><select v-model="aiDraftForm.taskType"><option>SMOKE</option><option>REGRESSION</option></select></div></div>
          <div class="two-col"><div class="field"><label>自动化类型</label><select v-model="aiDraftForm.automationType"><option>API</option><option>UI</option></select></div><div class="field"><label>框架</label><select v-model="aiDraftForm.framework"><option>PYTEST</option><option>PLAYWRIGHT</option><option>SELENIUM</option></select></div></div>
          <div class="field"><label>Git 仓库地址</label><input v-model="aiDraftForm.repositoryUrl" /></div>
          <div class="two-col"><div class="field"><label>Branch</label><input v-model="aiDraftForm.branch" /></div><div class="field"><label>环境</label><select v-model="aiDraftForm.environment"><option>TEST</option><option>STAGING</option><option>PROD</option></select></div></div>
          <div class="two-col"><div class="field"><label>测试路径</label><input v-model="aiDraftForm.testPath" /></div><div class="field"><label>Pipeline 类型</label><select v-model="aiDraftForm.pipelineType"><option>PIPELINE</option><option>FREESTYLE</option></select></div></div>
          <div class="field"><label>执行命令</label><input v-model="aiDraftForm.executeCommand" /></div>
          <div class="field"><label>Jenkins Job</label><input v-model="aiDraftForm.jenkinsJob" /></div>
          <div class="field"><label>执行参数（JSON）</label><textarea v-model="aiDraftForm.parametersJson" rows="3"></textarea></div>
          <label class="switch-row"><input v-model="aiDraftForm.allureEnabled" type="checkbox" />生成 Allure 报告</label>
          <div v-if="aiDraftForm.allureConfig" class="muted">Allure 配置：{{ aiDraftForm.allureConfig }}</div>
          <div v-if="aiDraftForm.missingItems.length" class="ai-callout"><strong>待补充项</strong><ul class="risk-list"><li v-for="(item, index) in aiDraftForm.missingItems" :key="index">{{ item }}</li></ul></div>
          <div v-if="aiDraftForm.riskHints.length" class="ai-callout"><strong>风险提示</strong><ul class="risk-list"><li v-for="(item, index) in aiDraftForm.riskHints" :key="index">{{ item }}</li></ul></div>
          <p class="muted">生成来源：{{ aiDraftForm.modelName }}（{{ aiDraftForm.provider }}）· {{ aiDraftForm.promptName }} {{ aiDraftForm.promptVersion }}</p>
          <p class="muted">{{ aiDraftForm.disclaimer }}</p>
          <div class="modal-foot"><button type="button" class="btn" @click="resetAiTask">返回重新输入</button><button type="button" class="btn" @click="closeModal">取消</button><button class="btn" :disabled="aiTaskLoading"><span v-if="aiTaskLoading" class="spinner"></span>保存草稿</button><button type="button" class="btn primary" :disabled="aiTaskLoading" @click="confirmAiDraft"><span v-if="aiTaskLoading" class="spinner"></span>{{ aiTaskLoading ? '创建中...' : '确认并创建任务' }}</button></div>
        </form>
      </template>
      <template v-else>
        <div class="flow compact-flow"><span class="current">1 描述</span><i>→</i><span>2 生成草稿</span><i>→</i><span>3 编辑确认</span><i>→</i><span>4 创建任务</span></div>
        <form @submit.prevent="generateAiTask">
          <div class="field"><label>Git 仓库地址 *</label><input v-model="aiTaskForm.repositoryUrl" placeholder="https://example.com/order-api.git" /></div>
          <div class="field"><label>Branch *</label><input v-model="aiTaskForm.branch" placeholder="main" /></div>
          <div class="field"><label>自然语言描述 *</label><textarea v-model="aiTaskForm.description" rows="4" placeholder="这是我的 pytest 接口自动化仓库，请创建 TEST 环境回归任务，执行 tests/regression，并生成 Allure 报告。"></textarea></div>
          <div class="ai-callout"><strong>AI 生成说明</strong><p>AI 将识别 API/UI 类型，推断框架、测试路径、执行命令、环境与参数、Pipeline 类型及 Allure 配置，并给出缺失项与风险提示。AI 不会修改 Git 代码、提交代码或创建 PR。</p></div>
          <div class="modal-foot"><button type="button" class="btn" @click="closeModal">取消</button><button class="btn primary" :disabled="aiTaskLoading"><span v-if="aiTaskLoading" class="spinner"></span>{{ aiTaskLoading ? '生成中...' : '生成配置草稿' }}</button></div>
        </form>
      </template>
    </div>
  </BaseModal>
</template>
