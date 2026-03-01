<template>
  <div class="login-container">
    <div class="login-wrapper">
      <div class="login-card">
        <h1>Login</h1>
        <div v-if="sessionTimeoutMessage" class="info-message">
          {{ sessionTimeoutMessage }}
        </div>
        <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="username">Username or Email</label>
          <input
            id="username"
            v-model="formData.username"
            type="text"
            required
            placeholder="Enter your username or email"
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

        <div v-if="error" class="error-message">
          {{ error }}
          <div class="forgot-password-link">
            Forgot password? <button type="button" @click="openForgotPasswordModal" class="link-button">Click here to reset it.</button>
          </div>
        </div>

        <div v-if="success" class="success-message">
          {{ success }}
        </div>

        <button type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? 'Logging in...' : 'Login' }}
        </button>
      </form>
    </div>
    
    <div class="register-section">
      <div class="register-content">
        <h2>Don't have an account?</h2>
        <p>Creating an account is easy. Click the button below.</p>
        <router-link to="/register" class="register-button">Register Here</router-link>
      </div>
    </div>
    </div>

    <!-- Forgot Password Modal -->
    <div v-if="showForgotPasswordModal" class="modal-overlay" @click="closeForgotPasswordModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>Reset Password</h3>
          <button @click="closeForgotPasswordModal" class="btn-close">&times;</button>
        </div>
        <form @submit.prevent="handleForgotPassword" class="modal-form">
          <p class="modal-description">Enter your email or username to reset your password.</p>
          
          <div class="form-group">
            <label for="resetEmail">Email</label>
            <input
              id="resetEmail"
              v-model="forgotPasswordForm.email"
              type="email"
              placeholder="Enter your email"
            />
          </div>

          <div class="separator">OR</div>

          <div class="form-group">
            <label for="resetUsername">Username</label>
            <input
              id="resetUsername"
              v-model="forgotPasswordForm.username"
              type="text"
              placeholder="Enter your username"
            />
          </div>

          <div v-if="forgotPasswordError" class="error-message">
            {{ forgotPasswordError }}
          </div>

          <div v-if="forgotPasswordSuccess" class="success-message">
            {{ forgotPasswordSuccess }}
          </div>

          <div v-if="!forgotPasswordSuccess" class="modal-actions">
            <button type="button" @click="closeForgotPasswordModal" class="btn-cancel">Cancel</button>
            <button type="submit" class="btn-submit" :disabled="isSubmittingForgotPassword || (!forgotPasswordForm.email && !forgotPasswordForm.username)">
              {{ isSubmittingForgotPassword ? 'Sending...' : 'Send Reset Link' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { success as notifySuccess } from '@/utils/notify'
import { api, throwIfNotOk } from '@/utils/api'

interface LoginFormData {
  username: string
  password: string
}

const store = useStore()
const router = useRouter()

const formData = ref<LoginFormData>({
  username: '',
  password: ''
})

const isSubmitting = ref(false)
const error = ref('')
const success = ref('')
const showPassword = ref(false)
const showForgotPasswordModal = ref(false)
const forgotPasswordForm = ref({
  email: '',
  username: ''
})
const isSubmittingForgotPassword = ref(false)
const forgotPasswordError = ref('')
const forgotPasswordSuccess = ref('')
const sessionTimeoutMessage = ref('')

// Check for session timeout
if (sessionStorage.getItem('sessionTimeout') === 'true') {
  sessionTimeoutMessage.value = 'Your session timed out, log in again to continue'
  sessionStorage.removeItem('sessionTimeout')
}

const openForgotPasswordModal = () => {
  showForgotPasswordModal.value = true
  forgotPasswordError.value = ''
  forgotPasswordSuccess.value = ''
}

const closeForgotPasswordModal = () => {
  showForgotPasswordModal.value = false
  forgotPasswordForm.value = {
    email: '',
    username: ''
  }
  forgotPasswordError.value = ''
  forgotPasswordSuccess.value = ''
}

const handleForgotPassword = async () => {
  forgotPasswordError.value = ''
  forgotPasswordSuccess.value = ''
  isSubmittingForgotPassword.value = true

  try {
    const response = await api.post('/auth/forgot-password', {
      email: forgotPasswordForm.value.email || undefined,
      username: forgotPasswordForm.value.username || undefined
    })

    await throwIfNotOk(response)

    const data = await response.json()
    forgotPasswordSuccess.value = data.message || 'Password reset link has been sent to your email.'
  } catch (err) {
    forgotPasswordError.value = err instanceof Error ? err.message : 'An error occurred'
  } finally {
    isSubmittingForgotPassword.value = false
  }
}

const handleSubmit = async () => {
  error.value = ''
  success.value = ''

  isSubmitting.value = true

  try {
    const response = await api.post('/auth/login', {
      username: formData.value.username,
      password: formData.value.password
    })

    await throwIfNotOk(response)

    const data = await response.json()

    // Store token and user data in Vuex
    store.dispatch('login', {
      token: data.token,
      user: data.user
    })

    success.value = 'Login successful!'

    // Show welcome toast
    notifySuccess(`Welcome back, ${data.user.fullName}!`)

    // we continue to honor a session-timeout redirect, but otherwise always
    // land on the account profile page. this keeps things simple and meets the
    // new requirement.
    const redirectPath = sessionStorage.getItem('redirectAfterLogin')
    if (redirectPath) {
      sessionStorage.removeItem('redirectAfterLogin')
    }

    setTimeout(() => {
      router.push(redirectPath || '/account/profile')
    }, 500)
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'An error occurred during login'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
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
  display: inline-block;
  padding: 12px 30px;
  background-color: #2196F3;
  color: white;
  text-decoration: none;
  border-radius: 4px;
  font-weight: 500;
  transition: background-color 0.3s;
}

.register-button:hover {
  background-color: #1976D2;
}

@media (max-width: 768px) {
  .login-wrapper {
    flex-direction: column;
  }
  
  .register-section {
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

input:focus {
  outline: none;
  border-color: var(--theme-color, #4CAF50);
}

.info-message {
  padding: 12px;
  background-color: #fff3cd;
  color: #856404;
  border-radius: 4px;
  margin-bottom: 20px;
  border: 1px solid #ffeaa7;
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

.forgot-password-link {
  margin-top: 10px;
  font-size: 14px;
}

.link-button {
  background: none;
  border: none;
  color: #2196F3;
  text-decoration: underline;
  cursor: pointer;
  padding: 0;
  font-size: 14px;
}

.link-button:hover {
  color: #1976D2;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 450px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
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

.modal-form {
  padding: 20px;
}

.modal-description {
  color: #666;
  margin: 0 0 20px;
  font-size: 14px;
}

.separator {
  text-align: center;
  color: #999;
  font-weight: 600;
  margin: 15px 0;
  position: relative;
}

.separator::before,
.separator::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 40%;
  height: 1px;
  background-color: #ddd;
}

.separator::before {
  left: 0;
}

.separator::after {
  right: 0;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.btn-cancel,
.btn-submit {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: opacity 0.3s;
}

.btn-cancel {
  background-color: #e6e6e6;
  color: #333;
}

.btn-cancel:hover {
  opacity: 0.8;
}

.btn-submit {
  background-color: var(--theme-color, #4CAF50);
  color: white;
}

.btn-submit:hover:not(:disabled) {
  opacity: 0.8;
}

.btn-submit:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}
</style>
