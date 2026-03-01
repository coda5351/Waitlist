<template>
  <div>
    <div class="info-section">
      <h3>Data Management</h3>
      <p>Export, import, and manage your data.</p>
    </div>

    <div class="info-section">
      <h3>Export User Data</h3>
      <p>Download all your personal data including profile information, activity history, and preferences.</p>
      <button @click="exportUserData" class="btn-export">
        <span class="material-symbols-outlined">download</span>
        <span>Export My Data</span>
      </button>
    </div>

    <div v-if="user?.role === 'ADMIN'" class="info-section">
      <h3>Export Account Data</h3>
      <p>Download all account data including users, settings, and configurations.</p>
      <button @click="exportAccountData" class="btn-export">
        <span class="material-symbols-outlined">download</span>
        <span>Export Account Data</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import '@/assets/themes.css'
import { useStore } from 'vuex'
import { computed } from 'vue'
import { error as notifyError } from '@/utils/notify'
import { api } from '@/utils/api'

const store = useStore()
const user = computed(() => store.getters.user)

const exportUserData = async () => {
  try {
    const response = await api.get('/users/export/user')
    
    if (response.ok) {
      const blob = await response.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `user-data-${new Date().toISOString().split('T')[0]}.json`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } else {
      notifyError('Failed to export user data. Please try again.', { timeout: false })
    }
  } catch (error) {
    notifyError('An error occurred while exporting user data.', { timeout: false })
    console.error('Error exporting user data:', error)
  }
}

const exportAccountData = async () => {
  try {
    const response = await api.get('/users/export/account')
    
    if (response.ok) {
      const blob = await response.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `account-data-${new Date().toISOString().split('T')[0]}.json`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } else {
      notifyError('Failed to export account data. Please try again.', { timeout: false })
    }
  } catch (error) {
    notifyError('An error occurred while exporting account data.', { timeout: false })
    console.error('Error exporting account data:', error)
  }
}
</script>

<style scoped>
.btn-export {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  background-color: var(--theme-color, #42b983);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  transition: opacity 0.3s;
  margin-top: 0.75rem;
}

.btn-export:hover {
  opacity: 0.8;
}

.btn-export .material-symbols-outlined {
  font-size: 20px;
}
</style>

