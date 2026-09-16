package dz.vecopharm.vecoassets.dto;

/**
 * Les 11 types de rapport du module {@code /rapports} (prompt maitre Phase
 * 9). Contrairement aux autres enums de ce projet, celui-ci n'est PAS
 * persiste en base (aucune colonne ne le porte) : c'est un parametre d'API
 * pur, qui choisit quelles lignes {@link ReportService} calcule - d'ou sa
 * place dans {@code dto/} plutot que {@code entity/}.
 */
public enum ReportType {
    /** Repartition des immobilisations actives par site (nombre + valeur d'acquisition totale). */
    PAR_SITE,
    /** Repartition par categorie (feuille de la hierarchie categorie/sous-categorie). */
    PAR_CATEGORIE,
    /** Repartition par direction/departement/service (champs libres de l'immobilisation). */
    PAR_SERVICE,
    /** Repartition par utilisateur actuellement affecte. */
    PAR_UTILISATEUR,
    /** Repartition par etat physique (AssetCondition). */
    PAR_ETAT,
    /** Liste des immobilisations non etiquetees. */
    NON_ETIQUETEES,
    /** Liste des immobilisations jamais inventoriees (aucun scan dans leur perimetre - lastInventoryAt null). */
    NON_INVENTORIEES,
    /** Liste des anomalies d'inventaire, toutes campagnes confondues. */
    ANOMALIES,
    /** Liste de tous les mouvements. */
    MOUVEMENTS,
    /** Mouvements de type TRANSFERT_INTER_SITE uniquement - vue preremplie du rapport Mouvements. */
    TRANSFERTS,
    /** Immobilisations actuellement au statut REFORME, avec date et motif de reforme. */
    REFORMES
}
