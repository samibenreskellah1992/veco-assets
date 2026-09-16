/** Mirrors the backend DTOs 1:1 (dz.vecopharm.vecoassets.dto) - see docs/ARCHITECTURE.md, Phase 9 (Reporting). */

import type { AssetCondition, AssetStatus } from '@/types/asset'
import type { AnomalyStatus } from '@/types/inventory'
import type { MovementType } from '@/types/movement'

export type ReportType =
  | 'PAR_SITE'
  | 'PAR_CATEGORIE'
  | 'PAR_SERVICE'
  | 'PAR_UTILISATEUR'
  | 'PAR_ETAT'
  | 'NON_ETIQUETEES'
  | 'NON_INVENTORIEES'
  | 'ANOMALIES'
  | 'MOUVEMENTS'
  | 'TRANSFERTS'
  | 'REFORMES'

export type ExportFormat = 'CSV' | 'XLSX' | 'PDF'

export interface ReportResultDto {
  type: ReportType
  title: string
  generatedAt: string
  columns: string[]
  rows: string[][]
}

/** Filtres optionnels communs à tous les rapports (voir ReportFilter côté backend) - chaque type n'en retient que le sous-ensemble pertinent. */
export interface ReportFilterParams {
  siteId?: string
  categoryId?: string
  condition?: AssetCondition
  status?: AssetStatus
  movementType?: MovementType
  anomalyStatus?: AnomalyStatus
  dateFrom?: string
  dateTo?: string
}

export const REPORT_TYPE_LABEL: Record<ReportType, string> = {
  PAR_SITE: 'Répartition par site',
  PAR_CATEGORIE: 'Répartition par catégorie',
  PAR_SERVICE: 'Répartition par direction / service',
  PAR_UTILISATEUR: 'Répartition par utilisateur',
  PAR_ETAT: 'Répartition par état physique',
  NON_ETIQUETEES: 'Immobilisations non étiquetées',
  NON_INVENTORIEES: 'Immobilisations non inventoriées',
  ANOMALIES: "Anomalies d'inventaire",
  MOUVEMENTS: 'Mouvements',
  TRANSFERTS: 'Transferts inter-site',
  REFORMES: 'Immobilisations réformées',
}

/** Quels filtres de la barre commune sont pertinents pour chaque type - les autres restent visibles mais n'ont aucun effet (voir ReportService côté backend, ils sont silencieusement ignorés). */
export const REPORT_TYPE_RELEVANT_FILTERS: Record<ReportType, Array<keyof ReportFilterParams>> = {
  PAR_SITE: ['siteId', 'categoryId', 'condition', 'status'],
  PAR_CATEGORIE: ['siteId', 'categoryId', 'condition', 'status'],
  PAR_SERVICE: ['siteId', 'categoryId', 'condition', 'status'],
  PAR_UTILISATEUR: ['siteId', 'categoryId', 'condition', 'status'],
  PAR_ETAT: ['siteId', 'categoryId', 'status'],
  NON_ETIQUETEES: ['siteId', 'categoryId', 'condition', 'status'],
  NON_INVENTORIEES: ['siteId', 'categoryId', 'condition', 'status'],
  ANOMALIES: ['siteId', 'anomalyStatus', 'dateFrom', 'dateTo'],
  MOUVEMENTS: ['siteId', 'movementType', 'dateFrom', 'dateTo'],
  TRANSFERTS: ['siteId', 'dateFrom', 'dateTo'],
  REFORMES: ['siteId', 'categoryId'],
}
