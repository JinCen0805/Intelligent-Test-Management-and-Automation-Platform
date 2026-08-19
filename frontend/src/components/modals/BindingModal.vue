<script setup lang="ts">
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useAutomationStore } from '@/stores/automation'
import { useAdminStore } from '@/stores/admin'
import BaseModal from '@/components/common/BaseModal.vue'

const automationStore = useAutomationStore()
const { pipelineBindings, bindingForm, editingBindingId, bindingFormVisible } = storeToRefs(automationStore)
const { openJobImport, openCreateJob, resetBindingForm, openNewBinding, closeBindingForm, editBinding, saveBinding } = automationStore
const adminStore = useAdminStore()
const { jenkinsInstances } = storeToRefs(adminStore)
const { loadJenkinsInstances } = adminStore

onMounted(() => {
  // 绑定/导入/创建 Job 都需要实例下拉，进入执行目标管理时按需加载，不再依赖先访问管理页。
  void loadJenkinsInstances()
})
</script>

<template>
  <BaseModal title="执行目标管理">
    <div class="modal-body">
      <div class="head-actions">
        <button class="btn primary" @click="openJobImport">⇩ 导入 Jenkins Job</button>
        <button class="btn" @click="openCreateJob">＋ 平台创建 Job</button>
        <button class="btn" @click="openNewBinding">＋ 新建绑定</button>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Pipeline 类型</th><th>Job 名称</th><th>Jenkins 实例</th><th>管理方式</th><th>状态</th><th></th></tr></thead>
          <tbody>
            <tr v-for="binding in pipelineBindings" :key="binding.id">
              <td><span class="tag blue">{{ binding.pipelineType }}</span></td>
              <td><strong>{{ binding.jobName }}</strong><br><span v-if="binding.folderPath" class="muted">{{ binding.folderPath }}</span></td>
              <td>{{ jenkinsInstances.find((item) => item.id === binding.jenkinsInstanceId)?.name || `#${binding.jenkinsInstanceId}` }}</td>
              <td><span class="tag" :class="binding.managedBy === 'EXTERNAL' ? 'gray' : 'green'">{{ binding.managedBy === 'EXTERNAL' ? '外部' : '平台' }}</span></td>
              <td><span class="tag" :class="binding.provisioningStatus === 'READY' ? 'green' : binding.provisioningStatus === 'FAILED' ? 'red' : 'orange'">{{ binding.provisioningStatus || 'PENDING' }}</span><small v-if="binding.lastError" class="muted">：{{ binding.lastError }}</small></td>
              <td><button class="btn" @click="editBinding(binding)">编辑</button></td>
            </tr>
            <tr v-if="pipelineBindings.length === 0"><td colspan="6" class="muted">暂无执行目标，可从 Jenkins 导入 Job 或新建绑定。</td></tr>
          </tbody>
        </table>
      </div>
      <form v-if="bindingFormVisible" @submit.prevent="saveBinding">
        <div class="card-head"><h3>{{ editingBindingId ? '编辑绑定' : '新建绑定' }}</h3></div>
        <div class="two-col">
          <div class="field"><label>Pipeline 类型 *</label><select v-model="bindingForm.pipelineType"><option>MULTIBRANCH_PIPELINE</option><option>PIPELINE_JOB</option><option>FREESTYLE_JOB</option></select></div>
          <div class="field"><label>Jenkins 实例 *</label><select v-model.number="bindingForm.jenkinsInstanceId"><option :value="null" disabled>请选择实例</option><option v-for="instance in jenkinsInstances.filter((item) => item.status === 'ENABLED')" :key="instance.id" :value="instance.id">{{ instance.name }} · {{ instance.baseUrl }}</option></select></div>
        </div>
        <div class="two-col">
          <div class="field"><label>Folder 路径</label><input v-model="bindingForm.folderPath" placeholder="例如 folder/sub" /></div>
          <div class="field"><label>Job 名称 *</label><input v-model="bindingForm.jobName" placeholder="例如 folder/order-api" /></div>
        </div>
        <div class="two-col">
          <div class="field"><label>Job URL</label><input v-model="bindingForm.jobUrl" /></div>
          <div class="field"><label>凭证作用域</label><input v-model="bindingForm.credentialScopeRef" /></div>
        </div>
        <div class="field"><label>管理方式</label><select v-model="bindingForm.managedBy"><option value="EXTERNAL">外部（默认不被平台修改）</option><option value="PLATFORM">平台托管</option></select></div>
        <div class="modal-foot"><button type="button" class="btn" @click="resetBindingForm">重置</button><button type="button" class="btn" @click="closeBindingForm">取消</button><button class="btn primary">{{ editingBindingId ? '保存修改' : '创建绑定' }}</button></div>
      </form>
    </div>
  </BaseModal>
</template>
