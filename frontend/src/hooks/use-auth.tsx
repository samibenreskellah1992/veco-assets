import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { fetchCurrentUser, loginRequest } from '@/services/auth-service'
import { UNAUTHORIZED_EVENT } from '@/services/api-client'
import { clearStoredToken, getStoredToken, setStoredToken } from '@/lib/token-storage'
import type { UserSummary } from '@/types/auth'

interface AuthContextValue {
  user: UserSummary | null
  /** True only while restoring a session from a stored token on first load. */
  isLoading: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  /** Backend is always the source of truth (@PreAuthorize) - this only avoids showing controls the user can't use. */
  hasPermission: (code: string) => boolean
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const token = getStoredToken()
    if (!token) {
      setIsLoading(false)
      return
    }
    fetchCurrentUser()
      .then(setUser)
      .catch(() => clearStoredToken())
      .finally(() => setIsLoading(false))
  }, [])

  useEffect(() => {
    function handleUnauthorized() {
      setUser(null)
    }
    window.addEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
  }, [])

  async function login(email: string, password: string) {
    const response = await loginRequest(email, password)
    setStoredToken(response.accessToken)
    setUser(response.user)
  }

  function logout() {
    clearStoredToken()
    setUser(null)
  }

  function hasPermission(code: string) {
    return user?.permissions.includes(code) ?? false
  }

  return (
    <AuthContext.Provider value={{ user, isLoading, login, logout, hasPermission }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth doit etre utilise a l\'interieur de <AuthProvider>')
  }
  return context
}
