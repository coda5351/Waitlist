<template>
  <div class="waitlist-page">
    <h2>{{ currentAction?.toLowerCase() == 'join' ? 'Join' : 'View' }} the Waitlist</h2>

    <p v-if="status">
      <em>Current status: <strong>{{ status.open ? 'Open' : 'Closed' }}</strong></em>
    </p>
    <p v-if="showWaitTimeMessage">
      <em>Approximate wait time: <strong>{{ status.estimatedWait }} minutes</strong></em>
    </p>

    <!-- show form if waitlist is enabled and open, and user doesn't have an active entry -->
    <div v-if="showWaitlistJoinForm" class="form-container">
      <div class="form-wrapper">
        <div class="form-card">
          <h1>Sign up</h1>
          <form @submit.prevent="submit">
            <p v-if="showDisabledMessage" class="alert">
              The waitlist has been disabled; your entry is no longer valid.
            </p>
            <div class="form-group">
              <label>Name:</label>
              <input v-model="form.name" />
            </div>
              <div class="form-group">
              <label>Phone:</label>
              <input v-model="form.phone" />
              <p v-if="form.phone && !isUSPhoneNumber(form.phone)" class="validation-error">
                Please enter a valid US phone number.
              </p>
            </div>
            <div class="form-group">
              <label>Party size:</label>
              <input type="number" v-model.number="form.partySize" min="1" />
            </div>
            <div class="form-group">
              <label>Waitlist Code:</label>
              <input type="text" disabled v-model.number="currentCode" />
            </div>
            <button :disabled="!isFormValid" type="submit">Join</button>
          </form>
        </div>
      </div>
    </div>

    <!-- if waitlist is disabled or closed, show message -->
    <div v-if="showDisabledMessageComputed || showDisabledMessage" class="ready-message warning-message" >
      <h3>The waitlist is currently closed.</h3>
      <p>Please check back later or ask the host for more information.</p>
    </div>

    <!-- show ready message if notified -->
    <div v-if="showReadyMessage" class="ready-message">
      <h3>Your table is ready -- please proceed to the host stand.</h3>
    </div>

    <!-- show left message if user has left the waitlist -->
    <div v-if="showLeftMessage" class="ready-message">
      <h3>You have left the waitlist -- have a nice day!</h3>
    </div>

    <!-- if user is missing from list show warning that they may have been called -->
    <div v-if="showMissingEntryMessage" class="ready-message warning-message">
      <h3>Your table has already been called or you are not on the list.</h3>
      <p>Sign up again to join the wait list.</p>
    </div>

    <!-- waitlist table -->
    <div v-if="showWaitlistTable && !showMissingEntryMessage" class="list">
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
import { WaitlistEvent, type WaitlistEventName } from '@/types/waitlistEvents'

const entries = ref<any[]>([])
let source: EventSource | null = null

function addSseListener(event: WaitlistEventName, handler: (e: MessageEvent) => void) {
  if (!source) return
  source.addEventListener(event, handler)
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
const form = ref({ name: '', phone: '', partySize: 1 })
const tableReady = ref(false)
const missingEntry = ref(false)
const listDisabledNotice = ref(false)
const showLeftMessage = ref(false)

const showWaitTimeMessage = computed(() => {
  return status.value && status.value.open && status.value.estimatedWait !== undefined && !tableReady.value
})
const showWaitlistJoinForm = computed(() => {
  return showWaitTimeMessage.value && isCurrentActionJoin.value
})
const showDisabledMessageComputed = computed(() => {
  return status.value && !status.value.open
})
const showReadyMessage = computed(() => {
  return tableReady.value && !isCurrentActionJoin.value
})
const showWaitlistTable = computed(() => {
  return entries.value.length > 0 && !isCurrentActionJoin.value && !tableReady.value
})
const showMissingEntryMessage = computed(() => {
  return missingEntry.value && !showLeftMessage.value
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

const isFormValid = computed(() => {
  return (
    form.value.name.trim() !== '' &&
    isUSPhoneNumber(form.value.phone) &&
    form.value.partySize > 0
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
    const resp = await api.delete(`/entries/${leaveTargetId.value}`)
    await throwIfNotOk(resp)
    entries.value = []
    showLeftMessage.value = true
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
      missingEntry.value = true
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
    form.value.partySize = 1
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

function checkEntryPresence() {
  // if we've gotten a ready notification or the list is disabled, don't mark missing
  if (tableReady.value || listDisabledNotice.value) {
    missingEntry.value = false
    return
  }
  if (currentCode.value) {
    missingEntry.value = !entries.value.some(e => e.code === currentCode.value)
  } else {
    missingEntry.value = false
  }
}

function initEventSource() {
  if (source) return; // already subscribed
  try {
    const streamUrl = getApiUrl('/entries/stream')
    source = new EventSource(streamUrl)
    addSseListener(WaitlistEvent.NEW_ENTRY, (e: MessageEvent) => {
      const payload = JSON.parse(e.data)
      const entry = payload.entry || payload
      if (payload.estimatedWait !== undefined) {
        // keep status estimate up-to-date
        if (status.value) status.value.estimatedWait = payload.estimatedWait
      }
      entries.value.push({ ...entry, idx: entries.value.length + 1, timestamp: formatTime(entry.timestamp) })
    })
    addSseListener(WaitlistEvent.DELETED_ENTRY, (e: MessageEvent) => {
      const payload = JSON.parse(e.data)
      const code = payload.code || String(e.data)
      if (payload.estimatedWait !== undefined && status.value) {
        status.value.estimatedWait = payload.estimatedWait
      }
      entries.value = entries.value.filter(r => r.code !== code)
      entries.value = entries.value.map((r,i) => ({ ...r, idx: i+1 }))
      checkEntryPresence()
    })
    addSseListener(WaitlistEvent.UPDATED_ENTRY, (e: MessageEvent) => {
      const payload = JSON.parse(e.data)
      const updated = payload.entry || payload
      if (payload.estimatedWait !== undefined && status.value) {
        status.value.estimatedWait = payload.estimatedWait
      }
      entries.value = entries.value.map(r => r.code === updated.code ? { ...r, ...updated, timestamp: formatTime(updated.timestamp) } : r)
      checkEntryPresence()
    })
    addSseListener(WaitlistEvent.NOTIFIED_ENTRY, (e: MessageEvent) => {
      const id = String(e.data)
      if (currentCode.value === id) {
        tableReady.value = true
      }
    })
    addSseListener(WaitlistEvent.WAITLIST_DISABLED, () => {
      // show notice but avoid duplicate popups
      listDisabledNotice.value = true
      tableReady.value = false
      fetchStatus()
    })
  } catch (ex) {
    console.warn('could not open event stream', ex)
    // fall back to polling
    setInterval(() => {
      fetchStatus()
    }, 30000)
  }
}

onMounted(async () => {
  if (currentCode.value) {
    fetchStatus()
    initEventSource()
  }
})

onUnmounted(() => {
  if (source) source.close()
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
  padding: 40px 20px 20px;
}
.form-wrapper {
  display: flex;
  gap: 30px;
  max-width: 900px;
  width: 100%;
}
.form-card {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  flex: 1;
  max-width: 400px;
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