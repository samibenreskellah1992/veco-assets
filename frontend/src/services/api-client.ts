import axios from 'axios'
import { clearStoredToken, getStoredToken } from '@/lib/token-storage'

/** Dispatched on window when a request comes back 401 - see hooks/use-auth.tsx. */
export const UNAUTHORIZED_EVENT = 'veco-assets:unauthorized'

/**
 * Single axios instance for all backend calls. Base URL is empty on
 * purpose: in dev, Vite proxies "/api" to the backend (see vite.config.ts);
 * in production the frontend and API are expected to sit behind the same
 * origin/reverse proxy. Never hardcode a backend host here.
 */
export const apiClient = axios.create({
  baseURL: '/api',
  timeout: 15_000,
})

// Phase 3/4: attach the JWT bearer token (if any) to every request.
apiClient.interceptors.request.use((config) => {
  const token = getStoredToken()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

// A 401 means the token is missing/expired/invalid (JsonAuthenticationEntryPoint,
// see backend/.../security) - never something a retry would fix. Clear the
// stale token and let use-auth.tsx react (it owns navigation to /login so
// this stays a plain axios concern, no router dependency here).
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401 && !error.config?.url?.endsWith('/auth/login')) {
      clearStoredToken()
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT))
    }
    return Promise.reject(error)
  },
)
