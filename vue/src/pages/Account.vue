<template>
  <div class="account">
    <div class="account-layout">
      <aside class="sidebar">
        <nav>
          <router-link 
            v-if="user?.role === 'ADMIN'"
            to="/account/account"
            :class="{ 'nav-button': true, active: selectedTab === 'account' }"
          >
            <span class="material-symbols-outlined">manage_accounts</span>
            <span>Account</span>
          </router-link>
          <router-link
            v-if="user?.role === 'ADMIN'"
            to="/account/waitlist"
            :class="{ 'nav-button': true, active: selectedTab === 'waitlist' }"
          >
            <span class="material-symbols-outlined">schedule</span>
            <span>Waitlist</span>
          </router-link>
          <router-link
            v-if="user?.role === 'ADMIN'"
            to="/account/messages"
            :class="{ 'nav-button': true, active: selectedTab === 'messages' }"
          >
            <span class="material-symbols-outlined">message</span>
            <span>Messages</span>
          </router-link>
          <router-link 
            to="/account/profile"
            :class="{ 'nav-button': true, active: selectedTab === 'profile' }"
          >
            <span class="material-symbols-outlined">person</span>
            <span>Profile</span>
          </router-link>
          <router-link 
            v-if="user?.role === 'ADMIN' || user?.role === 'Manager'"
            to="/account/users"
            :class="{ 'nav-button': true, active: selectedTab === 'users' }"
          >
            <span class="material-symbols-outlined">group</span>
            <span>Users</span>
          </router-link>
          <router-link 
            to="/account/data"
            :class="{ 'nav-button': true, active: selectedTab === 'data' }"
          >
            <span class="material-symbols-outlined">database</span>
            <span>Data</span>
          </router-link>
        </nav>
      </aside>

      <main class="account-main">
        <section class="account-info">
          <ProfileTab v-if="selectedTab === 'profile'" />
          <UsersTab v-else-if="selectedTab === 'users'" />
          <DataTab v-else-if="selectedTab === 'data'" />
          <AccountSettingsTab v-else-if="selectedTab === 'account'" />
          <WaitlistTab v-else-if="selectedTab === 'waitlist'" />
          <MessagesTab v-else-if="selectedTab === 'messages'" />
          <AccountSettingsTab v-else />
        </section>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import ProfileTab from './account/Profile.vue'
import UsersTab from './account/Users.vue'
import DataTab from './account/Data.vue'
import AccountSettingsTab from './account/AccountSettings.vue'
import WaitlistTab from './account/Waitlist.vue'
import MessagesTab from './account/Messages.vue'

const router = useRouter()
const route = useRoute()
const store = useStore()

// redirect unauthenticated visitors to login
onMounted(() => {
  if (!store.getters.token) {
    router.replace('/login')
  }
})

const user = computed(() => store.getters.user)

const selectedTab = computed(() => {
  const tab = route.path.split('/').pop()
  if (!tab || tab === 'account') {
    // Default to 'profile' for USER role, 'account' for others
    return user.value?.role === 'USER' ? 'profile' : 'account'
  }
  return tab as 'account' | 'profile' | 'users' | 'data' | 'waitlist' | 'messages'
})
</script>

<style scoped>
.account {
  padding: 1rem;
}

.account-layout {
  display: flex;
  gap: 1.5rem;
  max-width: 1000px;
  margin: 0 auto;
}

.sidebar {
  width: 220px;
}

.sidebar nav {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.sidebar button {
  text-align: left;
  padding: 0.75rem 1rem;
  background: #fff;
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
  transition: background 0.15s, border-color 0.15s;
  color: var(--theme-color, #42b983);
}

.sidebar button.active {
  background: var(--theme-color, #42b983);
  color: #fff;
  border-color: var(--theme-color, #42b983);
}

.sidebar a.nav-button {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-align: left;
  padding: 0.75rem 1rem;
  background: #fff;
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
  transition: background 0.15s, border-color 0.15s;
  color: var(--theme-color, #42b983);
  text-decoration: none;
}

.sidebar a.nav-button .material-symbols-outlined {
  font-size: 20px;
}

.sidebar a.nav-button.active {
  background: var(--theme-color, #42b983);
  color: #fff;
  border-color: var(--theme-color, #42b983);
}

.account-main {
  flex: 1;
  min-width: 0;
}

h2 {
  color: var(--theme-color, #42b983);
  margin-bottom: 1rem;
}

.account-info {
  background-color: #f9f9f9;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 1.5rem;
}

.info-section {
  margin-bottom: 1.25rem;
}

.info-section h3 {
  color: #333;
  margin-bottom: 0.75rem;
  font-size: 1.05rem;
}

.info-section p {
  color: #666;
  margin: 0.35rem 0;
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.btn-logout {
  padding: 0.6rem 1.1rem;
  background-color: #ff6b6b;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.95rem;
}

@media (max-width: 700px) {
  .account-layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }

  .actions {
    justify-content: center;
  }
}
</style>
