-- Checkpoint 3 de l'evolution "locaux scannables" (2026-09) : inventaire
-- par scan de local (prompt maitre section 26, workflow demande par Sami,
-- confirme apres livraison des Checkpoints 1/2 + tests automatises). Une
-- "session de scan de local" est volontairement plus legere qu'une
-- campagne d'inventaire complete (InventoryCampaign, Phase 7) : ouverte et
-- cloturee sur UN seul local, sans dates de debut/fin ni responsable de
-- campagne. Les "campagnes par local" completes restent un checkpoint
-- ulterieur eventuel (voir claude/veco-assets-phase1-status.md).
--
-- Plutot que de dupliquer les ecrans/API existants (page Anomalies, onglet
-- "Inventaire" d'une immobilisation, InventoryAnomalyService/Mapper...),
-- les scans et anomalies issus d'une session de local traversent les MEMES
-- tables inventory_scans/inventory_anomalies (Phase 7, V7) que ceux d'une
-- campagne : campaign_id devient nullable sur les deux tables et une
-- nouvelle colonne nullable location_session_id est ajoutee en parallele,
-- avec une contrainte CHECK garantissant que l'une des deux references est
-- toujours renseignee (jamais les deux ni aucune). Purement additif :
-- aucune ligne existante n'est modifiee, campaign_id reste renseigne pour
-- tous les scans/anomalies actuels issus d'une campagne.

CREATE TABLE location_inventory_sessions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id    UUID NOT NULL REFERENCES locations(id) ON DELETE RESTRICT,
    opened_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    opened_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    status         VARCHAR(20) NOT NULL DEFAULT 'EN_COURS'
                   CHECK (status IN ('EN_COURS', 'VALIDEE')),
    validated_by   UUID REFERENCES users(id) ON DELETE SET NULL,
    validated_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_location_inventory_sessions_location_id ON location_inventory_sessions(location_id);
CREATE INDEX idx_location_inventory_sessions_status ON location_inventory_sessions(status);

-- Une seule session EN_COURS a la fois par local (index partiel unique) -
-- verifie aussi par LocationInventorySessionService avant creation pour un
-- message d'erreur clair, mais l'index reste la garantie ultime en base
-- contre une double ouverture concurrente, meme discipline que le compteur
-- atomique par site introduit en V15 pour LocationCodeGenerator.
CREATE UNIQUE INDEX uq_location_inventory_sessions_one_open_per_location
    ON location_inventory_sessions(location_id) WHERE status = 'EN_COURS';

ALTER TABLE inventory_scans
    ALTER COLUMN campaign_id DROP NOT NULL,
    ADD COLUMN location_session_id UUID REFERENCES location_inventory_sessions(id) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_inventory_scans_campaign_or_session
        CHECK (campaign_id IS NOT NULL OR location_session_id IS NOT NULL);
CREATE INDEX idx_inventory_scans_location_session_id ON inventory_scans(location_session_id);

ALTER TABLE inventory_anomalies
    ALTER COLUMN campaign_id DROP NOT NULL,
    ADD COLUMN location_session_id UUID REFERENCES location_inventory_sessions(id) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_inventory_anomalies_campaign_or_session
        CHECK (campaign_id IS NOT NULL OR location_session_id IS NOT NULL);
CREATE INDEX idx_inventory_anomalies_location_session_id ON inventory_anomalies(location_session_id);
