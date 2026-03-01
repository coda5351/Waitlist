/**
 * API Utility Module
 * Provides centralized API configuration and fetch wrapper
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL
const API_TIMEOUT = parseInt(import.meta.env.VITE_API_TIMEOUT) || 30000

/**
 * Constructs a full API URL from a path
 * @param path - API endpoint path (e.g., '/users' or 'users')
 * @returns Full API URL
 */
export function getApiUrl(path: string): string {
  // Remove leading slash if present to avoid double slashes
  const cleanPath = path.startsWith('/') ? path.slice(1) : path

  // Normalise base URL by trimming any trailing slash
  let cleanBase = API_BASE_URL.endsWith('/') ? API_BASE_URL.slice(0, -1) : API_BASE_URL

  // backend endpoints are mounted under /api; if the environment variable was
  // supplied without that segment we add it here so the front‑end doesn't hit
  // the unprotected /auth/login path and receive a 401.  This guards against a
  // common misconfiguration (see issue #…)
  if (!cleanBase.endsWith('/api')) {
    cleanBase += '/api'
  }

  return `${cleanBase}/${cleanPath}`
}

/**
 * Enhanced fetch wrapper with automatic base URL and timeout handling
 * @param endpoint - API endpoint path
 * @param options - Standard fetch options
 * @returns Fetch response
 */
import store from '../store'

export async function apiFetch(endpoint: string, options: RequestInit = {}): Promise<Response> {
  const url = getApiUrl(endpoint)
  
  // Create abort controller for timeout
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), API_TIMEOUT)
  
  try {
    // Build headers, injecting Authorization if we have a token in the store
    const authHeader = store?.getters?.token ? { Authorization: `Bearer ${store.getters.token}` } : {}
    const response = await fetch(url, {
      ...options,
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
        ...authHeader,
        ...options.headers,
      },
    })
    
    clearTimeout(timeoutId)
    
    // Handle 403 Forbidden - Session timeout
    if (response.status === 403) {
      // Store the current path for redirect after login
      const currentPath = window.location.pathname
      if (currentPath !== '/login') {
        sessionStorage.setItem('redirectAfterLogin', currentPath)
        sessionStorage.setItem('sessionTimeout', 'true')
        
        // Redirect to login
        window.location.href = '/login'
      }
    }
    
    return response
  } catch (error) {
    clearTimeout(timeoutId)
    if (error instanceof Error && error.name === 'AbortError') {
      throw new Error(`Request timeout after ${API_TIMEOUT}ms`)
    }
    throw error
  }
}

/**
 * Parse an error response into a string message.
 * Tries to parse JSON { message } or falls back to text/statusText.
 */
export async function parseErrorResponse(response: Response): Promise<string> {
  try {
    const txt = await response.text()
    if (!txt) return response.statusText || `HTTP ${response.status}`

    try {
      const json = JSON.parse(txt)
      if (json && (json.message || json.error)) return json.message || json.error
    } catch (e) {
      // not json
    }

    return txt
  } catch (e) {
    return response.statusText || `HTTP ${response.status}`
  }
}

/**
 * Throws an Error if response is not ok. Uses parseErrorResponse to get message.
 */
export async function throwIfNotOk(response: Response): Promise<void> {
  if (!response.ok) {
    const msg = await parseErrorResponse(response)
    throw new Error(msg || `HTTP ${response.status}`)
  }
}

/**
 * API helper methods for common operations
 */
export const api = {
  /**
   * GET request
   */
  async get(endpoint: string, options: RequestInit = {}) {
    return apiFetch(endpoint, { ...options, method: 'GET' })
  },

  /**
   * POST request
   */
  async post(endpoint: string, data?: unknown, options: RequestInit = {}) {
    return apiFetch(endpoint, {
      ...options,
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    })
  },

  /**
   * PUT request
   */
  async put(endpoint: string, data?: unknown, options: RequestInit = {}) {
    return apiFetch(endpoint, {
      ...options,
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
    })
  },

  /**
   * DELETE request
   */
  async delete(endpoint: string, options: RequestInit = {}) {
    return apiFetch(endpoint, { ...options, method: 'DELETE' })
  },

  /**
   * PATCH request
   */
  async patch(endpoint: string, data?: unknown, options: RequestInit = {}) {
    return apiFetch(endpoint, {
      ...options,
      method: 'PATCH',
      body: data ? JSON.stringify(data) : undefined,
    })
  },
}
