<template>
  <div class="login-container">
    <div class="login-wrapper">
      <div class="login-card">
        <h1>Reset Password</h1>
        <p v-if="email">for <strong>{{ email }}</strong></p>

        <div v-if="successMessage" class="success-message">
          {{ successMessage }}
        </div>

        <div v-else>
          <form @submit.prevent="handleSubmit" class="reset-form">
            <div class="form-group">
              <label for="newPassword">New Password</label>
              <input
                id="newPassword"
                type="password"
                v-model="form.newPassword"
                required
                placeholder="Enter new password"
              />
            </div>

            <div class="form-group">
              <label for="confirmPassword">Confirm Password</label>
              <input
                id="confirmPassword"
                type="password"
                v-model="form.confirmPassword"
                required
                placeholder="Re-enter password"
              />
            </div>

            <div v-if="error" class="error-message">
              {{ error }}
            </div>

            <button type="submit" :disabled="isSubmitting">
              {{ isSubmitting ? 'Submitting...' : 'Reset Password' }}
            </button>
          </form>
        </div>
      </div>

      <!-- right-hand column with quick links -->
      <div class="register-section">
        <div class="register-content">
          <h2>Need help?</h2>
          <p class="button-label">Already have an account?</p>
          <p>
            <router-link to="/login" class="register-button">Go To Login</router-link>
          </p>
          <hr class="divider" />
          <p class="button-label">Need to create one?</p>
          <p>
            <router-link to="/register" class="register-button">Register Here</router-link>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { api, throwIfNotOk } from '@/utils/api'

const route = useRoute()
const token = ref<string | null>(null)
const email = ref<string | null>(null)

onMounted(() => {
  token.value = (route.query.token as string) || null
  email.value = (route.query.email as string) || null
})

const form = ref({ newPassword: '', confirmPassword: '' })
const error = ref('')
const successMessage = ref('')
const isSubmitting = ref(false)

const handleSubmit = async () => {
  error.value = ''
  if (form.value.newPassword !== form.value.confirmPassword) {
    error.value = 'Passwords do not match'
    return
  }

  if (!token.value || !email.value) {
    error.value = 'Missing reset token or email'
    return
  }

  isSubmitting.value = true
  try {
    const response = await api.post('/auth/reset-password', {
      email: email.value,
      token: token.value,
      newPassword: form.value.newPassword,
    })
    await throwIfNotOk(response)
    const data = await response.json()
    successMessage.value = data.message || 'Your password has been reset.'
  } catch (err) {
    error.value = err instanceof Error ? err.message : String(err)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
/* reuse same layout classes as login page */
.login-container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: 100vh;
  padding: 40px 20px 20px;
}

.login-wrapper {
  display: flex;
  gap: 30px;
  max-width: 900px;
  width: 100%;
}

.login-card {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  flex: 1;
  max-width: 400px;
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

.success-message {
  padding: 12px;
  background-color: #e8f5e9;
  color: #2e7d32;
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

@media (max-width: 768px) {
  .login-wrapper {
    flex-direction: column;
  }
}

/* right-hand panel for login/register links */
.register-section {
  background: #f8f9fa;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 300px;
}

.register-content {
  text-align: center;
}

.register-content h2 {
  margin: 0 0 10px;
  color: #333;
  font-size: 1.5rem;
}

.register-content p {
  margin: 0 0 25px;
  color: #666;
  font-size: 1rem;
}

.register-button {
  display: block;
  width: 240px;              /* fixed size
                               keeps buttons identical and centered */
  padding: 12px 30px;
  background-color: #2196F3;
  color: white;
  text-decoration: none;
  border-radius: 4px;
  font-weight: 500;
  transition: background-color 0.3s;
  margin: 5px auto;          /* horizontally centre within container */
}

.register-button:hover {
  background-color: #1976D2;
}

/* helper text and divider before buttons */
.button-label {
  margin: 0 0 5px;
  color: #555;
  font-size: 0.9rem;
}

.divider {
  border: none;
  border-top: 1px solid #ddd;
  margin: 15px 0;
}

/* header uses theme color so it can adapt when themes change */
h1 {
  margin: 0 0 30px;
  text-align: center;
  color: var(--theme-color, #333);
}
</style>
