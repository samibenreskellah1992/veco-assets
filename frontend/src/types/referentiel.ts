/** Mirrors the backend DTOs 1:1 (dz.vecopharm.vecoassets.dto) - see docs/ARCHITECTURE.md. */

export interface SiteDto {
  id: string
  code: string
  name: string
  address: string | null
  city: string | null
  active: boolean
}

export interface SiteRequest {
  code: string
  name: string
  address?: string
  city?: string
}

export interface BuildingDto {
  id: string
  siteId: string
  siteName: string
  code: string
  name: string
  active: boolean
}

export interface BuildingRequest {
  siteId: string
  code: string
  name: string
}

export interface FloorDto {
  id: string
  buildingId: string
  buildingName: string
  code: string
  name: string
  active: boolean
}

export interface FloorRequest {
  buildingId: string
  code: string
  name: string
}

export interface ZoneDto {
  id: string
  floorId: string
  floorName: string
  code: string
  name: string
  active: boolean
}

export interface ZoneRequest {
  floorId: string
  code: string
  name: string
}

export type LocationStatus = 'ACTIF' | 'INACTIF' | 'EN_TRAVAUX' | 'FERME' | 'A_INVENTORIER'

export const LOCATION_STATUS_LABEL: Record<LocationStatus, string> = {
  ACTIF: 'Actif',
  INACTIF: 'Inactif',
  EN_TRAVAUX: 'En travaux',
  FERME: 'Fermé',
  A_INVENTORIER: 'À inventorier',
}

export interface LocationDto {
  id: string
  zoneId: string
  zoneName: string
  code: string
  name: string
  active: boolean
  status: LocationStatus
  qrCode: string
  description: string | null
  responsibleUserId: string | null
  responsibleUserName: string | null
  lastInventoryAt: string | null
  assetCount: number
  siteId: string | null
  siteName: string | null
  buildingId: string | null
  buildingName: string | null
  floorId: string | null
  floorName: string | null
}

export interface LocationRequest {
  zoneId: string
  code: string
  name: string
  status?: LocationStatus
  description?: string
  responsibleUserId?: string
}

export interface AssetCategoryDto {
  id: string
  parentId: string | null
  parentName: string | null
  code: string
  name: string
  active: boolean
}

export interface AssetCategoryRequest {
  parentId?: string | null
  code: string
  name: string
}
