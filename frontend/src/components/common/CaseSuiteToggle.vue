<script setup lang="ts">
type CaseSuiteType = 'FUNCTIONAL' | 'API'

const props = defineProps<{
  modelValue: CaseSuiteType
}>()

const emit = defineEmits<{
  'update:modelValue': [value: CaseSuiteType]
}>()

const options: Array<{ value: CaseSuiteType, label: string }> = [
  { value: 'FUNCTIONAL', label: '功能测试' },
  { value: 'API', label: '接口测试' }
]

function updateSuiteType(value: CaseSuiteType) {
  if (value !== props.modelValue) {
    emit('update:modelValue', value)
  }
}
</script>

<template>
  <div class="case-suite-toggle" role="group" aria-label="测试范围切换">
    <div class="case-suite-toggle-track" :class="{ api: modelValue === 'API' }">
      <span class="case-suite-toggle-thumb" aria-hidden="true"></span>
      <button
        v-for="option in options"
        :key="option.value"
        type="button"
        class="case-suite-toggle-btn"
        :class="{ active: modelValue === option.value }"
        :aria-pressed="modelValue === option.value"
        @click="updateSuiteType(option.value)"
      >
        {{ option.label }}
      </button>
    </div>
  </div>
</template>
