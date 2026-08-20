<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useAdminStore } from '@/stores/admin'
import BaseModal from '@/components/common/BaseModal.vue'

const ui = useUiStore()
const { closeModal } = ui
const adminStore = useAdminStore()
const { userForm, editingUserId } = storeToRefs(adminStore)
const { saveUser } = adminStore

const title = computed(() => (editingUserId.value ? '编辑用户' : '新建用户'))
</script>

<template>
  <BaseModal :title="title">
    <form class="modal-body" @submit.prevent="saveUser">
      <div class="field"><label>用户名 *</label><input v-model="userForm.username" autocomplete="off" /></div>
      <div class="two-col">
        <div class="field"><label>显示名称 *</label><input v-model="userForm.displayName" /></div>
        <div class="field"><label>角色</label><select v-model="userForm.role"><option value="TEST_DEVELOPER">测试开发</option><option value="ADMIN">管理员</option></select></div>
      </div>
      <div class="field"><label>邮箱</label><input v-model="userForm.email" type="email" /></div>
      <div class="field"><label>{{ editingUserId ? '重置密码（可选）' : '初始密码 *' }}</label><input v-model="userForm.password" type="password" autocomplete="new-password" /></div>
      <div class="modal-foot">
        <button type="button" class="btn" @click="closeModal">取消</button>
        <button class="btn primary">{{ editingUserId ? '保存用户' : '创建用户' }}</button>
      </div>
    </form>
  </BaseModal>
</template>
