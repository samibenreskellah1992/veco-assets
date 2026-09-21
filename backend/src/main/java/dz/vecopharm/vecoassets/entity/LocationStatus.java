package dz.vecopharm.vecoassets.entity;

/**
 * Statut operationnel d'un local (Checkpoint 1 de l'evolution "locaux
 * scannables", 2026-09). Distinct du booleen {@code active} deja porte par
 * {@link Location} (qui reste le seul champ utilise par
 * activate/deactivate du CRUD referentiel existant) : {@code status}
 * qualifie l'etat operationnel du local lui-meme (en travaux, ferme, a
 * inventorier...) pour les futurs ecrans de scan/inventaire par local.
 */
public enum LocationStatus {
    ACTIF,
    INACTIF,
    EN_TRAVAUX,
    FERME,
    A_INVENTORIER
}
