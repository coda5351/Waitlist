<template>
  <div :class="['ready-message', variantClass]">
    <h3>{{ message }}</h3>
    <p v-if="subMessage">{{ subMessage }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { WaitlistEvent } from '@/types/waitlistEvents'

const props = defineProps<{
    message: string
    subMessage?: string
    eventName?: string
}>()

const variantClass = computed(() => {
  switch (props.eventName) {
    case WaitlistEvent.WAITLIST_DISABLED:
    case WaitlistEvent.DELETED_ENTRY:
      return 'warning-message'
    default:
      return ''
  }
})
</script>
