-- Phase 2: campagnes d'inventaire, scans et anomalies (prompt maitre
-- sections 14-16). Une campagne CLOTURE n'accepte plus de scans - cette
-- regle est controlee par le service layer, pas par une contrainte SQL,
-- car elle depend du statut au moment de l'ecriture.

CREATE TABLE inventory_campaigns (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(150) NOT NULL,
    site_id              UUID NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    zone_id              UUID REFERENCES zones(id) ON DELETE SET NULL,
    responsible_user_id  UUID REFERENCES users(id) ON DELETE SET NULL,
    start_date           DATE NOT NULL,
    end_date             DATE NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'BROUILLON'
                         CHECK (status IN ('BROUILLON', 'EN_PREPARATION', 'EN_COURS', 'TERMINE', 'VALIDE', 'CLOTURE')),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (end_date >= start_date)
);
CREATE INDEX idx_inventory_campaigns_site_id ON inventory_campaigns(site_id);
CREATE INDEX idx_inventory_campaigns_status ON inventory_campaigns(status);

CREATE TABLE inventory_scans (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id  UUID NOT NULL REFERENCES inventory_campaigns(id) ON DELETE RESTRICT,
    asset_id     UUID NOT NULL REFERENCES assets(id) ON DELETE RESTRICT,
    scanned_by   UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    scanned_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    result       VARCHAR(20) NOT NULL CHECK (result IN ('PRESENT', 'ANOMALIE')),
    comment      TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_inventory_scans_campaign_id ON inventory_scans(campaign_id);
CREATE INDEX idx_inventory_scans_asset_id ON inventory_scans(asset_id);
CREATE INDEX idx_inventory_scans_campaign_asset ON inventory_scans(campaign_id, asset_id);

CREATE TABLE inventory_anomalies (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id  UUID NOT NULL REFERENCES inventory_campaigns(id) ON DELETE RESTRICT,
    asset_id     UUID REFERENCES assets(id) ON DELETE SET NULL,
    scan_id      UUID REFERENCES inventory_scans(id) ON DELETE SET NULL,
    anomaly_type VARCHAR(40) NOT NULL
                 CHECK (anomaly_type IN ('INTROUVABLE', 'MAUVAISE_LOCALISATION', 'MAUVAIS_UTILISATEUR',
                        'NUMERO_SERIE_DIFFERENT', 'NON_REFERENCEE', 'DOUBLON', 'ETIQUETTE_DETERIOREE',
                        'ETIQUETTE_ABSENTE', 'HORS_SERVICE', 'AUTRE')),
    description  TEXT,
    reported_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'NOUVELLE'
                 CHECK (status IN ('NOUVELLE', 'EN_COURS', 'RESOLUE', 'REJETEE')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_inventory_anomalies_campaign_id ON inventory_anomalies(campaign_id);
CREATE INDEX idx_inventory_anomalies_asset_id ON inventory_anomalies(asset_id);
CREATE INDEX idx_inventory_anomalies_status ON inventory_anomalies(status);
