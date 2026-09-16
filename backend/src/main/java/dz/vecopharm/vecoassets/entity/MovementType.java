package dz.vecopharm.vecoassets.entity;

/** Type de mouvement d'une immobilisation (prompt maitre section 17). */
public enum MovementType {
    AFFECTATION,
    CHANGEMENT_UTILISATEUR,
    CHANGEMENT_SERVICE,
    CHANGEMENT_LOCALISATION,
    TRANSFERT_INTER_SITE,
    RETOUR,
    MAINTENANCE,
    SORTIE,
    REFORME
}
