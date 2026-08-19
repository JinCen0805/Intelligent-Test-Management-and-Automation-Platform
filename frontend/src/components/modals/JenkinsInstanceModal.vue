<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useAdminStore } from '@/stores/admin'
import BaseModal from '@/components/common/BaseModal.vue'

const ui = useUiStore()
const { closeModal } = ui
const adminStore = useAdminStore()
const { jenkinsInstanceForm, editingJenkinsInstanceId } = storeToRefs(adminStore)
const { saveJenkinsInstance } = adminStore

const title = computed(() => (editingJenkinsInstanceId.value ? '编辑 Jenkins 实例' : '新建 Jenkins 实例'))
</script>

<template>
  <BaseModal :title="title">
    <form class="modal-body" @submit.prevent="saveJenkinsInstance">
      <div class="two-col">
        <div class="field"><label>实例名称 *</label><input v-model="jenkinsInstanceForm.name" /></div>
        <div class="field"><label>Jenkins 地址 *</label><input v-model="jenkinsInstanceForm.baseUrl" placeholder="http://127.0.0.1:9090" /></div>
      </div>
      <div class="two-col">
        <div class="field"><label>服务账号 *</label><input v-model="jenkinsInstanceForm.username" /></div>
        <div class="field"><label>API Token {{ editingJenkinsInstanceId ? '（留空保持不变）' : '*' }}</label><input v-model="jenkinsInstanceForm.apiToken" type="password" autocomplete="new-password" /></div>
      </div>
      <div class="two-col">
        <div class="field"><label>连接超时 (ms)</label><input v-model.number="jenkinsInstanceForm.connectTimeoutMs" type="number" /></div>
        <div class="field"><label>读取超时 (ms)</label><input v-model.number="jenkinsInstanceForm.readTimeoutMs" type="number" /></div>
      </div>
      <div class="two-col">
        <div class="field"><label>轮询间隔 (ms)</label><input v-model.number="jenkinsInstanceForm.pollIntervalMs" type="number" /></div>
        <div class="field"><label>最大等待 (分钟)</label><input v-model.number="jenkinsInstanceForm.maxWaitMinutes" type="number" /></div>
      </div>
      <div class="modal-foot">
        <button type="button" class="btn" @click="closeModal">取消</button>
        <button class="btn primary">{{ editingJenkinsInstanceId ? '保存修改' : '创建实例' }}</button>
      </div>
    </form>
  </BaseModal>
</template>
