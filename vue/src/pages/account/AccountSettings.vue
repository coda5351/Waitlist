<template>
  <div>
    <div class="info-section">
      <h3>Account Settings</h3>
      <p>Manage your account preferences and security settings.</p>
    </div>

    <div class="info-section">
      <h3>Theme Accent Color</h3>
      <p>Choose your preferred accent color for the application.</p>
      <div class="color-options">
        <button
          :class="{ 'color-option': true, active: themeColor === 'green' }"
          @click="setThemeColor('green')"
        >
          <span class="color-swatch" style="background-color: #42b983;"></span>
          Green
        </button>
        <button
          :class="{ 'color-option': true, active: themeColor === 'burnt-orange' }"
          @click="setThemeColor('burnt-orange')"
        >
          <span class="color-swatch" style="background-color: #cc5500;"></span>
          Burnt Orange
        </button>
        <button
          :class="{ 'color-option': true, active: themeColor === 'sky-blue' }"
          @click="setThemeColor('sky-blue')"
        >
          <span class="color-swatch" style="background-color: #0ea5e9;"></span>
          Sky Blue
        </button>
      </div>
    </div>

    <!-- Twilio credentials -->
    <div id="twilio-config" class="info-section">
      <h3>Twilio Configuration</h3>
      <p>Specify the account SID and auth token used for sending SMS. These values are stored securely and are not retrievable once saved.</p>
      <div class="field">
        <label for="twilio-sid">Account SID</label>
        <input id="twilio-sid" type="text" v-model="twilioSid" />
      </div>
      <div class="field">
        <label for="twilio-token">Auth Token</label>
        <input id="twilio-token" v-model="twilioToken" />
      </div>
      <div class="actions">
        <button type="button" class="btn-save" @click.prevent.stop="saveTwilioSettings">Save Twilio Settings</button>
      </div>
    </div>

    <!-- SMS enable toggle separate form -->
    <div class="info-section">
      <h3>SMS Delivery</h3>
      <div class="field">
        <label for="sms-enabled">
          <input id="sms-enabled" type="checkbox" v-model="smsEnabled" />
          Enable SMS delivery
        </label>
        <p class="foot-note">Toggle off to prevent messages from being sent (they'll still be logged).</p>
      </div>
      <div class="actions">
        <button type="button" class="btn-save" @click.prevent.stop="saveSmsToggle">Save SMS Setting</button>
      </div>
    </div>

  </div>

  <!-- service hours section -->
  <div class="info-section">
    <h3>Service Hours</h3>
    <p>Defaults used when no explicit schedule; only relevant if waitlist feature is enabled.</p>
    <div class="schedule-fields service-hours" v-if="daysOfWeek.length">
      <div v-for="day in daysOfWeek" :key="day" class="field">
        <label>{{ day }}:</label>
        <input type="time" v-model="serviceHours[day].openTime" />
        <input type="time" v-model="serviceHours[day].closeTime" />
      </div>
    </div>
    <div class="actions">
      <button type="button" class="btn-save" @click.prevent.stop="saveServiceHours">Save service hours</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, computed, ref, watch } from 'vue'
import { useStore } from 'vuex'
import { useRoute } from 'vue-router'
import { error as notifyError } from '@/utils/notify'
import { useTheme, type ThemeColor } from '@/composables/useTheme'
import { api, throwIfNotOk, getApiUrl, parseErrorResponse } from '@/utils/api'
import '@/assets/themes.css'

const store = useStore()
const route = useRoute()
const user = computed(() => store.getters.user)

// twilio credentials state
const twilioSid = ref('')
const twilioToken = ref('')
const smsEnabled = ref(true)

// service hours logic moved here
const daysOfWeek = [
  'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'
]
const serviceHours = ref<Record<string,{openTime:string;closeTime:string}>>({})
// pre-populate keys so bindings never access undefined
daysOfWeek.forEach(d => {
  serviceHours.value[d] = { openTime: '', closeTime: '' }
})

function populateServiceHours() {
  // if we were navigated here with a hash, trigger highlight
  if (window.location.hash === '#twilio') {
    flashTwilioSection()
  }
}

// trigger whenever route.hash changes
watch(() => route.hash, (h) => {
  if (h === '#twilio') {
    flashTwilioSection()
  }
})

function flashTwilioSection() {
  const el = document.getElementById('twilio-config')
  if (!el) return
  el.classList.add('flash-highlight')
  // remove class after animation ends to allow retrigger
  el.addEventListener('animationend', () => {
    el.classList.remove('flash-highlight')
  }, { once: true })
}

function loadAccountSettings() {
  if (user.value && user.value.account) {
    // grab twilio info if available; backend returns placeholder if token is stored
    twilioSid.value = user.value.account.twilioAccountSid || ''
    twilioToken.value = user.value.account.twilioAuthToken || ''
    smsEnabled.value = user.value.account.smsEnabled !== false // default true
    if (user.value.account.serviceHours) {
      // copy existing and then ensure any missing day is present
      serviceHours.value = { ...user.value.account.serviceHours }
      daysOfWeek.forEach(d => {
        if (!serviceHours.value[d]) {
          serviceHours.value[d] = { openTime: '', closeTime: '' }
        }
      })
    } else {
      // already initialized above, but ensure values reset
      daysOfWeek.forEach(d => {
        serviceHours.value[d] = { openTime: '', closeTime: '' }
      })
    }
  }
}

const saveServiceHours = async (e?: Event) => {
  if (e) {
    e.preventDefault()
    e.stopPropagation()
  }

  // build payload as before
  const bhPayload: any = {}
  Object.entries(serviceHours.value).forEach(([day, hrs]) => {
    if (hrs.openTime || hrs.closeTime) {
      bhPayload[day] = {}
      if (hrs.openTime) bhPayload[day].openTime = hrs.openTime
      if (hrs.closeTime) bhPayload[day].closeTime = hrs.closeTime
    }
  })

  try {
    // Use a raw fetch here to avoid the global 403->login redirect in apiFetch
    const url = getApiUrl(`/accounts/${user.value.account.id}/waitlist-settings`)
    const token = store.getters.token
    const resp = await fetch(url, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify({ serviceHours: bhPayload })
    })

    // if backend refuses, show an error but don't navigate
    if (resp.status === 403) {
      const msg = await parseErrorResponse(resp)
      notifyError(msg || 'Not authorized to update service hours', { timeout: false })
      return
    }

    await throwIfNotOk(resp)
    const updated: any = await resp.json()
    if (store.getters.user) {
      store.commit('SET_USER', { ...store.getters.user, account: updated })
    }
    import('@/utils/notify').then(({ success }) => success('Service hours saved', { timeout: 2000 }))
  } catch (err: any) {
    notifyError(err?.message || 'Failed to save service hours', { timeout: false })
  }

  // ensure we remain on account tab regardless of side effects
  if (window.location.pathname.endsWith('/account') ||
      window.location.pathname.endsWith('/account/account')) {
    // rewrite to explicit account/account to keep settings active
    history.replaceState(null, '', '/account/account')
  }
}

const saveTwilioSettings = async (e?: Event) => {
  if (e) {
    e.preventDefault()
    e.stopPropagation()
  }

  try {
    const payload: any = {}
    if (twilioSid.value) payload.accountSid = twilioSid.value
    // only send auth token if it's been changed to something other than placeholder
    if (twilioToken.value && twilioToken.value !== '*************') payload.authToken = twilioToken.value

    const response = await api.patch(`/accounts/${user.value.account.id}/twilio-settings`, payload)
    await throwIfNotOk(response)
    const updated: any = await response.json()
    if (store.getters.user) {
      store.commit('SET_USER', { ...store.getters.user, account: updated })
    }
    import('@/utils/notify').then(({ success }) => success('Twilio settings saved', { timeout: 2000 }))
    // clear token field since backend won't return it
    twilioToken.value = ''
  } catch (err: any) {
    notifyError(err?.message || 'Failed to save Twilio settings', { timeout: false })
  }
}

const saveSmsToggle = async (e?: Event) => {
  if (e) {
    e.preventDefault()
    e.stopPropagation()
  }

  try {
    const response = await api.patch(`/accounts/${user.value.account.id}/twilio-settings`, { smsEnabled: smsEnabled.value })
    await throwIfNotOk(response)
    const updated: any = await response.json()
    if (store.getters.user) {
      store.commit('SET_USER', { ...store.getters.user, account: updated })
    }
    import('@/utils/notify').then(({ success }) => success('SMS setting saved', { timeout: 2000 }))
  } catch (err: any) {
    notifyError(err?.message || 'Failed to save SMS setting', { timeout: false })
  }
}

const { themeColor, setThemeColor: setLocalThemeColor, initTheme } = useTheme()


const setThemeColor = async (color: ThemeColor) => {
  // Update local theme first
  setLocalThemeColor(color)
  
  // Send to backend
  try {
    const response = await api.patch(`/accounts/${user.value.account.id}/branding-color`, { brandingColorCode: color })
    
    try {
      await throwIfNotOk(response)
    } catch (err: any) {
      notifyError(err?.message || 'Failed to update theme color', { timeout: false })
    }
  } catch (error) {
    notifyError('Error updating theme color', { timeout: false })
  }
}


onMounted(() => {
  initTheme()
  populateServiceHours()
  loadAccountSettings()
})
</script>

<style scoped>
.service-hours, .schedule-fields {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  margin-top: 0.5rem;
}

.service-hours .field {
  /* place label and inputs on same row for more compact service-hours layout */
  display: flex;
  align-items: center;
  margin-right: 1rem;
}

.service-hours .field label {
  min-width: 80px;
  margin-right: 0.5rem;
}

.service-hours .field input {
  /* allow time inputs to size themselves rather than full-width */
  width: auto;
  min-width: 100px;
  margin-right: 0.5rem;
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

.btn-save:hover {
  opacity: 0.9;
}

/* form input styling consistent with other pages */
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

input[type="checkbox"] {
  width: auto;
  margin-right: 8px;
  accent-color: var(--theme-color, #4CAF50);
  transform: scale(1.5);
}

@keyframes flash {
  0% { background-color: var(--theme-color, #42b983); }
  100% { background-color: transparent; }
}
.flash-highlight {
  animation: flash 2s ease;
}
</style>

