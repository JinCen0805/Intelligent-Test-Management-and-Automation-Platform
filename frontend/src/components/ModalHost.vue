<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import ProjectModal from '@/components/modals/ProjectModal.vue'
import RequirementUploadModal from '@/components/modals/RequirementUploadModal.vue'
import RequirementDetailModal from '@/components/modals/RequirementDetailModal.vue'
import RunModal from '@/components/modals/RunModal.vue'
import CaseEditModal from '@/components/modals/CaseEditModal.vue'
import ModelModal from '@/components/modals/ModelModal.vue'
import PromptModal from '@/components/modals/PromptModal.vue'
import AutomationProjectModal from '@/components/modals/AutomationProjectModal.vue'
import TaskModal from '@/components/modals/TaskModal.vue'
import AiTaskModal from '@/components/modals/AiTaskModal.vue'
import UserModal from '@/components/modals/UserModal.vue'
import MemberModal from '@/components/modals/MemberModal.vue'
import JenkinsInstanceModal from '@/components/modals/JenkinsInstanceModal.vue'
import BindingModal from '@/components/modals/BindingModal.vue'
import JobImportModal from '@/components/modals/JobImportModal.vue'
import CreateJobModal from '@/components/modals/CreateJobModal.vue'
import OnboardingModal from '@/components/modals/OnboardingModal.vue'
import ExecutionDetailModal from '@/components/modals/ExecutionDetailModal.vue'
import AnalysisModal from '@/components/modals/AnalysisModal.vue'
import CaseChatHistoryModal from '@/components/modals/CaseChatHistoryModal.vue'

const { modal } = storeToRefs(useUiStore())
</script>

<template>
  <ProjectModal v-if="modal === 'project'" />
  <RequirementUploadModal v-else-if="modal === 'requirement'" />
  <RequirementDetailModal v-else-if="modal === 'requirementDetail'" />
  <RunModal v-else-if="modal === 'run'" />
  <CaseEditModal v-else-if="modal === 'caseEdit'" />
  <ModelModal v-else-if="modal === 'model'" />
  <PromptModal v-else-if="modal === 'prompt'" />
  <AutomationProjectModal v-else-if="modal === 'automationProject'" />
  <TaskModal v-else-if="modal === 'task'" />
  <AiTaskModal v-else-if="modal === 'aiTask'" />
  <UserModal v-else-if="modal === 'user'" />
  <MemberModal v-else-if="modal === 'member'" />
  <JenkinsInstanceModal v-else-if="modal === 'jenkinsInstance'" />
  <BindingModal v-else-if="modal === 'binding'" />
  <JobImportModal v-else-if="modal === 'jobImport'" />
  <CreateJobModal v-else-if="modal === 'createJob'" />
  <OnboardingModal v-else-if="modal === 'onboarding'" />
  <ExecutionDetailModal v-else-if="modal === 'executionDetail'" />
  <AnalysisModal v-else-if="modal === 'analysis'" />
  <CaseChatHistoryModal v-else-if="modal === 'caseChatHistory'" />
</template>
