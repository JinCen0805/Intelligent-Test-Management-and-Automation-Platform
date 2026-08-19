<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useAutomationStore } from '@/stores/automation'
import { useAdminStore } from '@/stores/admin'
import BaseModal from '@/components/common/BaseModal.vue'

const ui = useUiStore()
const { openModal } = ui
const automationStore = useAutomationStore()
const { jobImportForm, jenkinsJobs } = storeToRefs(automationStore)
const { loadJenkinsJobs, importJenkinsJob } = automationStore
const { jenkinsInstances } = storeToRefs(useAdminStore())
</script>

<template>
  <BaseModal title="导入 Jenkins Job">
    <div class="modal-body">
      <div class="field">
        <label>Jenkins 实例 *</label>
        <select v-model.number="jobImportForm.jenkinsInstanceId" @change="loadJenkinsJobs">
          <option :value="null" disabled>请选择实例</option>
          <option v-for="instance in jenkinsInstances.filter((item) => item.status === 'ENABLED')" :key="instance.id" :value="instance.id">{{ instance.name }} · {{ instance.baseUrl }}</option>
        </select>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Job</th><th>类型</th><th>可构建</th><th></th></tr></thead>
          <tbody>
            <tr v-for="job in jenkinsJobs" :key="job.path">
              <td><strong>{{ job.name }}</strong><br><span class="muted">{{ job.path }}</span></td>
              <td><span class="tag blue">{{ job.pipelineType }}</span></td>
              <td>{{ job.buildable ? '是' : '否' }}</td>
              <td><button class="btn primary" :disabled="job.imported" @click="importJenkinsJob(job)">{{ job.imported ? '已导入' : '导入' }}</button></td>
            </tr>
            <tr v-if="jenkinsJobs.length === 0"><td colspan="4" class="muted">请先选择 Jenkins 实例。若列表为空，说明该实例暂无可用 Job。</td></tr>
          </tbody>
        </table>
      </div>
      <div class="modal-foot"><button class="btn" @click="openModal('binding')">返回执行目标</button></div>
    </div>
  </BaseModal>
</template>
