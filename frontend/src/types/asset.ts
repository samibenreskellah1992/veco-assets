/** Mirrors the backend DTOs 1:1 (dz.vecopharm.vecoassets.dto) - see docs/ARCHITECTURE.md. */

export type AssetCondition = 'NEUF' | 'BON' | 'MOYEN' | 'A_REPARER' | 'HORS_SERVICE' | 'REFORME'
export type AssetStatus = 'EN_STOCK' | 'EN_SERVICE' | 'EN_MAINTENANCE' | 'TRANSFERE' | 'REFORME' | 'SORTI'

export interface AssetDto {
  id: string
  assetCode: string
  designation: string
  categoryId: string
  categoryName: string | null
  brand: string | null
  model: string | null
  serialNumber: string | null
  siteId: string
  siteName: string | null
  buildingId: string | null
  buildingName: string | null
  floorId: string | null
  floorName: string | null
  zoneId: string | null
  zoneName: string | null
  locationId: string | null
  locationName: string | null
  direction: string | null
  department: string | null
  service: string | null
  currentUserId: string | null
  currentUserName: string | null
  responsibleUserId: string | null
  responsibleUserName: string | null
  acquisitionDate: string | null
  supplier: string | null
  invoiceNumber: string | null
  acquisitionValue: number | null
  commissioningDate: string | null
  warrantyUntil: string | null
  condition: AssetCondition
  status: AssetStatus
  comment: string | null
  labeled: boolean
  lastInventoryAt: string | null
  deleted: boolean
  deletedAt: string | null
  createdAt: string
  updatedAt: string
}

/** Champs communs à la création et la modification (voir AssetCreateRequest / AssetUpdateRequest côté backend). */
export interface AssetFormFields {
  designation: string
  categoryId: string
  brand?: string
  model?: string
  serialNumber?: string
  siteId: string
  buildingId?: string
  floorId?: string
  zoneId?: string
  locationId?: string
  direction?: string
  department?: string
  service?: string
  currentUserId?: string
  responsibleUserId?: string
  acquisitionDate?: string
  supplier?: string
  invoiceNumber?: string
  acquisitionValue?: number
  commissioningDate?: string
  warrantyUntil?: string
  comment?: string
}

export interface AssetCreateRequest extends AssetFormFields {
  condition?: AssetCondition
}

export interface AssetUpdateRequest extends AssetFormFields {
  condition: AssetCondition
  status: AssetStatus
  /** Motif facultatif joint à l'historique lorsque l'état, le statut ou l'affectation change. */
  changeComment?: string
}

export interface AssetStatusHistoryDto {
  id: string
  fieldName: 'CONDITION' | 'STATUS'
  oldValue: string | null
  newValue: string
  changedByName: string | null
  changedAt: string
  comment: string | null
}

export interface AssetAssignmentDto {
  id: string
  userId: string | null
  userName: string | null
  direction: string | null
  department: string | null
  service: string | null
  assignedFrom: string
  assignedUntil: string | null
  assignedByName: string | null
  comment: string | null
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface AssetListParams {
  page?: number
  size?: number
  sort?: string
  includeDeleted?: boolean
  siteId?: string
  categoryId?: string
  condition?: AssetCondition
  status?: AssetStatus
  labeled?: boolean
  search?: string
}

export const ASSET_CONDITION_LABEL: Record<AssetCondition, string> = {
  NEUF: 'Neuf',
  BON: 'Bon',
  MOYEN: 'Moyen',
  A_REPARER: 'À réparer',
  HORS_SERVICE: 'Hors service',
  REFORME: 'Réformé',
}

export const ASSET_STATUS_LABEL: Record<AssetStatus, string> = {
  EN_STOCK: 'En stock',
  EN_SERVICE: 'En service',
  EN_MAINTENANCE: 'En maintenance',
  TRANSFERE: 'Transféré',
  REFORME: 'Réformé',
  SORTI: 'Sorti',
}
