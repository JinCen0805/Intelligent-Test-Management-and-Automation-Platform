<script setup lang="ts">
import { useUiStore } from '@/stores/ui'

defineProps<{ title: string }>()

const ui = useUiStore()
const { closeModal } = ui
</script>

<template>
  <div class="modal-backdrop" @click.self="closeModal">
    <div class="modal">
      <div class="modal-head">
        <h3>{{ title }}</h3>
        <button class="icon-btn" @click="closeModal">×</button>
      </div>
      <slot />
    </div>
  </div>
</template>
