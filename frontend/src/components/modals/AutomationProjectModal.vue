<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useAutomationStore } from '@/stores/automation'
import { useAdminStore } from '@/stores/admin'
import BaseModal from '@/components/common/BaseModal.vue'

const ui = useUiStore()
const { closeModal } = ui
const automationStore = useAutomationStore()
const { automationProjectForm, editingAutomationProjectId } = storeToRefs(automationStore)
const { createAutomationProject } = automationStore
const { credentialGrants } = storeToRefs(useAdminStore())

const title = computed(() => (editingAutomationProjectId.value ? '编辑自动化项目' : '新建自动化项目'))
</script>

<template>
  <BaseModal :title="title">
    <form class="modal-body" @submit.prevent="createAutomationProject">
      <div class="two-col">
        <div class="field"><label>名称 *</label><input v-model="automationProjectForm.name" /></div>
        <div class="field"><label>类型</label><select v-model="automationProjectForm.automationType"><option>API</option><option>UI</option></select></div>
      </div>
      <div class="field"><label>Git 仓库地址 *</label><input v-model="automationProjectForm.repositoryUrl" /></div>
      <div class="two-col">
        <div class="field"><label>Branch</label><input v-model="automationProjectForm.branch" /></div>
        <div class="field"><label>Framework</label><input v-model="automationProjectForm.framework" /></div>
      </div>
      <div class="two-col">
        <div class="field"><label>Test Path</label><input v-model="automationProjectForm.testPath" /></div>
        <div class="field"><label>Git Credentials 授权</label><select v-model="automationProjectForm.credentialRef"><option value="">不使用项目凭证</option><option v-for="grant in credentialGrants" :key="grant.id" :value="grant.credentialRef">{{ grant.credentialRef }}{{ grant.credentialScopeRef ? ` · ${grant.credentialScopeRef}` : '' }}</option></select><small class="muted" v-if="credentialGrants.length === 0">当前项目暂无已授权凭证，请管理员先在系统管理中授权。</small></div>
      </div>
      <div class="two-col">
        <div class="field"><label>运行环境</label><input v-model="automationProjectForm.environment" /></div>
        <div class="field"><label>Jenkins Job</label><input v-model="automationProjectForm.jenkinsJob" /></div>
      </div>
      <div class="field"><label>执行命令</label><input v-model="automationProjectForm.executeCommand" /></div>
      <div class="modal-foot">
        <button type="button" class="btn" @click="closeModal">取消</button>
        <button class="btn primary">{{ editingAutomationProjectId ? '保存配置' : '创建自动化项目' }}</button>
      </div>
    </form>
  </BaseModal>
</template>
