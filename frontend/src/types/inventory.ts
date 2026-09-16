/** Mirrors the backend DTOs 1:1 (dz.vecopharm.vecoassets.dto) - see docs/ARCHITECTURE.md, Phase 7 (Inventaire). */

export type CampaignStatus = 'BROUILLON' | 'EN_PREPARATION' | 'EN_COURS' | 'TERMINE' | 'VALIDE' | 'CLOTURE'
export type ScanResult = 'PRESENT' | 'ANOMALIE'
export type AnomalyType =
  | 'INTROUVABLE'
  | 'MAUVAISE_LOCALISATION'
  | 'MAUVAIS_UTILISATEUR'
  | 'NUMERO_SERIE_DIFFERENT'
  | 'NON_REFERENCEE'
  | 'DOUBLON'
  | 'ETIQUETTE_DETERIOREE'
  | 'ETIQUETTE_ABSENTE'
  | 'HORS_SERVICE'
  | 'AUTRE'
export type AnomalyStatus = 'NOUVELLE' | 'EN_COURS' | 'RESOLUE' | 'REJETEE'

export interface InventoryCampaignDto {
  id: string
  name: string
  siteId: string
  siteName: string | null
  zoneId: string | null
  zoneName: string | null
  responsibleUserId: string | null
  responsibleUserName: string | null
  startDate: string
  endDate: string
  status: CampaignStatus
  createdAt: string
  updatedAt: string
}

export interface InventoryCampaignRequest {
  name: string
  siteId: string
  zoneId?: string
  responsibleUserId?: string
  startDate: string
  endDate: string
}

export interface CampaignProgressDto {
  campaignId: string
  totalAssetsInScope: number
  scannedAssetsCount: number
  presentCount: number
  anomaliesCount: number
  remainingCount: number
  progressPercent: number
}

export interface InventoryScanDto {
  id: string
  campaignId: string
  campaignName: string | null
  assetId: string | null
  assetCode: string | null
  assetDesignation: string | null
  scannedById: string | null
  scannedByName: string | null
  scannedAt: string
  result: ScanResult
  comment: string | null
}

export interface ScanRequest {
  assetCode: string
  result: ScanResult
  anomalyType?: AnomalyType
  comment?: string
}

export interface InventoryAnomalyDto {
  id: string
  campaignId: string
  campaignName: string | null
  assetId: string | null
  assetCode: string | null
  assetDesignation: string | null
  scanId: string | null
  anomalyType: AnomalyType
  description: string | null
  reportedById: string | null
  reportedByName: string | null
  status: AnomalyStatus
  createdAt: string
  updatedAt: string
}

export interface ScanResponse {
  assetRecognized: boolean
  scan: InventoryScanDto | null
  anomaly: InventoryAnomalyDto | null
}

export interface AttachmentDto {
  id: string
  ownerType: string
  ownerId: string
  category: string
  fileName: string
  contentType: string | null
  sizeBytes: number | null
  uploadedById: string | null
  uploadedByName: string | null
  createdAt: string
}

export const CAMPAIGN_STATUS_LABEL: Record<CampaignStatus, string> = {
  BROUILLON: 'Brouillon',
  EN_PREPARATION: 'En préparation',
  EN_COURS: 'En cours',
  TERMINE: 'Terminé',
  VALIDE: 'Validé',
  CLOTURE: 'Clôturé',
}

/** Ordre strict du workflow (voir InventoryCampaignService#transition côté backend) - jamais de saut d'étape. */
export const CAMPAIGN_STATUS_ORDER: CampaignStatus[] = [
  'BROUILLON',
  'EN_PREPARATION',
  'EN_COURS',
  'TERMINE',
  'VALIDE',
  'CLOTURE',
]

export const ANOMALY_TYPE_LABEL: Record<AnomalyType, string> = {
  INTROUVABLE: 'Introuvable',
  MAUVAISE_LOCALISATION: 'Mauvaise localisation',
  MAUVAIS_UTILISATEUR: 'Mauvais utilisateur',
  NUMERO_SERIE_DIFFERENT: 'Numéro de série différent',
  NON_REFERENCEE: 'Non référencée',
  DOUBLON: 'Doublon',
  ETIQUETTE_DETERIOREE: 'Étiquette détériorée',
  ETIQUETTE_ABSENTE: 'Étiquette absente',
  HORS_SERVICE: 'Hors service',
  AUTRE: 'Autre',
}

export const ANOMALY_STATUS_LABEL: Record<AnomalyStatus, string> = {
  NOUVELLE: 'Nouvelle',
  EN_COURS: 'En cours',
  RESOLUE: 'Résolue',
  REJETEE: 'Rejetée',
}
