<template>
  <div class="home">
    <h2>Welcome</h2>
    <p v-if="message">{{ message }}</p>
    <p v-else>Loading...</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '@/utils/api'
import store from '@/store'
import '@/assets/themes.css'

const message = ref<string>('')
const statusError = ref('')

// perform the fetches after the component has mounted so we don't block
onMounted(async () => {
  try {
    const response = await api.get('/hello')
    if (response.ok) {
      const data = await response.json()
      message.value = data.message || JSON.stringify(data)
    } else {
      message.value = `Error: ${response.status} ${response.statusText}`
    }
  } catch (error) {
    message.value = `Failed to fetch: ${error instanceof Error ? error.message : 'Unknown error'}`
  }
})
</script>

<style scoped>
.home {
  padding: 1rem;
}

h2 {
  color: var(--theme-color, #42b983);
  margin-bottom: 1rem;
}

p {
  color: #666;
}
</style>
