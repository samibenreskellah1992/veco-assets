package dz.vecopharm.vecoassets.entity;

/**
 * Statut d'une session de scan de local (Checkpoint 3 de l'evolution
 * "locaux scannables", 2026-09). Workflow volontairement reduit par
 * rapport a {@link CampaignStatus} (pas de BROUILLON/EN_PREPARATION/
 * TERMINE/CLOTURE) : une session est ouverte sur un local pour un scan
 * immediat, puis validee - il n'y a pas de phase de preparation separee.
 */
public enum LocationSessionStatus {
    EN_COURS,
    VALIDEE
}
