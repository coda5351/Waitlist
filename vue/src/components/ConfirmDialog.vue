<template>
  <teleport to="body">
    <div class="confirm-overlay" @click.self="cancel">
      <div class="confirm-dialog" role="dialog" aria-modal="true" :aria-label="title || 'Confirm'">
        <h3 class="confirm-title" v-if="title">{{ title }}</h3>
        <div class="confirm-message">{{ message }}</div>
        <div class="confirm-actions">
          <button class="btn btn-cancel" @click="cancel">{{ cancelLabel }}</button>
          <button class="btn btn-confirm" @click="confirmAction">{{ confirmLabel }}</button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'

const props = defineProps({
  title: { type: String, required: false, default: '' },
  message: { type: String, required: true },
  confirmLabel: { type: String, required: false, default: 'OK' },
  cancelLabel: { type: String, required: false, default: 'Cancel' },
})

const emit = defineEmits(['confirm', 'cancel'])

function cancel() {
  emit('cancel')
}
function confirmAction() {
  emit('confirm')
}

// Close on Escape
function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') cancel()
}

onMounted(() => document.addEventListener('keydown', onKey))
onUnmounted(() => document.removeEventListener('keydown', onKey))
</script>

<style scoped>
.confirm-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,0.36);
  z-index: 2000;
}
.confirm-dialog {
  background: #fff;
  border-radius: 8px;
  padding: 1rem 1.2rem;
  max-width: 420px;
  width: calc(100% - 40px);
  box-shadow: 0 12px 36px rgba(0,0,0,0.18);
}
.confirm-title { margin: 0 0 0.4rem 0; font-size: 1.05rem }
.confirm-message { margin-bottom: 0.8rem; color: rgba(0,0,0,0.8) }
.confirm-actions { display:flex; gap: 0.6rem; justify-content: flex-end }
.btn { padding: 0.45rem 0.8rem; border-radius: 6px; border: none; cursor: pointer }
.btn-cancel { background: #f1f1f1 }
.btn-confirm { background: #e3342f; color: #fff }
</style>
