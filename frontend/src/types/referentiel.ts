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

export interface LocationDto {
  id: string
  zoneId: string
  zoneName: string
  code: string
  name: string
  active: boolean
}

export interface LocationRequest {
  zoneId: string
  code: string
  name: string
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
