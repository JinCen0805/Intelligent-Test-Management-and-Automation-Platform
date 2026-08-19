<script setup lang="ts">
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useProjectStore } from '@/stores/project'
import { useAdminStore } from '@/stores/admin'
import BaseModal from '@/components/common/BaseModal.vue'

const ui = useUiStore()
const { closeModal } = ui
const projectStore = useProjectStore()
const { memberForm, projectMembers } = storeToRefs(projectStore)
const { addProjectMember } = projectStore
const adminStore = useAdminStore()
const { memberCandidates } = storeToRefs(adminStore)
const { loadMemberCandidates } = adminStore

onMounted(() => { void loadMemberCandidates() })
</script>

<template>
  <BaseModal title="添加项目成员">
    <form class="modal-body" @submit.prevent="addProjectMember">
      <div class="field">
        <label>用户</label>
        <select v-model.number="memberForm.userId">
          <option :value="null" disabled>请选择启用用户</option>
          <option v-for="user in memberCandidates.filter((item) => !projectMembers.some((member) => member.userId === item.id))" :key="user.id" :value="user.id">{{ user.displayName }} · {{ user.username }}</option>
        </select>
      </div>
      <div class="field"><label>项目角色</label><select v-model="memberForm.role"><option value="MANAGER">项目管理员</option><option value="TEST_DEVELOPER">测试开发</option><option value="VIEWER">只读成员</option></select></div>
      <div class="modal-foot">
        <button type="button" class="btn" @click="closeModal">取消</button>
        <button class="btn primary">添加成员</button>
      </div>
    </form>
  </BaseModal>
</template>
