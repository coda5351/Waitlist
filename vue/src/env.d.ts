/// <reference types="vite/client" />

declare module 'vuex' {
  export * from 'vuex/types/index.d.ts'
  export { Store } from 'vuex/types/index.d.ts'
}

interface ImportMetaEnv {
  // Application Settings
  readonly VITE_APP_TITLE: string
  readonly VITE_APP_ENV: 'development' | 'production'
  
  // API Configuration
  readonly VITE_API_BASE_URL: string
  readonly VITE_API_TIMEOUT: string
  
  // Feature Flags
  readonly VITE_ENABLE_DEBUG: string
  readonly VITE_ENABLE_MOCK_DATA: string
  
  // Authentication
  readonly VITE_AUTH_TOKEN_KEY: string
  readonly VITE_SESSION_TIMEOUT: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
