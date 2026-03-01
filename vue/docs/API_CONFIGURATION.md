# API Configuration Guide

## Environment Variables

The application uses environment-specific configuration files for API settings:

- **Development**: `.env.development` - Used when running `npm run dev`
- **Production**: `.env.production` - Used when running `npm run build`

### API Base URL Configuration

```env
# Development
VITE_API_BASE_URL=http://localhost:8080/api

# Production
VITE_API_BASE_URL=https://api.waitlist.com/api
```

## API Utility Usage

All API calls should use the centralized API utility located at `src/utils/api.ts`.

### Basic Usage

```typescript
import { apiFetch, api, getApiUrl } from '@/utils/api'

// Using apiFetch directly
const response = await apiFetch('/users', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`
  }
})

// Using convenience methods
const response = await api.get('/users', {
  headers: { 'Authorization': `Bearer ${token}` }
})

const response = await api.post('/users', { 
  name: 'John Doe',
  email: 'john@example.com'
}, {
  headers: { 'Authorization': `Bearer ${token}` }
})
```

### Available Methods

The backend exposes a few account-related endpoints used by the UI.
- `GET /accounts/{id}` – retrieve account data
- `PATCH /accounts/{id}/branding-color` – update theme color
- **`PATCH /accounts/{id}/waitlist-settings`** – set waitlist enable flag and open/close times
- **`GET /accounts/{id}/waitlist-status`** – query whether the waitlist is currently open.  Response now includes an `estimatedWait` field specifying total queue wait time in minutes (5 minutes per party under 4, 10 minutes otherwise).
- **`GET /accounts/{id}/messages`** – retrieve the map of editable message templates for the account.
- **`PATCH /accounts/{id}/messages`** – replace the account's message templates with the provided key/value object. This is used by the new *Account Messages* page.
- **`GET /entries/stream`** – server‑sent events stream that pushes waitlist entry changes (new, deleted, updated)

  All entry‑specific endpoints accept either the numeric `id` or the public 6‑character `code` in the path.  Clients should prefer the code for security and cleaner URLs.

  Event payloads now reference entries by their public `code` instead of numeric IDs.
  Additionally, `new-entry`, `updated-entry`, and `deleted-entry` events now send an object with two keys: `entry` (or `code` for deletion) and `estimatedWait`.
- **`POST /entries/{id}` or `{code}/sms`** – send an SMS to a waitlist entry (JSON body must contain `"message"`).
  Path segment may be either numeric ID or the publicly visible 6‑character code.
  - events: `new-entry`, `deleted-entry`, `updated-entry`, `notified-entry` (id of entry whose table is ready)

  *Note:* the backend also exposes a Twilio integration endpoint to text an entry.


- **`api.get(endpoint, options)`** - GET request
- **`api.post(endpoint, data, options)`** - POST request with JSON body
- **`api.put(endpoint, data, options)`** - PUT request with JSON body
- **`api.patch(endpoint, data, options)`** - PATCH request with JSON body
- **`api.delete(endpoint, options)`** - DELETE request

### Features

1. **Automatic Base URL**: All requests automatically use `VITE_API_BASE_URL`
2. **Request Timeout**: Configurable via `VITE_API_TIMEOUT` (default: 30000ms)
3. **JSON Headers**: Content-Type is automatically set to `application/json`
4. **Timeout Handling**: Requests abort after the configured timeout period
5. **Type Safety**: Full TypeScript support with proper typing

### Getting Full URLs

If you need the complete URL (e.g., for constructing URL objects):

```typescript
import { getApiUrl } from '@/utils/api'

const fullUrl = getApiUrl('/users')
// Returns: http://localhost:8080/api/users (in development)

// For URL objects with query params
const url = new URL(getApiUrl('/users'))
url.searchParams.append('page', '1')
```

## Migration from Direct Fetch

Before:
```typescript
const response = await fetch('http://localhost:8080/api/users', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify(data)
})
```

After:
```typescript
import { api } from '@/utils/api'

const response = await api.post('/users', data)
// OR
import { apiFetch } from '@/utils/api'

const response = await apiFetch('/users', {
  method: 'POST',
  body: JSON.stringify(data)
})
```

## Benefits

1. **Centralized Configuration**: Change API base URL in one place
2. **Environment-Specific**: Automatically uses correct endpoint per environment
3. **Consistent Error Handling**: Uniform timeout and error management
4. **Cleaner Code**: Less boilerplate in each API call
5. **Type Safety**: Better TypeScript support and autocomplete
