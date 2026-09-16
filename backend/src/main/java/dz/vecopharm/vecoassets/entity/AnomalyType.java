package dz.vecopharm.vecoassets.entity;

/** Type d'anomalie d'inventaire (prompt maitre section 16). */
public enum AnomalyType {
    INTROUVABLE,
    MAUVAISE_LOCALISATION,
    MAUVAIS_UTILISATEUR,
    NUMERO_SERIE_DIFFERENT,
    NON_REFERENCEE,
    DOUBLON,
    ETIQUETTE_DETERIOREE,
    ETIQUETTE_ABSENTE,
    HORS_SERVICE,
    AUTRE
}
