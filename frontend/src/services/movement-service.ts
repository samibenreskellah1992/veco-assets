import { apiClient } from '@/services/api-client'
import type {
  MovementCreateRequest,
  MovementDto,
  MovementRejectRequest,
  MovementStatus,
  MovementType,
} from '@/types/movement'

export interface MovementListParams {
  assetId?: string
  status?: MovementStatus
  type?: MovementType
}

export const movementsApi = {
  list: async (params?: MovementListParams) => (await apiClient.get<MovementDto[]>('/movements', { params })).data,
  get: async (id: string) => (await apiClient.get<MovementDto>(`/movements/${id}`)).data,
  request: async (request: MovementCreateRequest) => (await apiClient.post<MovementDto>('/movements', request)).data,
  validate: async (id: string) => (await apiClient.post<MovementDto>(`/movements/${id}/validate`)).data,
  reject: async (id: string, request?: MovementRejectRequest) =>
    (await apiClient.post<MovementDto>(`/movements/${id}/reject`, request ?? {})).data,
  execute: async (id: string) => (await apiClient.post<MovementDto>(`/movements/${id}/execute`)).data,
}
