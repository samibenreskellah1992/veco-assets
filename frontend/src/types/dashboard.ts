/** Mirrors the backend DTOs 1:1 (dz.vecopharm.vecoassets.dto) - see docs/ARCHITECTURE.md, Phase 9 (Reporting). */

export interface CountByLabelDto {
  label: string
  count: number
}

/**
 * Ligne d'audit affichée dans l'"activité récente" du tableau de bord -
 * reprend {@code dto/AuditLogDto.java}, jamais exposé côté frontend avant
 * cette phase (l'écran Administration > Audit reste un placeholder Phase 10).
 */
export interface AuditLogDto {
  id: string
  userFullName: string | null
  action: string
  module: string
  entityName: string | null
  entityId: string | null
  oldValue: string | null
  newValue: string | null
  ipAddress: string | null
  occurredAt: string
}

export interface DashboardDto {
  totalAssets: number
  labeledAssets: number
  unlabeledAssets: number
  inventoriedAssets: number
  notInventoriedAssets: number
  openAnomalies: number
  inStock: number
  inService: number
  inMaintenance: number
  reformed: number
  exited: number
  totalAcquisitionValue: number
  bySite: CountByLabelDto[]
  byCategory: CountByLabelDto[]
  byCondition: CountByLabelDto[]
  recentActivity: AuditLogDto[]
}
