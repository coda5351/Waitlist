<template>
  <div class="home">
    <h2>Welcome</h2>
    <p v-if="message">{{ message }}</p>
    <p v-else>Loading...</p>
    <p v-if="waitlistInfo">
      <strong>Waitlist status:</strong> {{ waitlistInfo.enabled ? (waitlistInfo.open ? 'Open' : 'Closed') : 'Closed' }}
      <span v-if="waitlistInfo.enabled && waitlistInfo.openTime">
        ({{ formatHour(waitlistInfo.openTime) }} - {{ formatHour(waitlistInfo.closeTime) }})
      </span>
    </p>
    <p v-if="waitlistInfo && waitlistInfo.enabled && waitlistInfo.open && waitlistInfo.estimatedWait !== undefined">
      Estimated wait: {{ waitlistInfo.estimatedWait }} minutes
    </p>
    <router-link v-if="waitlistInfo && waitlistInfo.enabled" class="cta-button" to="/waitlist">Join Waitlist</router-link>
    <p v-if="statusError" class="error">Unable to load waitlist status: {{ statusError }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '@/utils/api'
import store from '@/store'
import '@/assets/themes.css'

const message = ref<string>('')
const waitlistInfo = ref<any | null>(null)
const statusError = ref('')

// helper to turn ISO datetime or hh:mm strings into hh:mm am/pm
function formatHour(val: string | null | undefined) {
  if (!val) return ''
  // if contains T assume full datetime
  let timeStr = val
  if (val.includes('T')) {
    const parts = val.split('T')[1]
    timeStr = parts.slice(0,5)
  }
  const [h, m] = timeStr.split(':').map(Number)
  if (isNaN(h) || isNaN(m)) return val
  let hour = h % 12 || 12
  const ampm = h >= 12 ? 'pm' : 'am'
  return `${hour}:${m.toString().padStart(2,'0')} ${ampm}`
}

// perform the fetches after the component has mounted so we don't block
onMounted(async () => {
  try {
    const response = await api.get('/hello')
    if (response.ok) {
      const data = await response.json()
      message.value = data.message || JSON.stringify(data)
    } else {
      message.value = `Error: ${response.status} ${response.statusText}`
    }
  } catch (error) {
    message.value = `Failed to fetch: ${error instanceof Error ? error.message : 'Unknown error'}`
  }

  // always try account 1 if none is signed in
  const acctId = store.getters.user?.account?.id || 1
  try {
    const resp = await api.get(`/accounts/${acctId}/waitlist-status`)
    if (resp.ok) {
      waitlistInfo.value = await resp.json()
    } else {
      statusError.value = `HTTP ${resp.status}`
    }
  } catch (err: any) {
    statusError.value = err.message || String(err)
  }
})
</script>

<style scoped>
.home {
  padding: 1rem;
}

h2 {
  color: var(--theme-color, #42b983);
  margin-bottom: 1rem;
}

p {
  color: #666;
}
</style>
