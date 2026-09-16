import { apiClient } from '@/services/api-client'
import type {
  AssetAssignmentDto,
  AssetCreateRequest,
  AssetDto,
  AssetListParams,
  AssetStatusHistoryDto,
  AssetUpdateRequest,
  PageResponse,
} from '@/types/asset'

export const assetsApi = {
  list: async (params: AssetListParams) => (await apiClient.get<PageResponse<AssetDto>>('/assets', { params })).data,
  get: async (id: string) => (await apiClient.get<AssetDto>(`/assets/${id}`)).data,
  create: async (request: AssetCreateRequest) => (await apiClient.post<AssetDto>('/assets', request)).data,
  update: async (id: string, request: AssetUpdateRequest) =>
    (await apiClient.put<AssetDto>(`/assets/${id}`, request)).data,
  archive: async (id: string) => (await apiClient.post<AssetDto>(`/assets/${id}/archive`)).data,
  statusHistory: async (id: string) => (await apiClient.get<AssetStatusHistoryDto[]>(`/assets/${id}/status-history`)).data,
  assignments: async (id: string) => (await apiClient.get<AssetAssignmentDto[]>(`/assets/${id}/assignments`)).data,
}
