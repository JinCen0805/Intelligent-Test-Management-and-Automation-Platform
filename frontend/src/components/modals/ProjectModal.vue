<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useProjectStore } from '@/stores/project'
import BaseModal from '@/components/common/BaseModal.vue'

const ui = useUiStore()
const { closeModal } = ui
const projectStore = useProjectStore()
const { newProject } = storeToRefs(projectStore)
const { createProject } = projectStore
</script>

<template>
  <BaseModal title="创建项目">
    <form class="modal-body" @submit.prevent="createProject">
      <div class="two-col">
        <div class="field"><label>项目名称 *</label><input v-model="newProject.name" placeholder="例如：星河电商平台" /></div>
        <div class="field"><label>项目标识 *</label><input v-model="newProject.code" placeholder="例如：XH-MALL" /></div>
      </div>
      <div class="field"><label>项目描述</label><textarea v-model="newProject.description" placeholder="描述项目范围、业务目标与测试重点"></textarea></div>
      <div class="modal-foot">
        <button type="button" class="btn" @click="closeModal">取消</button>
        <button class="btn primary">创建项目</button>
      </div>
    </form>
  </BaseModal>
</template>
