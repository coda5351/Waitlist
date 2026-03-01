<template>
  <div>
    <div class="info-section">
      <h3>Account Messages</h3>
      <p>Customize the templates used for notifications (SMS/email). Use <code>%s</code> placeholders as needed in the text.</p>
    </div>

    <div class="info-section">
      <div v-for="(entry, idx) in entries" :key="idx" class="message-row">
        <div class="field">
          <label>Key</label>
          <input type="text" v-model="entry.key" placeholder="message key" />
        </div>
        <div class="field">
          <label>Template</label>
          <textarea v-model="entry.value" rows="3" />
        </div>
        <button class="btn-remove" @click="removeRow(idx)">Remove</button>
      </div>
      <button class="btn-add" @click="addRow">Add message</button>
    </div>

    <div class="actions">
      <button class="btn-save" @click="saveMessages">Save</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useStore } from 'vuex'
import { api, throwIfNotOk } from '@/utils/api'
import { error as notifyError, success as notifySuccess } from '@/utils/notify'

const store = useStore()
const accountId = computed(() => store.getters.user?.account?.id)

interface MessageEntry { key: string; value: string }
const entries = ref<MessageEntry[]>([])

async function load() {
  try {
    if (!accountId.value) return
    const resp = await api.get(`/accounts/${accountId.value}/messages`)
    if (resp.ok) {
      const data = await resp.json()
      entries.value = Object.entries(data).map(([k, v]) => ({ key: k, value: String(v) }))
    }
  } catch (err) {
    console.error('failed to load messages', err)
  }
}

function addRow() {
  entries.value.push({ key: '', value: '' })
}

function removeRow(idx: number) {
  entries.value.splice(idx, 1)
}

async function saveMessages() {
  const payload: Record<string,string> = {}
  entries.value.forEach(e => {
    if (e.key && e.key.trim()) {
      payload[e.key.trim()] = e.value || ''
    }
  })
  try {
    if (!accountId.value) return
    const resp = await api.patch(`/accounts/${accountId.value}/messages`, payload)
    await throwIfNotOk(resp)
    notifySuccess('Messages saved', { timeout: 2000 })
  } catch (err:any) {
    notifyError(err?.message || 'Failed to save messages', { timeout: false })
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.message-row {
  border: 1px solid #ddd;
  padding: 1rem;
  margin-bottom: 1rem;
  position: relative;
}
.field {
  margin-bottom: 0.5rem;
}
.field label {
  display: block;
  font-weight: 500;
  margin-bottom: 0.25rem;
}
.field input,
.field textarea {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  box-sizing: border-box;
}
.btn-remove {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  background: #ff6b6b;
  color: #fff;
  border: none;
  border-radius: 4px;
  padding: 0.25rem 0.5rem;
  cursor: pointer;
}
.btn-add {
  margin-top: 0.5rem;
  padding: 0.5rem 1rem;
  background: var(--theme-color, #42b983);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.actions {
  margin-top: 1rem;
}
.btn-save {
  padding: 0.5rem 1rem;
  background-color: var(--theme-color, #42b983);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
</style>
