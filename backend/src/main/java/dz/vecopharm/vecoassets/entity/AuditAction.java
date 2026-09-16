package dz.vecopharm.vecoassets.entity;

/** Type d'action enregistree dans l'audit trail (prompt maitre section 25). */
public enum AuditAction {
    CONNEXION,
    CREATION,
    MODIFICATION,
    SUPPRESSION_LOGIQUE,
    AFFECTATION,
    TRANSFERT,
    INVENTAIRE,
    VALIDATION,
    CHANGEMENT_STATUT,
    GENERATION_ETIQUETTE
}
