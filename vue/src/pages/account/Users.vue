<template>
  <div class="info-section" :class="{ 'panel-open': showModal }">
    <div class="main-content" :class="{ 'minimized': showModal }">
      <div class="header-section">
        <div>
          <h3>Manage Users</h3>
          <p>Invite, remove, or change roles for users.</p>
        </div>
        <button @click="openCreateModal" class="btn-create" title="Create User">
          <span class="material-symbols-outlined">person_add</span>
        </button>
      </div>
      
      <div class="filter-section">
        <input
          v-model="filters.fullName"
          type="text"
          placeholder="Filter by name..."
          class="search-input"
        />
        <input
          v-model="filters.email"
          type="text"
          placeholder="Filter by email..."
          class="search-input"
        />
        <select
          v-model="filters.role"
          class="search-input"
        >
          <option value="">All Roles</option>
          <option v-for="role in availableRoles" :key="role" :value="role">
            {{ role }}
          </option>
        </select>
      </div>
      
      <div class="users-table-container">
        <DataTable
          :columns="[
            { key: 'actions', label: 'Actions' },
            { key: 'fullName', label: 'Full Name', sortable: true },
            { key: 'email', label: 'Email', sortable: true },
            { key: 'role', label: 'Role', sortable: true },
            { key: 'createdAt', label: 'Joined', sortable: true }
          ]"
          :rows="sortedAndFilteredUsers"
          :loading="false"
          :sortColumn="sortColumn"
          :sortDirection="sortDirection"
          @toggle-sort="toggleSort"
          row-key="email"
          table-class="users-table"
        >
          <template #row="{ row }">
            <tr>
              <td>
                <button @click="openEditModal(row)" class="btn-edit" title="Edit user">
                  <span class="material-symbols-outlined">edit</span>
                </button>
              </td>
              <td>{{ row.fullName }}</td>
              <td>{{ row.email }}</td>
              <td>{{ row.role }}</td>
              <td>{{ formatDate(row.createdAt) }}</td>
            </tr>
          </template>

          <template #empty>
            <tr>
              <td colspan="5" class="no-data">No users found</td>
            </tr>
          </template>
        </DataTable>
      </div>
    </div>

    <!-- Edit User Slide Panel -->
    <transition name="slide">
      <div v-if="showModal" class="slide-panel">
        <div class="panel-header">
          <h3>Edit User</h3>
          <button @click="closeModal" class="btn-close">&times;</button>
        </div>
        <form @submit.prevent="handleUpdateUser" class="panel-form">
          <h4 class="section-title">User Information</h4>
          
          <div class="form-group">
            <label for="editFullName">Full Name</label>
            <input
              id="editFullName"
              v-model="editForm.fullName"
              type="text"
              required
            />
          </div>

          <div class="form-group">
            <label for="editEmail">Email</label>
            <input
              id="editEmail"
              v-model="editForm.email"
              type="email"
              required
            />
          </div>

          <div class="form-group">
            <label for="editRole">Role</label>
            <select id="editRole" v-model="editForm.role" required :disabled="isCustomerRole" :class="{ 'readonly-field': isCustomerRole }">
              <option v-for="role in filteredRolesForEdit" :key="role" :value="role">
                {{ role }}
              </option>
            </select>
            <span v-if="isCustomerRole" class="info-text">Customer role cannot be changed</span>
          </div>

          <div class="form-group info-row">
            <label>Username:</label>
            <div class="info-display">{{ editForm.username }}</div>
          </div>

          <div class="form-group info-row">
            <label>Date Created:</label>
            <div class="info-display">{{ formatDate(editForm.createdAt) }}</div>
          </div>

          <div class="panel-actions">
            <button type="button" @click="closeModal" class="btn-cancel">Cancel</button>
            <button type="submit" class="btn-save">
              <span class="material-symbols-outlined">save</span>
              <span>Save Changes</span>
            </button>
          </div>
        </form>
      </div>
    </transition>

    <!-- Overlay -->
    <transition name="fade">
      <div v-if="showModal" class="panel-overlay" @click="closeModal"></div>
    </transition>

    <!-- Create User Modal -->
    <div v-if="showCreateModal" class="modal-overlay" @click="closeCreateModal">
      <div class="modal-content modal-content-wide" @click.stop>
        <div class="modal-header">
          <h3>Create User</h3>
          <button @click="closeCreateModal" class="btn-close">&times;</button>
        </div>
        <form @submit.prevent="handleCreateUser" class="modal-form">
          <div class="form-columns">
            <div class="form-column">
              <h4 class="section-title">User Information</h4>
              
              <div class="form-group">
                <label for="createFullName">Full Name</label>
                <input
                  id="createFullName"
                  v-model="createForm.fullName"
                  type="text"
                  required
                />
              </div>

              <div class="form-group">
                <label for="createUsername">Username</label>
                <input
                  id="createUsername"
                  v-model="createForm.username"
                  type="text"
                  required
                />
              </div>

              <div class="form-group">
                <label for="createEmail">Email</label>
                <input
                  id="createEmail"
                  v-model="createForm.email"
                  type="email"
                  required
                />
              </div>

              <div class="form-group">
                <label for="createRole">Role</label>
                <select id="createRole" v-model="createForm.role" required>
                  <option v-for="role in availableRoles" :key="role" :value="role">
                    {{ role }}
                  </option>
                </select>
              </div>
            </div>

            <div class="form-column password-section">
              <h4 class="section-title">Password Settings</h4>
              
              <div class="form-group checkbox-group">
                <input
                  id="setCustomPassword"
                  v-model="setCustomPassword"
                  type="checkbox"
                />
                <label for="setCustomPassword" class="checkbox-label">Set custom password</label>
              </div>

              <div class="password-info" v-if="!setCustomPassword">
                <span class="material-symbols-outlined">info</span>
                <p>A random password will be generated and sent to the user's email.</p>
              </div>

              <div v-if="setCustomPassword" class="form-group">
                <label for="createPassword">Password</label>
                <input
                  id="createPassword"
                  v-model="createForm.password"
                  type="password"
                  :required="setCustomPassword"
                />
              </div>

              <div v-if="setCustomPassword" class="form-group">
                <label for="createConfirmPassword">Confirm Password</label>
                <input
                  id="createConfirmPassword"
                  v-model="createForm.confirmPassword"
                  type="password"
                  :required="setCustomPassword"
                />
                <span v-if="passwordMismatch" class="error">Passwords do not match</span>
              </div>
            </div>
          </div>

          <div class="modal-actions">
            <button type="button" @click="closeCreateModal" class="btn-cancel">Cancel</button>
            <button type="submit" class="btn-save" :disabled="passwordMismatch">Create User</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import '@/assets/themes.css'
import '@/assets/account-pages.css'
import { ref, onMounted, computed } from 'vue'
import DataTable from '@/components/DataTable.vue'
import { useStore } from 'vuex'
import { formatDate } from '@/utils/dateFormatter'
import { api } from '@/utils/api'

interface User {
  id: string
  fullName: string
  username: string
  email: string
  role: string
  createdAt: string
}

interface CreateUserForm {
  fullName: string
  username: string
  email: string
  password: string
  confirmPassword: string
  role: string
}

const store = useStore()
const users = ref<User[]>([])
const filters = ref({
  fullName: '',
  email: '',
  role: ''
})
const sortColumn = ref<'fullName' | 'email' | 'role' | 'createdAt' | null>(null)
const sortDirection = ref<'asc' | 'desc'>('asc')
const showModal = ref(false)
const showCreateModal = ref(false)
const setCustomPassword = ref(false)
const availableRoles = ref<string[]>([])
const editForm = ref<User>({
  id: '',
  fullName: '',
  username: '',
  email: '',
  role: '',
  createdAt: ''
})
const createForm = ref<CreateUserForm>({
  fullName: '',
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  role: ''
})

const isCustomerRole = computed(() => {
  return editForm.value.role?.toLowerCase() === 'customer'
})

const filteredRolesForEdit = computed(() => {
  const currentRole = editForm.value.role?.toLowerCase()
  const restrictedRoles = ['admin', 'user', 'manager']
  
  // If current role is admin, user, or manager, remove customer from available roles
  if (restrictedRoles.includes(currentRole)) {
    return availableRoles.value.filter(role => role.toLowerCase() !== 'customer')
  }
  
  return availableRoles.value
})

const generateRandomPassword = (): string => {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*'
  let password = ''
  for (let i = 0; i < 8; i++) {
    password += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return password
}

const passwordMismatch = computed(() => {
  if (!setCustomPassword.value) return false
  return createForm.value.password !== '' && 
         createForm.value.confirmPassword !== '' && 
         createForm.value.password !== createForm.value.confirmPassword
})

const filteredUsers = computed(() => {
  return users.value.filter(user => {
    const nameMatch = !filters.value.fullName || 
      user.fullName.toLowerCase().includes(filters.value.fullName.toLowerCase())
    
    const emailMatch = !filters.value.email || 
      user.email.toLowerCase().includes(filters.value.email.toLowerCase())
    
    const roleMatch = !filters.value.role || 
      user.role === filters.value.role
    
    return nameMatch && emailMatch && roleMatch
  })
})

const sortedAndFilteredUsers = computed(() => {
  const filtered = filteredUsers.value
  
  if (!sortColumn.value) {
    return filtered
  }
  
  return [...filtered].sort((a, b) => {
    let comparison = 0
    
    if (sortColumn.value === 'createdAt') {
      // Sort dates chronologically
      const aDate = new Date(a.createdAt).getTime()
      const bDate = new Date(b.createdAt).getTime()
      comparison = aDate - bDate
    } else {
      // Sort strings alphabetically
      const aVal = a[sortColumn.value!].toLowerCase()
      const bVal = b[sortColumn.value!].toLowerCase()
      comparison = aVal < bVal ? -1 : aVal > bVal ? 1 : 0
    }
    
    return sortDirection.value === 'asc' ? comparison : -comparison
  })
})

const toggleSort = (column: 'fullName' | 'email' | 'role' | 'createdAt') => {
  if (sortColumn.value === column) {
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortColumn.value = column
    sortDirection.value = 'asc'
  }
}

const fetchRoles = async () => {
  try {
    const response = await api.get('/users/roles')
    if (response.ok) {
      const data = await response.json()
      availableRoles.value = data
    }
  } catch (error) {
    console.error('Failed to fetch roles:', error)
  }
}

const openEditModal = async (user: User) => {
  editForm.value = { ...user }
  await fetchRoles()
  showModal.value = true
  document.body.classList.add('no-scroll')
}

const closeModal = () => {
  showModal.value = false
  document.body.classList.remove('no-scroll')
  editForm.value = {
    id: '',
    fullName: '',
    username: '',
    email: '',
    role: '',
    createdAt: ''
  }
}

const openCreateModal = async () => {
  await fetchRoles()
  showCreateModal.value = true
  document.body.classList.add('no-scroll')
}

const closeCreateModal = () => {
  showCreateModal.value = false
  document.body.classList.remove('no-scroll')
  setCustomPassword.value = false
  createForm.value = {
    fullName: '',
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    role: ''
  }
}

const handleCreateUser = async () => {
  if (setCustomPassword.value && passwordMismatch.value) {
    return
  }

  // Generate random password if custom password not set
  const passwordToSend = setCustomPassword.value && createForm.value.password 
    ? createForm.value.password 
    : generateRandomPassword()

  try {
    const response = await api.post('/users', {
      fullName: createForm.value.fullName,
      username: createForm.value.username,
      email: createForm.value.email,
      password: passwordToSend,
      role: createForm.value.role
    })
    
    if (response.ok) {
      const newUser = await response.json()
      users.value.push(newUser)
      closeCreateModal()
    }
  } catch (error) {
    console.error('Failed to create user:', error)
  }
}

const handleUpdateUser = async () => {
  try {
    const response = await api.put(`/users/${editForm.value.id}`, editForm.value)
    
    if (response.ok) {
      // Update the user in the local array
      const index = users.value.findIndex(u => u.id === editForm.value.id)
      if (index !== -1) {
        users.value[index] = { ...editForm.value }
      }
      closeModal()
    }
  } catch (error) {
    console.error('Failed to update user:', error)
  }
}

onMounted(async () => {
  await fetchRoles()
  
  try {
    const response = await api.get('/users')
    
    if (response.ok) {
      const data = await response.json()
      users.value = data
    }
  } catch (error) {
    console.error('Failed to fetch users:', error)
  }
})
</script>

<style scoped>
.info-section {
  position: relative;
  display: flex;
  width: 100%;
  overflow: hidden;
}
.users-table-container {
  margin-top: 1.5rem;
  overflow-x: auto;
}

/* Button styling remains specific to Users page */
.btn-edit {
  padding: 6px 12px;
  background-color: var(--theme-color, #42b983);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: opacity 0.3s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-edit .material-symbols-outlined {
  font-size: 18px;
}

.btn-edit:hover {
  opacity: 0.8;
}

/* Slide Panel Styles */
.slide-panel {
  position: fixed;
  top: 0;
  right: 0;
  width: 500px;
  height: 100vh;
  background: white;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.15);
  z-index: 1001;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.slide-enter-active,
.slide-leave-active {
  transition: transform 0.3s ease;
}

.slide-enter-from {
  transform: translateX(100%);
}

.slide-leave-to {
  transform: translateX(100%);
}

.panel-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.3);
  z-index: 1000;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px;
  border-bottom: 1px solid #e8e8e8;
  background: linear-gradient(to bottom, #fafafa, #ffffff);
  flex-shrink: 0;
}

.panel-header h3 {
  margin: 0;
  color: #1a1a1a;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.panel-form {
  padding: 28px;
  overflow-y: auto;
  flex: 1;
}

.panel-form .form-group {
  margin-bottom: 20px;
}

.panel-form .form-group:last-of-type {
  margin-bottom: 0;
}

.panel-form label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #333;
  font-size: 14px;
  letter-spacing: 0.01em;
}

.panel-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid #e8e8e8;
}

.panel-form input,
.panel-form select {
  width: 100%;
  padding: 12px 14px;
  border: 2px solid #d4d4d4;
  border-radius: 8px;
  font-size: 15px;
  box-sizing: border-box;
  transition: all 0.2s ease;
  font-family: inherit;
  background-color: #ffffff;
}

.panel-form input:hover:not(.readonly-field),
.panel-form select:hover {
  border-color: #a0a0a0;
}

.panel-form input:focus:not(.readonly-field),
.panel-form select:focus {
  outline: none;
  border-color: var(--theme-color, #42b983);
  box-shadow: 0 0 0 3px rgba(66, 185, 131, 0.15);
}

.panel-form .section-title {
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  padding-bottom: 12px;
  border-bottom: 2px solid #e8e8e8;
}

.password-section {
  background-color: #fafbfc;
  padding: 20px;
  border-radius: 8px;
  border: 1.5px solid #e8e8e8;
}

.password-info {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px;
  background-color: #e8f5f1;
  border-radius: 6px;
  border: 1px solid #c8e6dd;
  margin-top: 12px;
}

.password-info .material-symbols-outlined {
  color: var(--theme-color, #42b983);
  font-size: 20px;
  flex-shrink: 0;
}

.password-info p {
  margin: 0;
  font-size: 13px;
  color: #2d5a4a;
  line-height: 1.5;
}

@media (max-width: 768px) {
  .slide-panel {
    width: 100%;
  }
  
  .main-content.minimized {
    margin-right: 0;
  }
}
</style>
