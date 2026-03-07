<template>
  <div id="app" class="app">
    <nav class="navbar">
      <div class="nav-brand">Waitlist</div>
      <!-- hamburger toggle for small screens -->
      <button class="nav-toggle" @click="toggleMenu" aria-label="Toggle menu">
        <span class="bar"></span>
        <span class="bar"></span>
        <span class="bar"></span>
      </button>
      <div :class="['nav-links', { open: mobileMenuOpen }]">
        <router-link to="/" class="nav-link" @click="mobileMenuOpen = false">Home</router-link>
        <router-link v-if="isAuthenticated" to="/account" class="nav-link" @click="mobileMenuOpen = false">Account</router-link>
        <router-link v-if="!isAuthenticated" to="/login" class="nav-link" @click="mobileMenuOpen = false">Login</router-link>
      </div>
    </nav>
    <main class="container">
      <router-view />
    </main>

    <!-- inline login modal shown when token expires -->
    <LoginModal />
  </div>
</template>

<script setup lang="ts">
import { useTheme } from '@/composables/useTheme'
import { onMounted, computed, ref } from 'vue'
import { useStore } from 'vuex'
import LoginModal from '@/components/LoginModal.vue'

const { initTheme } = useTheme()
const store = useStore()

const isAuthenticated = computed(() => store.getters.isAuthenticated)

// state for mobile menu
const mobileMenuOpen = ref(false)
function toggleMenu() {
  mobileMenuOpen.value = !mobileMenuOpen.value
}

onMounted(() => {
  initTheme()
})
</script>

<style scoped>
:global([data-theme="green"]) {
  --theme-color: #42b983;
}

:global([data-theme="burnt-orange"]) {
  --theme-color: #cc5500;
}

:global([data-theme="sky-blue"]) {
  --theme-color: #0ea5e9;
}

.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 2rem;
  background-color: #f5f5f5;
  border-bottom: 2px solid var(--theme-color);
  gap: 2rem;
}

.nav-brand {
  font-size: 1.5rem;
  font-weight: bold;
  color: var(--theme-color);
}

.nav-links {
  display: flex;
  gap: 1rem;
  flex: 1;
}

/* hamburger button hidden by default */
.nav-toggle {
  display: none;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.5rem;
}
.nav-toggle .bar {
  display: block;
  width: 25px;
  height: 3px;
  margin: 4px 0;
  background-color: #333;
  transition: all 0.3s;
}

/* responsive adjustments */
@media (max-width: 600px) {
  .nav-links {
    display: none;
    position: absolute;
    top: 60px;
    left: 0;
    right: 0;
    background-color: #f5f5f5;
    flex-direction: column;
    align-items: center;
    gap: 0;
    padding: 1rem 0;
  }
  .nav-links.open {
    display: flex;
  }
  .nav-toggle {
    display: block;
  }
}

.nav-link {
  padding: 0.5rem 1rem;
  color: #333;
  text-decoration: none;
  border-radius: 4px;
  transition: all 0.3s;
}

.nav-link:hover,
.nav-link.router-link-active {
  background-color: var(--theme-color);
  color: white;
}

.container {
  flex: 1;
  padding: 2rem;
  text-align: center;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen',
    'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue',
    sans-serif;
}
</style>
