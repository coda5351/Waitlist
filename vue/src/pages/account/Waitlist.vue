<template>
  <div>
    <h3>Waitlist Settings</h3>
    <p>Configure the account's waitlist opening and closing schedule.</p>

    <p v-if="status">
      <strong>Status:</strong> {{ status.enabled ? (status.open ? 'Open' : 'Closed') : 'Disabled' }}
    </p>

    <div v-if="waitlistActive">
      <button class="btn-save" @click="confirmDisableWaitlist">Disable Waitlist</button>
    </div>
    <template v-else>
      <label>
        <input type="checkbox" v-model="waitlistEnabled" />
        Waitlist enabled
      </label>
      <div v-if="waitlistEnabled" class="schedule-fields">
        <div class="field">
          <label>Open date:</label>
          <input type="date" v-model="openDate" />
        </div>
        <div class="field">
          <label>Open time:</label>
          <input type="time" v-model="openTime" />
        </div>
        <div class="field">
          <label>Close date:</label>
          <input type="date" v-model="closeDate" />
        </div>
        <div class="field">
          <label>Close time:</label>
          <input type="time" v-model="closeTime" />
        </div>
      </div>
      <div class="actions">
        <button class="btn-save" @click="saveWaitlistSettings">Save</button>
      </div>
    </template>

    <div class="entries-list" v-if="waitlistActive">
      <h3>Current Waitlist Entries</h3>
      <template v-if="entries.length">
        <DataTable :columns="columns" :rows="entries">
          <template #row="{ row }">
            <tr>
              <td>
                <button @click="openNotifyModal(row.code)" class="btn-notify">Notify</button>
              </td>
              <td>{{ row.idx }}</td>
              <td>{{ row.name }}</td>
                    <td>{{ formatPhoneNumber(row.phone) }}</td>
              <td>{{ row.partySize }}</td>
              <td>{{ row.timestamp }}</td>
            </tr>
          </template>
        </DataTable>
      </template>
      <p v-else class="no-entries-msg">
        The waitlist is enabled but there are no entries yet.
      </p>
    </div>

    <!-- notification modal -->
    <div v-if="modalVisible" class="modal-overlay">
      <div class="modal-content">
        <h3>Notify Customer</h3>
        <div v-if="modalEntry">
          <ul class="entry-info">
            <li><span class="material-symbols-outlined">account_circle</span> <strong>Name:</strong> {{ modalEntry.name }}</li>
            <li><span class="material-symbols-outlined">phone</span> <strong>Phone:</strong> 
              <span v-if="modalEntry.phone">&nbsp;
                <a :href="phoneLink(modalEntry.phone)">{{ formatPhoneNumber(modalEntry.phone) }}</a>
              </span>
              <span v-else>No Phone</span>
            </li>
            <li v-if="modalEntry.partySize"><span class="material-symbols-outlined">groups</span> <strong>Party size:</strong> {{ modalEntry.partySize }}</li>
          </ul>
        </div>
        <p>What would you like to do?</p>
        <p v-if="!smsConfigured || smsFailed" class="foot-note">
          If you would like to notify the customer by SMS, configure your Twilio account in
          <router-link to="/account/account#twilio" class="link-inline">Account Settings</router-link>.
        </p>
        <p v-if="!smsEnabled" class="foot-note">
          Sms has been disabled, click the send sms to simulate the notification being sent without actually sending a message. This is useful for testing or if you have SMS configured but it is currently failing.
        </p>
        <div v-if="smsCooldown > 0 || notifyCooldown > 0">
          <span class="cooldown-msg">
            You can send another message in {{ smsCooldown > 0 ? smsCooldown : notifyCooldown }} second{{ (smsCooldown > 0 ? smsCooldown : notifyCooldown) === 1 ? '' : 's' }}
          </span>
        </div>
        <div class="modal-actions">
          <div class="action-group" v-if="!smsConfigured || smsFailed">
            <button
              @click="sendNotification(modalEntryId!, 'notify', 'tableReady')"
              class="btn-notify"
              :disabled="notifyCooldown > 0 || smsCooldown > 0"
              type="button"
            >
              Send notification
            </button>
          </div>
          <div class="action-group" v-if="smsConfigured && !smsFailed">
            <button
              @click="sendNotification(modalEntryId!, 'sms', 'tableReady')"
              class="btn-notify"
              :disabled="notifyCooldown > 0 || smsCooldown > 0"
              type="button"
            >
              Send SMS
            </button>
          </div>
          <button @click="guestArrived(modalEntryId!)" class="btn-notify" type="button">Guest has arrived</button>
          <button @click="closeModal" class="btn-cancel" type="button">Cancel</button>
        </div>
      </div>
    </div>

    <!-- disable confirmation modal -->
    <div v-if="disableDialogVisible" class="modal-overlay">
      <div class="modal-content">
        <h3>Confirm Disable</h3>
        <p>Are you sure you want to disable the waitlist? This will clear the current entries.</p>
        <div class="modal-actions no-gap">
          <button class="btn-notify btn-danger" @click="performDisable">Yes, disable</button>
          <button class="btn-cancel" @click="disableDialogVisible = false">Cancel</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { useStore } from 'vuex'
import { error as notifyError } from '@/utils/notify'
import { api, throwIfNotOk, getApiUrl } from '@/utils/api'
import DataTable from '@/components/DataTable.vue'
import { formatTime } from '@/utils/dateFormatter'
import { formatPhoneNumber, phoneLink } from '@/utils/phoneFormatter'

const store = useStore()
const user = computed(() => store.getters.user)

const waitlistEnabled = ref(false)
const waitlistActive = computed(() => status.value?.enabled && status.value?.open)

// helper to know if SMS feature is editable
const smsConfigured = computed(() => !!user.value?.account?.twilioAccountSid)
const smsFailed = ref(false)
const smsEnabled = computed(() => !!user.value?.account?.smsEnabled && !smsFailed.value)

// helper: convert service-hours string to a value suitable for <input type="time"> (HH:mm)
function normalizeTimeForInput(val: string | undefined): string {
  if (!val) return ''
  // strip anything after minutes (seconds, fraction)
  return val.length > 5 ? val.slice(0, 5) : val
}

watch(waitlistEnabled, (val, oldVal) => {
  // push updates will keep us in sync; if we just disabled the waitlist after it was
  // enabled, clear local entries so the table disappears.
  if (oldVal && !val) {
    entries.value = []
  }
  if (val) {
    // compute a default schedule; prefer service hours for today if available
    const now = new Date()
    const today = now.toISOString().slice(0, 10)
    const timeStr = now.toTimeString().slice(0, 5)

    openDate.value = openDate.value || today
    closeDate.value = closeDate.value || today

    // attempt to fill from account service hours
    const svc = user.value?.account?.serviceHours || {}
    const days: Record<number, string> = {
      0: 'SUNDAY',
      1: 'MONDAY',
      2: 'TUESDAY',
      3: 'WEDNESDAY',
      4: 'THURSDAY',
      5: 'FRIDAY',
      6: 'SATURDAY'
    }
    const dayKey = days[now.getDay()]
    const todayHours = svc[dayKey]
    if (todayHours) {
      // always apply the service hours so they pre-fill the form
      openTime.value = normalizeTimeForInput(todayHours.openTime)
      closeTime.value = normalizeTimeForInput(todayHours.closeTime)
    }

    // fall back to current time for any missing fields
    if (!openTime.value) openTime.value = timeStr
    if (!closeTime.value) closeTime.value = timeStr
  }
})
const openDate = ref('')
const openTime = ref('')
const closeDate = ref('')
const closeTime = ref('')
const status = ref<any>(null)

const entries = ref<any[]>([])
const modalVisible = ref(false)
const modalEntryId = ref<string | null>(null)
const modalEntry = ref<any>(null)  // full entry data for modal display

// confirmation dialog for disabling waitlist
const disableDialogVisible = ref(false)

// cooldown state for notification buttons (seconds remaining)
const notifyCooldown = ref(0)
const smsCooldown = ref(0)
let notifyTimer: number | null = null
let smsTimer: number | null = null

// clear timer vars when their cooldown drops to zero
watch(notifyCooldown, (n) => {
  if (n <= 0 && notifyTimer) {
    clearInterval(notifyTimer)
    notifyTimer = null
  }
})
watch(smsCooldown, (n) => {
  if (n <= 0 && smsTimer) {
    clearInterval(smsTimer)
    smsTimer = null
  }
})

/**
 * start a 30‑second countdown; returns the interval id so caller can clear later.
 * @param cooldownRef reactive seconds counter
 */
function startCooldown(cooldownRef: typeof notifyCooldown): number {
  cooldownRef.value = 30
  const id = window.setInterval(() => {
    cooldownRef.value--
    if (cooldownRef.value <= 0) {
      clearInterval(id)
    }
  }, 1000)
  return id
}

const columns = [
  { key: 'actions', label: '' },
  { key: 'idx', label: '#' },
  { key: 'name', label: 'Name' },
  { key: 'phone', label: 'Phone' },
  { key: 'partySize', label: 'Party' },
  { key: 'timestamp', label: 'Time' }
]

async function loadEntries() {
  try {
    const resp = await api.get('/entries')
    if (resp.ok) {
      const data = await resp.json()
      entries.value = data.map((e: any, i: number) => ({
        ...e,
        idx: i + 1,
        timestamp: formatTime(e.timestamp)
      }))
    }
  } catch (err) {
    console.error('failed to load entries', err)
  }
}

// when user clicks notify button we display a modal allowing
// them to choose between sending the notification or indicating arrival.
function openNotifyModal(code: string) {
  modalEntryId.value = code
  modalEntry.value = entries.value.find(e => e.code === code) || null
  modalVisible.value = true
}

async function sendNotification(id: string, type: 'sms' | 'notify' = 'notify', message: string = 'tableReady') {
  try {
    const resp = await api.post(`/entries/${id}/notify`, { type, message })
    await throwIfNotOk(resp)
    import('@/utils/notify').then(({ success }) => success('Notification sent', { timeout: 2000 }))
    // start cooldown regardless of previous state
    switch (type) {
      case 'sms':
        if (smsTimer) {
          clearInterval(smsTimer)
        }
        smsTimer = startCooldown(smsCooldown)
        break
      case 'notify':
      default:
         if (notifyTimer) {
          clearInterval(notifyTimer)
        }
        notifyTimer = startCooldown(notifyCooldown)
        break
    }
  } catch (err) {
    import('@/utils/notify').then(({ error }) => error('Failed to send notification', { timeout: false }))
  }
}

async function guestArrived(id: string) {
  try {
    const d = await api.delete(`/entries/${id}`)
    await throwIfNotOk(d)
  } catch (e) {
    console.error('delete failed', e)
  }
  entries.value = entries.value.filter(e => e.code !== id)
  closeModal()
}

function closeModal() {
  modalVisible.value = false
  modalEntryId.value = null
  modalEntry.value = null
  // clear any in‑progress cooldowns so the next time the modal opens the buttons are enabled
  notifyCooldown.value = 0
  smsCooldown.value = 0
  if (notifyTimer) {
    clearInterval(notifyTimer)
    notifyTimer = null
  }
  if (smsTimer) {
    clearInterval(smsTimer)
    smsTimer = null
  }
}

function confirmDisableWaitlist() {
  disableDialogVisible.value = true
}

async function performDisable() {
  // this mirrors previous disableWaitlist logic with confirmation suppressed
  try {
    const response = await api.patch(`/accounts/${user.value.account.id}/waitlist-settings`, { waitlistEnabled: false })
    await throwIfNotOk(response)
    const updated: any = await response.json()
    if (store.getters.user) {
      store.commit('SET_USER', { ...store.getters.user, account: updated })
    }
    waitlistEnabled.value = false
    import('@/utils/notify').then(({ success }) => success('Waitlist disabled', { timeout: 2000 }))
    fetchStatus()
    entries.value = []
  } catch (err: any) {
    notifyError(err?.message || 'Failed to disable waitlist', { timeout: false })
  } finally {
    disableDialogVisible.value = false
  }
}

// we no longer poll for changes; the backend will push updates via SSE


function populate() {
  if (user.value && user.value.account) {
    waitlistEnabled.value = !!user.value.account.waitlistEnabled
    if (user.value.account.waitlistOpenTime) {
      const dt = user.value.account.waitlistOpenTime
      openDate.value = dt.slice(0, 10)
      openTime.value = dt.slice(11, 16)
    }
    if (user.value.account.waitlistCloseTime) {
      const dt = user.value.account.waitlistCloseTime
      closeDate.value = dt.slice(0, 10)
      closeTime.value = dt.slice(11, 16)
    }

    // if waitlist is enabled but no explicit times are stored, fall back to
    // today's service hours (mirrors watch logic for toggling)
    if (waitlistEnabled.value) {
      const now = new Date()
      const days: Record<number, string> = {
        0: 'SUNDAY',
        1: 'MONDAY',
        2: 'TUESDAY',
        3: 'WEDNESDAY',
        4: 'THURSDAY',
        5: 'FRIDAY',
        6: 'SATURDAY'
      }
      const dayKey = days[now.getDay()]
      const svc = user.value.account.serviceHours || {}
      const todayHours = svc[dayKey]
      if (todayHours) {
        openDate.value = openDate.value || now.toISOString().slice(0, 10)
        openTime.value = normalizeTimeForInput(todayHours.openTime)
        closeDate.value = closeDate.value || now.toISOString().slice(0, 10)
        closeTime.value = normalizeTimeForInput(todayHours.closeTime)
      }
    }
  }
}

async function fetchStatus() {
  try {
    const id = user.value?.account?.id || 1
    const resp = await api.get(`/accounts/${id}/waitlist-status`)
    if (resp.ok) status.value = await resp.json()
  } catch (err) {}
}

const saveWaitlistSettings = async () => {
  // client-side time range validation when enabling
  if (waitlistEnabled.value && openDate.value && openTime.value && closeDate.value && closeTime.value) {
    const openDT = new Date(`${openDate.value}T${openTime.value}:00`)
    const closeDT = new Date(`${closeDate.value}T${closeTime.value}:00`)
    const now = new Date()
    if (now < openDT || now > closeDT) {
      notifyError('Cannot enable waitlist: current time is outside the specified open/close window', { timeout: false })
      return
    }
  }
  try {
    const payload: any = { waitlistEnabled: waitlistEnabled.value }
    if (openDate.value && openTime.value) {
      payload.waitlistOpenTime = `${openDate.value}T${openTime.value}:00`
    }
    if (closeDate.value && closeTime.value) {
      payload.waitlistCloseTime = `${closeDate.value}T${closeTime.value}:00`
    }

    const response = await api.patch(`/accounts/${user.value.account.id}/waitlist-settings`, payload)
    await throwIfNotOk(response)
    const updated: any = await response.json()
    if (store.getters.user) {
      store.commit('SET_USER', { ...store.getters.user, account: updated })
    }
    import('@/utils/notify').then(({ success }) => success('Settings saved', { timeout: 2000 }))
    // refresh status to reflect any new open/closed state
    fetchStatus()
  } catch (err: any) {
    notifyError(err?.message || 'Failed to save waitlist settings', { timeout: false })
  }
}

let source: EventSource | null = null

onMounted(async () => {
  populate()
  fetchStatus()
  loadEntries()
  // establish server-sent event stream for live updates
  try {
    // build full URL via utility to keep base config logic
    const streamUrl = getApiUrl('/entries/stream')
    source = new EventSource(streamUrl)
    source.addEventListener('new-entry', (e: MessageEvent) => {
      const payload = JSON.parse(e.data)
      const entry = payload.entry || payload
      if (payload.estimatedWait !== undefined && status.value) {
        status.value.estimatedWait = payload.estimatedWait
      }
      // compute formatted fields identical to initial load
      const idx = entries.value.length + 1
      entries.value.push({ ...entry, idx, timestamp: formatTime(entry.timestamp) })
    })
    source.addEventListener('deleted-entry', (e: MessageEvent) => {
      const payload = JSON.parse(e.data)
      const code = payload.code || String(e.data)
      if (payload.estimatedWait !== undefined && status.value) {
        status.value.estimatedWait = payload.estimatedWait
      }
      entries.value = entries.value.filter(r => r.code !== code)
      // re-index rows
      entries.value = entries.value.map((r,i) => ({ ...r, idx: i+1 }))
    })
    source.addEventListener('updated-entry', (e: MessageEvent) => {
      const payload = JSON.parse(e.data)
      const updated = payload.entry || payload
      if (payload.estimatedWait !== undefined && status.value) {
        status.value.estimatedWait = payload.estimatedWait
      }
      entries.value = entries.value.map(r => r.code === updated.code ? { ...r, ...updated, timestamp: formatTime(updated.timestamp) } : r)
    })
    source.addEventListener('notified-entry', (e: MessageEvent) => {
      const code = String(e.data)
      // optionally flag entry locally so UI can show it has been notified
      entries.value = entries.value.map(r => r.code === code ? { ...r, notified: true } : r)
    })
  } catch (ex) {
    console.warn('could not open event stream', ex)
  }
})

onUnmounted(() => {
  if (source) source.close()
  if (notifyTimer) clearInterval(notifyTimer)
  if (smsTimer) clearInterval(smsTimer)
})
</script>

<style scoped>
.schedule-fields {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  margin-top: 0.5rem;
}

.schedule-fields .field {
  display: flex;
  flex-direction: column;
  margin-right: 1rem;
}

.btn-save {
  padding: 0.5rem 1rem;
  background-color: var(--theme-color, #42b983);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

/* align form fields with application-wide styles */
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

.btn-notify {
  /* make button about 25% larger: bump padding & font-size */
  padding: 0.3125rem 0.625rem; /* 0.25 *1.25, 0.5*1.25 */
  font-size: 1.0625rem; /* 0.85 * 1.25 */
  background-color: var(--theme-color, #42b983);
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.btn-notify:hover {
  opacity: 0.9;
}
.btn-notify:disabled {
  background-color: #aaa;
  cursor: not-allowed;
}
.btn-save:hover {
  opacity: 0.9;
}

/* modal overlay */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal-content {
  background: white;
  padding: 2rem;
  border-radius: 6px;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 2px 10px rgba(0,0,0,0.2);
  text-align: center;
}
.modal-actions {
  margin-top: 1rem;
  display: flex;
  gap: 0.5rem;
  justify-content: center;
}
.btn-cancel {
  /* use a lighter accent so it doesn't resemble a disabled button */
  background: #e0e0e0;
  color: #333;
  border: 1px solid #ccc;
}
.btn-cancel:hover {
  background: #d5d5d5;
  opacity: 1;
}

/* cooldown styling */
.modal-actions .action-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 0.5rem;
}
.cooldown-msg {
  font-size: 0.85rem;
  color: #888;
  margin-top: 0.25rem;
}
.entry-info {
  text-align: left;
  padding-left: 1rem;
  font-size: 0.95rem;
  list-style-type: none;
  margin: 0;
  padding: 0;
}
.entry-info .material-symbols-outlined {
  vertical-align: middle;
  margin-right: 0.25rem;
  font-size: 1.5rem;
}
.foot-note {
  font-size: 0.85rem;
  color: #888;
  margin-top: 0.5rem;
}

input[type="checkbox"] {
  width: auto;
  margin-right: 8px;
  accent-color: var(--theme-color, #4CAF50);
  transform: scale(1.5);
}

</style>
