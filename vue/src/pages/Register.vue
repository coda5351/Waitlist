<template>
  <div class="register-container">
    <div class="register-wrapper">
      <div class="register-card">
        <h1>Register</h1>
        <div v-if="registrationSuccess" class="success-container">
          <div class="success-icon">✓</div>
          <h2>Thank you for registering.</h2>
          <p>Go to the log in page to continue.</p>
          <router-link to="/login" class="login-button">Go to Login</router-link>
        </div>
        <form v-else @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="username">Username</label>
          <input
            id="username"
            v-model="formData.username"
            type="text"
            required
            placeholder="Enter username"
          />
        </div>

        <div class="form-group">
          <label for="email">Email</label>
          <input
            id="email"
            v-model="formData.email"
            type="email"
            required
            placeholder="Enter email"
          />
        </div>

        <div class="form-group">
          <label for="fullName">Full Name</label>
          <input
            id="fullName"
            v-model="formData.fullName"
            type="text"
            required
            placeholder="Enter full name"
          />
        </div>

        <div class="form-group">
          <label for="password">Password</label>
          <div class="password-input-wrapper">
            <input
              id="password"
              v-model="formData.password"
              :type="showPassword ? 'text' : 'password'"
              required
              placeholder="Enter password"
            />
            <button 
              type="button" 
              @click="showPassword = !showPassword" 
              class="password-toggle"
              :title="showPassword ? 'Hide password' : 'Show password'"
            >
              <span class="material-symbols-outlined">
                {{ showPassword ? 'visibility_off' : 'visibility' }}
              </span>
            </button>
          </div>
        </div>

        <div class="form-group">
          <label for="confirmPassword">Confirm Password</label>
          <div class="password-input-wrapper">
            <input
              id="confirmPassword"
              v-model="formData.confirmPassword"
              :type="showConfirmPassword ? 'text' : 'password'"
              required
              placeholder="Confirm password"
            />
            <button 
              type="button" 
              @click="showConfirmPassword = !showConfirmPassword" 
              class="password-toggle"
              :title="showConfirmPassword ? 'Hide password' : 'Show password'"
            >
              <span class="material-symbols-outlined">
                {{ showConfirmPassword ? 'visibility_off' : 'visibility' }}
              </span>
            </button>
          </div>
          <span v-if="passwordMismatch" class="error">Passwords do not match</span>
        </div>

        <div v-if="error" class="error-message">
          {{ error }}
        </div>

        <div v-if="success" class="success-message">
          {{ success }}
        </div>

        <button type="submit" :disabled="isSubmitting || passwordMismatch">
          {{ isSubmitting ? 'Registering...' : 'Register' }}
        </button>
      </form>
    </div>
    
    <div class="login-section">
      <div class="login-content">
        <h2>Already registered?</h2>
        <p>Log in instead</p>
        <router-link to="/login" class="login-button">Go to Login</router-link>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { success as notifySuccess } from '@/utils/notify'
import { api, throwIfNotOk } from '@/utils/api'

interface RegisterFormData {
  username: string
  email: string
  password: string
  confirmPassword: string
  fullName: string
}

const router = useRouter()
const store = useStore()

const formData = ref<RegisterFormData>({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  fullName: ''
})

const isSubmitting = ref(false)
const error = ref('')
const success = ref('')
const registrationSuccess = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)

const passwordMismatch = computed(() => {
  return formData.value.password !== '' && 
         formData.value.confirmPassword !== '' && 
         formData.value.password !== formData.value.confirmPassword
})

const handleSubmit = async () => {
  error.value = ''
  success.value = ''

  if (passwordMismatch.value) {
    error.value = 'Passwords do not match'
    return
  }

  isSubmitting.value = true

  try {
    const requestBody: any = {
      username: formData.value.username,
      email: formData.value.email,
      password: formData.value.password,
      confirmPassword: formData.value.confirmPassword,
      fullName: formData.value.fullName
    }
    
    const response = await api.post('/auth/register', requestBody)

    await throwIfNotOk(response)

    const data = await response.json()
    
    // Auto-login after successful registration
    if (data.token && data.user) {
      await store.dispatch('login', { token: data.token, user: data.user })
      notifySuccess(`Welcome, ${data.user.fullName}!`)

      router.push('/account/profile')
    } else {
      registrationSuccess.value = true
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'An error occurred during registration'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: 100vh;
  padding: 40px 20px 20px;
}

.register-wrapper {
  display: flex;
  gap: 30px;
  max-width: 900px;
  width: 100%;
}

.register-card {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  flex: 1;
  max-width: 450px;
}

.login-section {
  background: #f8f9fa;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 300px;
}

.login-content {
  text-align: center;
}

.login-content h2 {
  margin: 0 0 10px;
  color: #333;
  font-size: 1.5rem;
}

.login-content p {
  margin: 0 0 25px;
  color: #666;
  font-size: 1rem;
}

.login-button {
  display: inline-block;
  padding: 12px 30px;
  background-color: #2196F3;
  color: white;
  text-decoration: none;
  border-radius: 4px;
  font-weight: 500;
  transition: background-color 0.3s;
}

.login-button:hover {
  background-color: #1976D2;
}

@media (max-width: 768px) {
  .register-wrapper {
    flex-direction: column;
  }
  
  .login-section {
    flex: 1;
  }
}

h1 {
  margin: 0 0 30px;
  text-align: center;
  color: #333;
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

input:read-only {
  background-color: #f5f5f5;
  cursor: not-allowed;
  color: #666;
}

.password-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.password-input-wrapper input {
  padding-right: 45px;
}

.password-toggle {
  position: absolute;
  right: 8px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  transition: color 0.2s;
  width: auto;
}

.password-toggle:hover {
  color: #333;
  background: none;
  opacity: 1;
}

.password-toggle .material-symbols-outlined {
  font-size: 20px;
}

.registration-code-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.registration-code-wrapper input {
  padding-right: 45px;
}

.code-valid-icon {
  position: absolute;
  right: 12px;
  color: #4CAF50;
  font-size: 24px;
  pointer-events: none;
}

.code-invalid-icon {
  position: absolute;
  right: 12px;
  color: #f44336;
  font-size: 24px;
  pointer-events: none;
}

input:focus {
  outline: none;
  border-color: var(--theme-color, #4CAF50);
}

.error {
  display: block;
  color: #f44336;
  font-size: 12px;
  margin-top: 5px;
}

.success-box {
  background-color: rgba(46, 204, 113, 0.1);
  border: 1px solid rgba(46, 204, 113, 0.3);
  color: #27ae60;
  padding: 0.75rem;
  border-radius: 6px;
  font-size: 0.875rem;
  margin-top: 0.5rem;
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

.success-container {
  text-align: center;
  padding: 20px;
}

.success-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background-color: #4CAF50;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: bold;
}

.success-container h2 {
  margin: 0 0 10px;
  color: #333;
  font-size: 1.5rem;
}

.success-container p {
  margin: 0 0 25px;
  color: #666;
  font-size: 1rem;
}

.success-container .login-button {
  display: inline-block;
  padding: 12px 30px;
  background-color: var(--theme-color, #4CAF50);
  color: white;
  text-decoration: none;
  border-radius: 4px;
  font-weight: 500;
  transition: opacity 0.3s;
}

.success-container .login-button:hover {
  opacity: 0.9;
}
</style>
