<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useAutomationStore } from '@/stores/automation'
import BaseModal from '@/components/common/BaseModal.vue'

const ui = useUiStore()
const { closeModal } = ui
const automationStore = useAutomationStore()
const { taskForm, editingTaskId, automationProjects, taskBindings } = storeToRefs(automationStore)
const { saveTask, onTaskAutomationProjectChange } = automationStore

const title = computed(() => (editingTaskId.value ? '编辑测试任务' : '新建测试任务'))
</script>

<template>
  <BaseModal :title="title">
    <form class="modal-body" @submit.prevent="saveTask">
      <div class="field"><label>任务名称 *</label><input v-model="taskForm.name" /></div>
      <div class="two-col">
        <div class="field"><label>自动化项目 *</label><select v-model.number="taskForm.automationProjectId" @change="onTaskAutomationProjectChange"><option :value="null" disabled>请选择项目</option><option v-for="project in automationProjects" :key="project.id" :value="project.id">{{ project.name }}</option></select></div>
        <div class="field"><label>任务类型</label><input v-model="taskForm.taskType" /></div>
      </div>
      <div class="two-col">
        <div class="field"><label>执行环境 *</label><select v-model="taskForm.environment"><option>TEST</option><option>STAGING</option><option>PROD</option></select></div>
        <div class="field"><label>Branch *</label><input v-model="taskForm.branch" placeholder="main" /></div>
      </div>
      <div class="field"><label>执行目标（Pipeline Binding）</label><select v-model.number="taskForm.pipelineBindingId"><option :value="null">不指定（使用自动化项目默认 Job）</option><option v-for="binding in taskBindings" :key="binding.id" :value="binding.id">{{ binding.jobName }} · {{ binding.pipelineType }}</option></select><small class="muted" v-if="taskForm.automationProjectId && taskBindings.length === 0">当前自动化项目暂无执行目标，可先到自动化项目卡片「执行目标」处导入或新建。</small></div>
      <div class="field"><label>执行参数（JSON）</label><textarea v-model="taskForm.parametersJson" rows="4" placeholder='{"suite":"smoke","tags":"P0"}'></textarea></div>
      <div class="modal-foot">
        <button type="button" class="btn" @click="closeModal">取消</button>
        <button class="btn primary">{{ editingTaskId ? '保存修改' : '创建测试任务' }}</button>
      </div>
    </form>
  </BaseModal>
</template>
