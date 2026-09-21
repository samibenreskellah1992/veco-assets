import { apiClient } from '@/services/api-client'
import type {
  AssetCategoryDto,
  AssetCategoryRequest,
  BuildingDto,
  BuildingRequest,
  FloorDto,
  FloorRequest,
  LocationDto,
  LocationRequest,
  SiteDto,
  SiteRequest,
  ZoneDto,
  ZoneRequest,
} from '@/types/referentiel'

// --- Sites -----------------------------------------------------------------
export const sitesApi = {
  list: async () => (await apiClient.get<SiteDto[]>('/sites')).data,
  create: async (request: SiteRequest) => (await apiClient.post<SiteDto>('/sites', request)).data,
  update: async (id: string, request: SiteRequest) => (await apiClient.put<SiteDto>(`/sites/${id}`, request)).data,
  activate: async (id: string) => (await apiClient.post<SiteDto>(`/sites/${id}/activate`)).data,
  deactivate: async (id: string) => (await apiClient.post<SiteDto>(`/sites/${id}/deactivate`)).data,
  remove: async (id: string) => apiClient.delete(`/sites/${id}`),
}

// --- Bâtiments ---------------------------------------------------------------
export const buildingsApi = {
  list: async (siteId?: string) =>
    (await apiClient.get<BuildingDto[]>('/buildings', { params: siteId ? { siteId } : undefined })).data,
  create: async (request: BuildingRequest) => (await apiClient.post<BuildingDto>('/buildings', request)).data,
  update: async (id: string, request: BuildingRequest) =>
    (await apiClient.put<BuildingDto>(`/buildings/${id}`, request)).data,
  activate: async (id: string) => (await apiClient.post<BuildingDto>(`/buildings/${id}/activate`)).data,
  deactivate: async (id: string) => (await apiClient.post<BuildingDto>(`/buildings/${id}/deactivate`)).data,
  remove: async (id: string) => apiClient.delete(`/buildings/${id}`),
}

// --- Étages ------------------------------------------------------------------
export const floorsApi = {
  list: async (buildingId?: string) =>
    (await apiClient.get<FloorDto[]>('/floors', { params: buildingId ? { buildingId } : undefined })).data,
  create: async (request: FloorRequest) => (await apiClient.post<FloorDto>('/floors', request)).data,
  update: async (id: string, request: FloorRequest) => (await apiClient.put<FloorDto>(`/floors/${id}`, request)).data,
  activate: async (id: string) => (await apiClient.post<FloorDto>(`/floors/${id}/activate`)).data,
  deactivate: async (id: string) => (await apiClient.post<FloorDto>(`/floors/${id}/deactivate`)).data,
  remove: async (id: string) => apiClient.delete(`/floors/${id}`),
}

// --- Zones ---------------------------------------------------------------
export const zonesApi = {
  list: async (floorId?: string) =>
    (await apiClient.get<ZoneDto[]>('/zones', { params: floorId ? { floorId } : undefined })).data,
  create: async (request: ZoneRequest) => (await apiClient.post<ZoneDto>('/zones', request)).data,
  update: async (id: string, request: ZoneRequest) => (await apiClient.put<ZoneDto>(`/zones/${id}`, request)).data,
  activate: async (id: string) => (await apiClient.post<ZoneDto>(`/zones/${id}/activate`)).data,
  deactivate: async (id: string) => (await apiClient.post<ZoneDto>(`/zones/${id}/deactivate`)).data,
  remove: async (id: string) => apiClient.delete(`/zones/${id}`),
}

// --- Localisations -------------------------------------------------------
export const locationsApi = {
  list: async (zoneId?: string) =>
    (await apiClient.get<LocationDto[]>('/locations', { params: zoneId ? { zoneId } : undefined })).data,
  get: async (id: string) => (await apiClient.get<LocationDto>(`/locations/${id}`)).data,
  create: async (request: LocationRequest) => (await apiClient.post<LocationDto>('/locations', request)).data,
  update: async (id: string, request: LocationRequest) =>
    (await apiClient.put<LocationDto>(`/locations/${id}`, request)).data,
  activate: async (id: string) => (await apiClient.post<LocationDto>(`/locations/${id}/activate`)).data,
  deactivate: async (id: string) => (await apiClient.post<LocationDto>(`/locations/${id}/deactivate`)).data,
  remove: async (id: string) => apiClient.delete(`/locations/${id}`),
}

// --- Catégories d'immobilisation ------------------------------------------
export const assetCategoriesApi = {
  list: async () => (await apiClient.get<AssetCategoryDto[]>('/asset-categories')).data,
  create: async (request: AssetCategoryRequest) =>
    (await apiClient.post<AssetCategoryDto>('/asset-categories', request)).data,
  update: async (id: string, request: AssetCategoryRequest) =>
    (await apiClient.put<AssetCategoryDto>(`/asset-categories/${id}`, request)).data,
  activate: async (id: string) => (await apiClient.post<AssetCategoryDto>(`/asset-categories/${id}/activate`)).data,
  deactivate: async (id: string) => (await apiClient.post<AssetCategoryDto>(`/asset-categories/${id}/deactivate`)).data,
  remove: async (id: string) => apiClient.delete(`/asset-categories/${id}`),
}
