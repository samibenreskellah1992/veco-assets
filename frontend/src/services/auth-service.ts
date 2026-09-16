import { apiClient } from '@/services/api-client'
import type { LoginResponse, UserSummary } from '@/types/auth'

export async function loginRequest(email: string, password: string): Promise<LoginResponse> {
  const { data } = await apiClient.post<LoginResponse>('/auth/login', { email, password })
  return data
}

export async function fetchCurrentUser(): Promise<UserSummary> {
  const { data } = await apiClient.get<UserSummary>('/auth/me')
  return data
}
