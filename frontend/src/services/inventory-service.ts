import { apiClient } from '@/services/api-client'
import type { AssetDto } from '@/types/asset'
import type {
  AttachmentDto,
  CampaignProgressDto,
  CampaignStatus,
  InventoryAnomalyDto,
  InventoryCampaignDto,
  InventoryCampaignRequest,
  InventoryScanDto,
  LocationInventorySessionDto,
  LocationSessionProgressDto,
  ScanRequest,
  ScanResponse,
  AnomalyStatus,
} from '@/types/inventory'

export const inventoryCampaignsApi = {
  list: async (params?: { siteId?: string; status?: CampaignStatus }) =>
    (await apiClient.get<InventoryCampaignDto[]>('/inventory-campaigns', { params })).data,
  get: async (id: string) => (await apiClient.get<InventoryCampaignDto>(`/inventory-campaigns/${id}`)).data,
  create: async (request: InventoryCampaignRequest) =>
    (await apiClient.post<InventoryCampaignDto>('/inventory-campaigns', request)).data,
  update: async (id: string, request: InventoryCampaignRequest) =>
    (await apiClient.put<InventoryCampaignDto>(`/inventory-campaigns/${id}`, request)).data,
  advance: async (id: string, status: CampaignStatus) =>
    (await apiClient.put<InventoryCampaignDto>(`/inventory-campaigns/${id}/status`, { status })).data,
  validate: async (id: string) => (await apiClient.post<InventoryCampaignDto>(`/inventory-campaigns/${id}/validate`)).data,
  close: async (id: string) => (await apiClient.post<InventoryCampaignDto>(`/inventory-campaigns/${id}/close`)).data,
  progress: async (id: string) => (await apiClient.get<CampaignProgressDto>(`/inventory-campaigns/${id}/progress`)).data,
  pendingAssets: async (id: string) => (await apiClient.get<AssetDto[]>(`/inventory-campaigns/${id}/pending-assets`)).data,
  scans: async (id: string) => (await apiClient.get<InventoryScanDto[]>(`/inventory-campaigns/${id}/scans`)).data,
  anomalies: async (id: string) => (await apiClient.get<InventoryAnomalyDto[]>(`/inventory-campaigns/${id}/anomalies`)).data,
  scan: async (id: string, request: ScanRequest) =>
    (await apiClient.post<ScanResponse>(`/inventory-campaigns/${id}/scans`, request)).data,
}

export const locationInventorySessionsApi = {
  list: async (params?: { locationId?: string }) =>
    (await apiClient.get<LocationInventorySessionDto[]>('/location-inventory-sessions', { params })).data,
  get: async (id: string) => (await apiClient.get<LocationInventorySessionDto>(`/location-inventory-sessions/${id}`)).data,
  open: async (locationId: string) =>
    (await apiClient.post<LocationInventorySessionDto>('/location-inventory-sessions', { locationId })).data,
  validate: async (id: string) =>
    (await apiClient.post<LocationInventorySessionDto>(`/location-inventory-sessions/${id}/validate`)).data,
  progress: async (id: string) =>
    (await apiClient.get<LocationSessionProgressDto>(`/location-inventory-sessions/${id}/progress`)).data,
  pendingAssets: async (id: string) =>
    (await apiClient.get<AssetDto[]>(`/location-inventory-sessions/${id}/pending-assets`)).data,
  scans: async (id: string) => (await apiClient.get<InventoryScanDto[]>(`/location-inventory-sessions/${id}/scans`)).data,
  anomalies: async (id: string) =>
    (await apiClient.get<InventoryAnomalyDto[]>(`/location-inventory-sessions/${id}/anomalies`)).data,
  scan: async (id: string, request: ScanRequest) =>
    (await apiClient.post<ScanResponse>(`/location-inventory-sessions/${id}/scans`, request)).data,
}

export const assetInventoryHistoryApi = {
  forAsset: async (assetId: string) => (await apiClient.get<InventoryScanDto[]>(`/assets/${assetId}/inventory-scans`)).data,
}

export const inventoryAnomaliesApi = {
  listAll: async () => (await apiClient.get<InventoryAnomalyDto[]>('/inventory-anomalies')).data,
  updateStatus: async (id: string, status: AnomalyStatus) =>
    (await apiClient.put<InventoryAnomalyDto>(`/inventory-anomalies/${id}/status`, { status })).data,
  photos: async (id: string) => (await apiClient.get<AttachmentDto[]>(`/inventory-anomalies/${id}/photos`)).data,
  attachPhoto: async (id: string, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return (
      await apiClient.post<AttachmentDto>(`/inventory-anomalies/${id}/photos`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
    ).data
  },
}

export const attachmentsApi = {
  /** Download URL for an <img>/<a> - the JWT is attached by the axios interceptor, not usable directly as a plain <img src>; use attachmentsApi.fetchBlob for inline previews instead. */
  downloadUrl: (id: string) => `/api/attachments/${id}/download`,
  fetchBlob: async (id: string) => (await apiClient.get(`/attachments/${id}/download`, { responseType: 'blob' })).data as Blob,
}
