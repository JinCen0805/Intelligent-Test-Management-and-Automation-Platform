<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import BaseModal from '@/components/common/BaseModal.vue'
import { useAiStore } from '@/stores/ai'
import { useUiStore } from '@/stores/ui'

const aiStore = useAiStore()
const { caseChatHistory, activeCaseChatSessionId } = storeToRefs(aiStore)
const { loadCaseChatHistory, restoreCaseChatSession, deleteCaseChatSession, deleteAllCaseChatSessions } = aiStore
const { activeCaseSuiteType } = storeToRefs(useUiStore())

const currentSessionTitle = computed(() => {
  const active = caseChatHistory.value.find((item) => item.sessionId === activeCaseChatSessionId.value)
  return active?.title || '当前对话'
})
const suiteLabel = computed(() => activeCaseSuiteType.value === 'API' ? '接口测试' : '功能测试')

function openSession(sessionId: number) {
  restoreCaseChatSession(sessionId)
}

function handleDelete(sessionId: number) {
  void deleteCaseChatSession(sessionId)
}

function handleDeleteAll() {
  void deleteAllCaseChatSessions()
}
</script>

<template>
  <BaseModal title="历史对话">
    <div class="modal-body case-chat-history">
      <div class="case-chat-history-head">
        <div>
          <strong>当前会话</strong>
          <span class="tag blue">{{ suiteLabel }}</span>
          <p class="muted">{{ currentSessionTitle }}</p>
        </div>
        <div class="head-actions">
          <button class="btn danger-text" :disabled="caseChatHistory.length === 0" @click="handleDeleteAll">全部删除</button>
          <button class="btn" @click="loadCaseChatHistory(false)">刷新</button>
        </div>
      </div>

      <div v-if="caseChatHistory.length" class="case-chat-history-list">
        <div v-for="session in caseChatHistory" :key="session.sessionId" class="case-chat-history-item" :class="{ active: session.sessionId === activeCaseChatSessionId }">
          <div class="case-chat-history-meta" @click="openSession(session.sessionId)">
            <strong>{{ session.title }}</strong>
            <span>{{ new Date(session.updatedAt).toLocaleString('zh-CN') }}</span>
          </div>
          <div class="head-actions">
            <button class="btn" @click="openSession(session.sessionId)">恢复</button>
            <button class="btn danger-text" @click="handleDelete(session.sessionId)">删除</button>
          </div>
        </div>
      </div>
      <div v-else class="muted">暂无历史对话</div>
    </div>
  </BaseModal>
</template>
