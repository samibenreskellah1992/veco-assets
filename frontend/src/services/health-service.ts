import { apiClient } from '@/services/api-client'
import type { HealthStatus } from '@/types/health'

export async function fetchHealth(): Promise<HealthStatus> {
  const { data } = await apiClient.get<HealthStatus>('/health')
  return data
}
