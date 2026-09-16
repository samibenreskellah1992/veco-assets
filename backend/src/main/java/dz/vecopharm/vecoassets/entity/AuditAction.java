package dz.vecopharm.vecoassets.entity;

/** Type d'action enregistree dans l'audit trail (prompt maitre section 25). */
public enum AuditAction {
    CONNEXION,
    CREATION,
    MODIFICATION,
    SUPPRESSION_LOGIQUE,
    /** Suppression physique d'une donnee de referentiel jamais utilisee (aucun enfant/aucune reference) - voir Phase 4, distinct de {@link #SUPPRESSION_LOGIQUE} qui ne s'applique qu'aux immobilisations (prompt maitre section 26). */
    SUPPRESSION,
    AFFECTATION,
    TRANSFERT,
    INVENTAIRE,
    VALIDATION,
    CHANGEMENT_STATUT,
    GENERATION_ETIQUETTE
}
