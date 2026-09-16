const STORAGE_KEY = 'veco-assets.accessToken'

/**
 * Single place that touches localStorage for the JWT. Kept tiny and
 * isolated so the storage mechanism (localStorage today) can change later
 * without hunting through the app - api-client.ts and use-auth.tsx are the
 * only callers.
 */
export function getStoredToken(): string | null {
  try {
    return localStorage.getItem(STORAGE_KEY)
  } catch {
    return null
  }
}

export function setStoredToken(token: string): void {
  try {
    localStorage.setItem(STORAGE_KEY, token)
  } catch {
    // localStorage unavailable (private browsing, quota) - the session
    // simply won't survive a reload, which is a degraded but safe fallback.
  }
}

export function clearStoredToken(): void {
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch {
    // no-op
  }
}
