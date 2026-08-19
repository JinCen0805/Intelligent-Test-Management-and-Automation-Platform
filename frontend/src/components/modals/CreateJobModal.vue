<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useAutomationStore } from '@/stores/automation'
import { useAdminStore } from '@/stores/admin'
import BaseModal from '@/components/common/BaseModal.vue'

const ui = useUiStore()
const { openModal } = ui
const automationStore = useAutomationStore()
const { createJobForm } = storeToRefs(automationStore)
const { createJenkinsJob } = automationStore
const { jenkinsInstances } = storeToRefs(useAdminStore())
</script>

<template>
  <BaseModal title="平台创建 Jenkins Job">
    <div class="modal-body">
      <p class="muted">
        平台将生成 <code>config.xml</code> 并调用 Jenkins API 创建 Job，成功后自动建立执行目标绑定。
      </p>
      <form @submit.prevent="createJenkinsJob">
        <div class="field">
          <label>Jenkins 实例 *</label>
          <select v-model.number="createJobForm.jenkinsInstanceId">
            <option :value="null" disabled>请选择实例</option>
            <option v-for="instance in jenkinsInstances.filter((item) => item.status === 'ENABLED')" :key="instance.id" :value="instance.id">{{ instance.name }} · {{ instance.baseUrl }}</option>
          </select>
        </div>
        <div class="field">
          <label>Job 名称 *</label>
          <input v-model="createJobForm.jobName" placeholder="例如 folder/order-api-smoke" />
          <small class="muted">可包含 folder 前缀（该 folder 需已在 Jenkins 中存在）。</small>
        </div>
        <div class="field">
          <label>Pipeline 类型 *</label>
          <select v-model="createJobForm.pipelineType">
            <option value="PIPELINE_JOB">Pipeline Job</option>
            <option value="FREESTYLE_JOB">Freestyle Job</option>
          </select>
        </div>
        <template v-if="createJobForm.pipelineType === 'PIPELINE_JOB'">
          <div class="field">
            <label>脚本来源</label>
            <select v-model="createJobForm.scriptSource">
              <option value="TEMPLATE">平台生成模板</option>
              <option value="CUSTOM">粘贴 Jenkinsfile</option>
            </select>
          </div>
          <div v-if="createJobForm.scriptSource === 'CUSTOM'" class="field">
            <label>Jenkinsfile 脚本 *</label>
            <textarea v-model="createJobForm.customScript" rows="8" placeholder="粘贴 Declarative Pipeline 脚本"></textarea>
          </div>
        </template>
        <div class="field">
          <label>凭证作用域</label>
          <input v-model="createJobForm.credentialScopeRef" placeholder="可选" />
        </div>
        <div class="modal-foot">
          <button type="button" class="btn" @click="openModal('binding')">返回执行目标</button>
          <button type="submit" class="btn primary">创建 Job</button>
        </div>
      </form>
    </div>
  </BaseModal>
</template>
