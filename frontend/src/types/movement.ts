/** Mirrors the backend DTOs 1:1 (dz.vecopharm.vecoassets.dto) - see docs/ARCHITECTURE.md, Phase 8 (Mouvements). */

export type MovementType =
  | 'AFFECTATION'
  | 'CHANGEMENT_UTILISATEUR'
  | 'CHANGEMENT_SERVICE'
  | 'CHANGEMENT_LOCALISATION'
  | 'TRANSFERT_INTER_SITE'
  | 'RETOUR'
  | 'MAINTENANCE'
  | 'SORTIE'
  | 'REFORME'

export type MovementStatus = 'DEMANDE' | 'VALIDE' | 'EXECUTE' | 'REJETE'

/**
 * Pre-remplissage du formulaire "Nouveau mouvement" (page Mouvements) au
 * depart d'un autre ecran - Checkpoint 3 "locaux scannables" (2026-09) :
 * une anomalie MAUVAISE_LOCALISATION detectee au scan d'un local propose
 * ce mouvement (CHANGEMENT_LOCALISATION si meme site, TRANSFERT_INTER_SITE
 * sinon), sans jamais le creer automatiquement - passe en navigation
 * state React Router (voir LocationInventorySessionDetailPage).
 */
export interface MovementPrefill {
  assetId: string
  movementType: MovementType
  toSiteId?: string
  toBuildingId?: string
  toFloorId?: string
  toZoneId?: string
  toLocationId?: string
  reason?: string
}

export interface MovementDto {
  id: string
  assetId: string
  assetCode: string
  assetDesignation: string
  movementType: MovementType
  fromSiteId: string | null
  fromSiteName: string | null
  toSiteId: string | null
  toSiteName: string | null
  fromLocationId: string | null
  fromLocationName: string | null
  toLocationId: string | null
  toLocationName: string | null
  fromUserId: string | null
  fromUserName: string | null
  toUserId: string | null
  toUserName: string | null
  fromDirection: string | null
  toDirection: string | null
  fromDepartment: string | null
  toDepartment: string | null
  fromService: string | null
  toService: string | null
  requestedById: string | null
  requestedByName: string | null
  validatedById: string | null
  validatedByName: string | null
  status: MovementStatus
  reason: string | null
  comment: string | null
  requestedAt: string
  validatedAt: string | null
  executedAt: string | null
}

export interface MovementCreateRequest {
  assetId: string
  movementType: MovementType
  toSiteId?: string
  toLocationId?: string
  toUserId?: string
  toDirection?: string
  toDepartment?: string
  toService?: string
  reason?: string
  comment?: string
}

export interface MovementRejectRequest {
  comment?: string
}

export const MOVEMENT_TYPE_LABEL: Record<MovementType, string> = {
  AFFECTATION: 'Affectation',
  CHANGEMENT_UTILISATEUR: "Changement d'utilisateur",
  CHANGEMENT_SERVICE: 'Changement de service',
  CHANGEMENT_LOCALISATION: 'Changement de localisation',
  TRANSFERT_INTER_SITE: 'Transfert inter-site',
  RETOUR: 'Retour au stock',
  MAINTENANCE: 'Maintenance',
  SORTIE: 'Sortie',
  REFORME: 'Réforme',
}

export const MOVEMENT_STATUS_LABEL: Record<MovementStatus, string> = {
  DEMANDE: 'Demandé',
  VALIDE: 'Validé',
  EXECUTE: 'Exécuté',
  REJETE: 'Rejeté',
}

/** Types de mouvement necessitant un utilisateur destinataire (voir MovementService#applyTargetFields côté backend). */
export const MOVEMENT_TYPES_REQUIRING_USER: MovementType[] = ['AFFECTATION', 'CHANGEMENT_UTILISATEUR']

/** Types de mouvement necessitant direction/departement/service (au moins un). */
export const MOVEMENT_TYPES_REQUIRING_SERVICE: MovementType[] = ['CHANGEMENT_SERVICE']

/** Changement de local au sein du meme site (local cible obligatoire). */
export const MOVEMENT_TYPES_REQUIRING_LOCATION: MovementType[] = ['CHANGEMENT_LOCALISATION']

/** Changement de site (site cible obligatoire, local cible optionnel). */
export const MOVEMENT_TYPES_REQUIRING_SITE: MovementType[] = ['TRANSFERT_INTER_SITE']
