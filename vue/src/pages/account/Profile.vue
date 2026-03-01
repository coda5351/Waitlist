<template>
  <div class="profile-sections">
    <div class="info-section">
      <h3>Profile Information</h3>
      <p><strong>Username:</strong> {{ user?.username || 'N/A' }}</p>
      <p><strong>Email:</strong> {{ user?.email || 'N/A' }}</p>
      <p><strong>Member since:</strong> {{ formattedCreatedAt }}</p>
      <p><strong>Account Status:</strong> Active</p>
    </div>
  </div>
  <div class="info-section actions">
    <button class="btn-logout" @click="handleLogout">Logout</button>
  </div>
</template>

<script setup lang="ts">
import '@/assets/themes.css'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { computed } from 'vue'
import { info as notifyInfo } from '@/utils/notify'
import { formatDate } from '@/utils/dateFormatter'

const store = useStore()
const router = useRouter()

const user = computed(() => store.getters.user)

const formattedCreatedAt = computed(() => {
  if (!user.value?.createdAt) return 'N/A'
  return formatDate(user.value.createdAt)
})

const handleLogout = () => {
  store.dispatch('logout')
  notifyInfo('You are now logged out')
  router.push('/login')
}
</script>

<style scoped>
.profile-sections {
  display: flex;
  gap: 1.5rem;
  margin-bottom: 1.25rem;
}

.profile-sections .info-section {
  flex: 1;
}

@media (max-width: 768px) {
  .profile-sections {
    flex-direction: column;
  }
}

.translation-note {
  margin-top: 1rem;
  font-size: 0.85rem;
  color: #666;
}

.support-link {
  color: var(--theme-color, #42b983);
  text-decoration: none;
  font-weight: 500;
}

.support-link:hover {
  text-decoration: underline;
}
</style>
