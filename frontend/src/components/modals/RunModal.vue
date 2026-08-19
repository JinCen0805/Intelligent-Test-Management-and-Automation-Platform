<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useAutomationStore } from '@/stores/automation'
import BaseModal from '@/components/common/BaseModal.vue'

const ui = useUiStore()
const { closeModal } = ui
const automationStore = useAutomationStore()
const { runForm, executableTasks } = storeToRefs(automationStore)
const { runTest, syncRunFormFromTask } = automationStore
</script>

<template>
  <BaseModal title="执行测试任务">
    <form class="modal-body" @submit.prevent="runTest">
      <div class="field">
        <label>测试任务</label>
        <select v-model.number="runForm.taskId" @change="syncRunFormFromTask">
          <option :value="null" disabled>请选择测试任务</option>
          <option v-for="task in executableTasks" :key="task.id" :value="task.id">{{ task.name }} · {{ task.jenkinsJob }}</option>
        </select>
      </div>
      <div class="field"><label>执行参数（JSON）</label><textarea v-model="runForm.parametersJson" rows="4" placeholder='{"suite":"smoke"}'></textarea></div>
      <div class="ai-callout"><strong>执行链路</strong><p>平台将通过 Jenkins 触发构建，完成后回收 Build、Commit、日志和 Allure 报告。</p></div>
      <div class="modal-foot">
        <button type="button" class="btn" @click="closeModal">取消</button>
        <button class="btn primary">立即执行</button>
      </div>
    </form>
  </BaseModal>
</template>
