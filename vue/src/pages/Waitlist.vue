<template>
  <div class="waitlist-page">
    <h2>{{ currentAction?.toLowerCase() == 'join' ? 'Join' : 'View' }} the Waitlist</h2>

    <div v-if="status">
      <p>
        <em>We are currently <strong>{{ status.open ? 'open' : 'closed' }}</strong>.</em><br />
        <em>Approximate wait time: <strong>{{ status.estimatedWait }} minutes</strong></em>
      </p>
    </div>

    <div v-if="showWaitlistJoinForm" class="form-container">
      <div class="form-wrapper">
        <div class="form-card">
          <h1>Sign up</h1>
          <form @submit.prevent="submit">
            <div class="form-group">
              <input v-model="form.name" placeholder="Name" />
            </div>
            <div class="form-group">
              <input v-model="form.phone" @blur="formatPhone" placeholder="Phone" />
              <p v-if="form.phone && !isUSPhoneNumber(form.phone)" class="validation-error">
                Please enter a valid US phone number.
              </p>
            </div>
            <div class="form-group">
              <input type="number" v-model.number="form.partySize" min="1" placeholder="Party size" />
            </div>
            <input type="hidden" :value="currentCode" />
            <button :disabled="!isFormValid" type="submit">Join</button>
          </form>
        </div>
      </div>
    </div>

    <StatusMessage
      v-if="statusMessage?.showMessage"
      :message="statusMessage?.message"
      :subMessage="statusMessage?.subMessage"
      :eventName="statusMessage?.eventName"
    />

    <div v-if="showWaitlistTable" class="list">
      <h3>Current Waitlist</h3>
      <DataTable :columns="columns" :rows="entries">
        <template #row="{ row }">
          <tr :class="{ 'highlighted-row': currentCode && row.code === currentCode }">
            <td>
              <button v-if="currentCode && row.code === currentCode"
                      @click="confirmLeave(row.code)"
                      class="btn-cancel"
                      style="margin-left:0.5rem;"
                      title="Leave the waitlist">
                Leave
              </button>
            </td>
            <td>{{ row.idx }}</td>
            <td>
              {{ (currentCode && row.code !== currentCode)
                  ? row.name.slice(0,2) + '...' 
                  : row.name }}
            </td>
            <td>{{ row.partySize }}</td>
            <td>{{ row.timestamp }}</td>
          </tr>
        </template>
      </DataTable>
    </div>

    <!-- leave confirmation modal -->
    <div v-if="leaveDialogVisible" class="modal-overlay">
      <div class="modal-content">
        <h3>Confirm</h3>
        <p>Are you sure you want to leave your spot on the waitlist?</p>
        <div class="modal-actions no-gap">
          <button class="btn-notify btn-danger" @click="performLeave">Yes, leave</button>
          <button class="btn-cancel" @click="leaveDialogVisible = false">Cancel</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, throwIfNotOk, getApiUrl } from '@/utils/api'
import DataTable from '@/components/DataTable.vue'
import StatusMessage from '@/components/StatusMessage.vue'
import { WaitlistEvent } from '@/types/waitlistEvents'
import { useWaitlistSse } from '@/composables/useWaitlistSse'
import type { StatusMessage as StatusMessageType } from '@/types/statusMessage'

const entries = ref<any[]>([])
const { init: initSse, stop: stopSse } = useWaitlistSse()
let statusPollInterval: number | null = null

function stopRealtimeUpdates() {
  stopSse()
  if (statusPollInterval !== null) {
    clearInterval(statusPollInterval)
    statusPollInterval = null
  }
}

const leaveDialogVisible = ref(false)
const leaveTargetId = ref<string | null>(null)
const columns = [
  { key: '', label: '' },
  { key: 'idx', label: '#' },
  { key: 'name', label: 'Name' },
  { key: 'partySize', label: 'Party Of' },
  { key: 'timestamp', label: 'Check in time' }
]
import { formatTime } from '@/utils/dateFormatter'
const status = ref<any>(null)
const showDisabledMessage = ref(false)
const wasEnabled = ref<boolean | null>(null)
const form = ref({ name: '', phone: '', partySize: null as number | null })
const listDisabledNotice = ref(false)
const statusMessage = ref<any | null>(null)

const showWaitlistJoinForm = computed(() => {
  return currentAction.value?.toLowerCase() === 'join' && !statusMessage.value?.showMessage
})
const showWaitlistTable = computed(() => {
  return currentAction.value?.toLowerCase() === 'view' && !statusMessage.value?.showMessage
})

const route = useRoute()
const router = useRouter()
const currentCode = computed(() => {
  if (route.params.code) {
    return String(route.params.code)
  }
  return route.query.code ? String(route.query.code) : null
})
const currentAction = computed(() => {
  if (route.params.action) {
    return String(route.params.action)
  }
  return route.params.action ? String(route.params.action) : null
})
const isCurrentActionJoin = computed(() => currentAction.value?.toLowerCase() === 'join')


// simple US phone number validator (allows formats like 123-456-7890, (123) 456-7890, 1234567890)
function isUSPhoneNumber(phone: string) {
  const pattern = /^\(?([0-9]{3})\)?[-.\s]?([0-9]{3})[-.\s]?([0-9]{4})$/
  return pattern.test(phone.trim())
}

// format phone number on blur (e.g. 4155551212 -> (415) 555-1212)
function formatPhone() {
  if (!form.value.phone) return
  const digits = form.value.phone.replace(/\D/g, '')
  if (digits.length === 10) {
    form.value.phone = `(${digits.slice(0, 3)}) ${digits.slice(3, 6)}-${digits.slice(6)}`
  } else {
    // keep digits only for partial/invalid lengths
    form.value.phone = digits
  }
}

const isFormValid = computed(() => {
  return (
    form.value.name.trim() !== '' &&
    isUSPhoneNumber(form.value.phone) &&
    (form.value.partySize !== null && form.value.partySize > 0)
  )
})

// allow the user to leave their own spot
function confirmLeave(code: string) {
  leaveTargetId.value = code
  leaveDialogVisible.value = true
}

async function performLeave() {
  if (!leaveTargetId.value) return
  try {
    stopRealtimeUpdates()
    const resp = await api.delete(`/entries/${leaveTargetId.value}`)
    console.log('leave response', resp)
    await throwIfNotOk(resp)
    entries.value = []
    const payload = await resp.json()
    statusMessage.value = {
        showMessage: true,
        message: payload?.message,
        subMessage: payload?.subMessage,
        eventName: payload?.eventName
      } as StatusMessageType
  } catch (e) {
    console.error('leave spot failed', e)
  } finally {
    leaveDialogVisible.value = false
    leaveTargetId.value = null
  }
}

async function fetchStatus() {
  try {
    const url = `/waitlists/${isCurrentActionJoin.value ? '' : 'entry/'}${currentCode.value}/status`
    const resp = await api.get(url)
    if (resp.ok) {
      const newStatus = await resp.json()
      // detect transition from enabled->disabled when user has an entry
      if (wasEnabled.value && newStatus && !newStatus.enabled && currentCode.value) {
        showDisabledMessage.value = true
      }
      status.value = newStatus
      wasEnabled.value = newStatus.enabled
      // clear notice if it has reopened
      if (newStatus.enabled && newStatus.open) {
        listDisabledNotice.value = false
      }
      entries.value = newStatus.entries?.map((e: any, i: number) => ({ ...e, idx: i + 1, timestamp: formatTime(e.timestamp) })) || []
    } else if (resp.status === 404) {
      // if we get a 404 it means the entry is missing, which could be because they were called or never existed
      statusMessage.value = {
        showMessage: true,
        message: "We couldn't find your spot on the waitlist.",
        subMessage: "If you were recently called, please contact the host. Otherwise, feel free to join again!",
        eventName: WaitlistEvent.DELETED_ENTRY
      } as StatusMessageType
    }
  } catch (err: Promise<Response> | any) {
    console.error('fetch status failed', err)
  }
}

async function submit() {
  try {
    const resp = await api.post(`/entries/create/${currentCode.value}`, form.value)
    await throwIfNotOk(resp)
    const json = await resp.json()
    form.value.name = ''
    form.value.phone = ''
    form.value.partySize = null
    if (json && json.code) {
      // update URL to include the code as a path segment (and clear query param)
      router.replace({ path: `/waitlist/view/${json.code}` })
      // subscribe to updates now that we have an id
      initEventSource()
    }
  } catch (err: any) {
    console.error(err)
    const msg = err?.message || ''
    if (msg.toLowerCase().includes('disabled')) {
      // show notice and hide form by marking status disabled
      listDisabledNotice.value = true
      if (status.value) {
        status.value.enabled = false
        status.value.open = false
      }
    }
  }
}

function getEventMessageForEntryAndSetWaitTime(e: MessageEvent) {
  try {
    const payload = JSON.parse(e.data)
    if (payload.estimatedWait !== undefined && status.value) {
      status.value.estimatedWait = payload.estimatedWait
    }
    const eventEntryCode = payload.entry?.code || null
    if (payload.eventName === WaitlistEvent.WAITLIST_DISABLED) {
      statusMessage.value = {
        showMessage: true,
        message: payload.message,
        subMessage: payload.subMessage,
        eventName: payload.eventName
      } as StatusMessageType
      status.value.open = false
    } else if (eventEntryCode && eventEntryCode === currentCode.value) {
      statusMessage.value = {
        showMessage: true,
        message: payload.message,
        subMessage: payload.subMessage,
        eventName: payload.eventName
      } as StatusMessageType
    }
  } catch {
    statusMessage.value = {
      showMessage: true,
      message: "Could not parse update message",
      subMessage: null,
      eventName: null
    } as unknown as StatusMessageType
  }
}

function initEventSource() {
  const streamUrl = getApiUrl('/entries/stream')
  initSse(streamUrl, {
    onNewEntry(payload) {
      const entry = payload.entry || payload
      entries.value.push({ ...entry, idx: entries.value.length + 1, timestamp: formatTime(entry.timestamp) })
      getEventMessageForEntryAndSetWaitTime({ data: JSON.stringify(payload) } as MessageEvent)
    },
    onDeletedEntry(payload) {
      const deletedEntry = payload.entry || payload.code || payload
      entries.value = entries.value.filter(r => r.code !== deletedEntry.code)
      entries.value = entries.value.map((r, i) => ({ ...r, idx: i + 1 }))
      getEventMessageForEntryAndSetWaitTime({ data: JSON.stringify(payload) } as MessageEvent)
    },
    onUpdatedEntry(payload) {
      const updated = payload.entry || payload
      entries.value = entries.value.filter(r => r.code !== updated.code)
      getEventMessageForEntryAndSetWaitTime({ data: JSON.stringify(payload) } as MessageEvent)
    },
    onNotifiedEntry(payload) {
      const notified = payload.entry || payload
      entries.value = entries.value.filter(r => r.code !== notified.code)
      getEventMessageForEntryAndSetWaitTime({ data: JSON.stringify(payload) } as MessageEvent)
    },
    onWaitlistDisabled(payload) {
      getEventMessageForEntryAndSetWaitTime({ data: JSON.stringify(payload) } as MessageEvent)
      fetchStatus()
    },
    onError(err) {
      console.warn('could not open event stream', err)
      statusPollInterval = window.setInterval(() => fetchStatus(), 30000)
    }
  })
}

onMounted(async () => {
  if (currentCode.value) {
    fetchStatus()
    initEventSource()
  }
})

onUnmounted(() => {
  stopRealtimeUpdates()
})
</script>

<style scoped>
.waitlist-page {
  padding: 1rem;
}

.form-container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: 100vh;
  padding: 20px 20px 0;
}
.form-wrapper {
  display: flex;
  justify-content: center;
  gap: 30px;
  max-width: 900px;
  width: 100%;
}

.form-card {
  margin: 0 auto;
  width: 100%;
  max-width: 420px;
  background: white;
  padding: 32px;
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.form-card h1 {
  margin-bottom: 1rem;
  font-size: 1.6rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  font-size: 0.95rem;
  color: #444;
}

input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
  transition: border-color 0.3s;
}


.list table {
  width: 100%;
  border-collapse: collapse;
}

.highlighted-row {
  color: var(--theme-color, #42b983);
  background-color: rgba(66, 185, 131, 0.1); /* 10% opacity of default green */
}
.list th, .list td {
  border: 1px solid #ddd;
  padding: 0.5rem;
}
.list th {
  background: #f5f5f5;
}

/* make table wrapper 50% wide on larger screens */
.list .table-wrapper {
  width: 50%;
  margin: 0 auto;
}


label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #555;
}

input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
  transition: border-color 0.3s;
}

input:focus {
  outline: none;
  border-color: var(--theme-color, #4CAF50);
}

button {
  width: 100%;
  margin-top: 16px;
  padding: 14px;
  background-color: var(--theme-color, #4CAF50);
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s;
}

button:hover:not(:disabled) {
  opacity: 0.9;
}

button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.validation-error {
  color: #d32f2f;
  font-size: 0.9rem;
  margin-top: 4px;
}

.register-section {
  flex: 1;
}

@media (max-width: 600px) {
  .list .table-wrapper {
    width: 100%;
  }
}

.ready-message {
  text-align: center;
  padding: 2rem;
  background: #e0ffe0;
  border: 1px solid #4CAF50;
  border-radius: 4px;
  color: #2e7d32;
  font-size: 1.2rem;
}
.warning-message {
  background: #fff3cd;
  border-color: #ffeeba;
  color: #856404;
}

.no-gap {
  padding-top: 0;
}

/* danger button style */
.btn-danger {
  background-color: #d32f2f;
}
.btn-danger:hover:not(:disabled) {
  opacity: 0.85;
}
</style>