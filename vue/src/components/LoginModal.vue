<template>
  <div v-if="isVisible" class="modal-overlay" @click="close">
    <div class="modal-content login-card" @click.stop>
      <div class="modal-header">
        <h3>Session Expired</h3>
        <button @click="close" class="btn-close">&times;</button>
      </div>
      <div class="modal-body">
        <p>{{ message }}</p>
        <form @submit.prevent="submitLogin">
          <div class="form-group">
            <label for="modal-username">Username</label>
            <input
              id="modal-username"
              v-model="formData.username"
              type="text"
              required
              placeholder="Enter your username"
            />
          </div>
          <div class="form-group">
            <label for="modal-password">Password</label>
            <input
              id="modal-password"
              v-model="formData.password"
              type="password"
              required
              placeholder="Enter your password"
            />
          </div>
          <div class="error-message" v-if="error">
            {{ error }}
          </div>
          <button type="submit" :disabled="isSubmitting">
            {{ isSubmitting ? 'Logging in...' : 'Log In' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useStore } from 'vuex'
import { api, throwIfNotOk } from '@/utils/api'
import { success as notifySuccess } from '@/utils/notify'
import { useRouter } from 'vue-router'

const store = useStore()
const router = useRouter()

const isVisible = computed(() => store.getters.loginModalVisible)
const message = computed(() => store.getters.loginModalMessage)

const formData = ref({ username: '', password: '' })
const isSubmitting = ref(false)
const error = ref('')

function close() {
  store.dispatch('hideLoginModal')
  error.value = ''
}

async function submitLogin() {
  error.value = ''
  isSubmitting.value = true

  try {
    const response = await api.post('/auth/login', {
      username: formData.value.username,
      password: formData.value.password
    })
    await throwIfNotOk(response)

    const data = await response.json()
    // store token + user
    store.dispatch('login', { token: data.token, user: data.user })

    notifySuccess(`Welcome back, ${data.user.fullName}!`)

    // redirect if there was somewhere we wanted to go after login
    const redirectPath = sessionStorage.getItem('redirectAfterLogin')
    if (redirectPath) {
      sessionStorage.removeItem('redirectAfterLogin')
      router.push(redirectPath)
    }

    close()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'An error occurred during login'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  /* match login-card spacing */
  padding: 40px;
  border-radius: 8px;
  width: 90%;
  max-width: 400px;
  /* softer shadow like page card */
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.modal-body {
  padding: 20px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e6e6e6;
}

.modal-header h3 {
  margin: 0;
  color: #333;
}

.btn-close {
  background: none;
  border: none;
  font-size: 28px;
  color: #999;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  line-height: 1;
}

.btn-close:hover {
  color: #333;
}

.btn-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  line-height: 1;
  cursor: pointer;
}

.form-group {
  margin-bottom: 20px;
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

.error-message {
  padding: 12px;
  background-color: #ffebee;
  color: #c62828;
  border-radius: 4px;
  margin-bottom: 20px;
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
</style>