import { apiClient } from '@/services/api-client'
import type { DashboardDto } from '@/types/dashboard'

export const dashboardApi = {
  get: async () => (await apiClient.get<DashboardDto>('/dashboard')).data,
}
