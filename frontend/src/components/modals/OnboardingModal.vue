<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'
import { useAutomationStore } from '@/stores/automation'
import BaseModal from '@/components/common/BaseModal.vue'

const ui = useUiStore()
const { closeModal } = ui
const automationStore = useAutomationStore()
const {
  onboarding,
  onboardingLoading,
  onboardingProgress,
  onboardingReadySteps,
  onboardingTotalSteps
} = storeToRefs(automationStore)
const {
  onboardingProject,
  onboardingStatusLabel,
  onboardingStepMeta,
  onboardingStepStatusLabel,
  stepDuration,
  retryOnboarding,
  cancelOnboarding
} = automationStore
</script>

<template>
  <BaseModal title="初始化自动化">
    <div class="modal-body">
      <div class="detail-grid">
        <div><span class="muted">自动化项目</span><strong>{{ onboardingProject()?.name || '--' }}</strong></div>
        <div><span class="muted">初始化编号</span><strong>{{ onboarding ? onboarding.onboardingId.slice(0, 8) : '--' }}</strong></div>
        <div><span class="muted">整体状态</span><strong>{{ onboarding ? onboardingStatusLabel(onboarding.status) : '启动中...' }}</strong></div>
        <div><span class="muted">重试次数</span><strong>{{ onboarding?.retryCount ?? 0 }}</strong></div>
      </div>
      <div v-if="onboarding?.errorMessage" class="auth-error">失败原因：{{ onboarding.errorMessage }}<span v-if="onboarding.errorCode" class="muted">（{{ onboarding.errorCode }}）</span></div>
      <div class="progress"><i :style="{ width: `${onboardingProgress}%` }"></i></div>
      <p class="muted">{{ onboardingProgress }}% · 已完成 {{ onboardingReadySteps }} / {{ onboardingTotalSteps }} 步</p>
      <div class="table-wrap">
        <table>
          <thead><tr><th>步骤</th><th>责任角色</th><th>状态</th><th>耗时</th><th>重试</th></tr></thead>
          <tbody>
            <tr v-for="step in (onboarding?.steps ?? [])" :key="step.id">
              <td><strong>{{ onboardingStepMeta(step.stepKey).label }}</strong><br><span class="muted">{{ step.stepKey }}</span></td>
              <td>{{ onboardingStepMeta(step.stepKey).role }}</td>
              <td><span class="tag" :class="step.status === 'READY' ? 'green' : step.status === 'FAILED' ? 'red' : step.status === 'RUNNING' ? 'blue' : 'gray'">{{ onboardingStepStatusLabel(step.status) }}</span></td>
              <td>{{ stepDuration(step) }}</td>
              <td>{{ step.retryCount }}</td>
            </tr>
            <tr v-if="!onboarding"><td colspan="5" class="muted">正在启动初始化...</td></tr>
          </tbody>
        </table>
      </div>
      <div class="modal-foot">
        <button class="btn" @click="closeModal">关闭</button>
        <button v-if="onboarding?.status === 'FAILED'" class="btn primary" :disabled="onboardingLoading" @click="retryOnboarding">重试</button>
        <button v-if="['CREATED', 'QUEUED', 'RUNNING'].includes(onboarding?.status ?? '')" class="btn" @click="cancelOnboarding">取消初始化</button>
      </div>
    </div>
  </BaseModal>
</template>
