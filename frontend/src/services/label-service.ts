import { apiClient } from '@/services/api-client'
import type { AssetLabelDto, AssetLabelFormatDto, AssetLabelFormatRequest, LabelGenerationRequest } from '@/types/label'

export const assetLabelFormatsApi = {
  list: async () => (await apiClient.get<AssetLabelFormatDto[]>('/asset-label-formats')).data,
  create: async (request: AssetLabelFormatRequest) =>
    (await apiClient.post<AssetLabelFormatDto>('/asset-label-formats', request)).data,
  update: async (id: string, request: AssetLabelFormatRequest) =>
    (await apiClient.put<AssetLabelFormatDto>(`/asset-label-formats/${id}`, request)).data,
  activate: async (id: string) => (await apiClient.post<AssetLabelFormatDto>(`/asset-label-formats/${id}/activate`)).data,
  deactivate: async (id: string) => (await apiClient.post<AssetLabelFormatDto>(`/asset-label-formats/${id}/deactivate`)).data,
  remove: async (id: string) => apiClient.delete(`/asset-label-formats/${id}`),
}

export const labelsApi = {
  /** Returns the real generated PDF as a Blob - used for both inline preview (object URL) and download. */
  generate: async (request: LabelGenerationRequest) =>
    (await apiClient.post('/labels/generate', request, { responseType: 'blob' })).data as Blob,
  historyForAsset: async (assetId: string) => (await apiClient.get<AssetLabelDto[]>(`/assets/${assetId}/labels`)).data,
}
