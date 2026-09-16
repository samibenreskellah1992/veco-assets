-- Phase 2: table centrale des immobilisations. Le code metier
-- (VECO-IMM-000001) est genere par le backend (service layer), jamais par
-- une sequence appelee depuis le frontend - cette table ne fait que porter
-- la contrainte d'unicite qui le garantit en base.
--
-- Suppression logique uniquement (colonne `deleted`) : voir prompt maitre
-- section 26 et docs/ARCHITECTURE.md section 5. Rien ne doit jamais faire
-- un DELETE physique sur cette table depuis le code applicatif.

CREATE TABLE assets (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Identification
    asset_code           VARCHAR(30)  NOT NULL UNIQUE,

    -- Designation
    designation          VARCHAR(255) NOT NULL,
    category_id          UUID NOT NULL REFERENCES asset_categories(id) ON DELETE RESTRICT,
    brand                VARCHAR(100),
    model                VARCHAR(100),
    serial_number        VARCHAR(150),

    -- Localisation courante
    site_id              UUID NOT NULL REFERENCES sites(id) ON DELETE RESTRICT,
    building_id          UUID REFERENCES buildings(id) ON DELETE SET NULL,
    floor_id             UUID REFERENCES floors(id) ON DELETE SET NULL,
    zone_id              UUID REFERENCES zones(id) ON DELETE SET NULL,
    location_id          UUID REFERENCES locations(id) ON DELETE SET NULL,

    -- Affectation courante
    direction            VARCHAR(150),
    department           VARCHAR(150),
    service              VARCHAR(150),
    current_user_id      UUID REFERENCES users(id) ON DELETE SET NULL,
    responsible_user_id  UUID REFERENCES users(id) ON DELETE SET NULL,

    -- Acquisition
    acquisition_date     DATE,
    supplier             VARCHAR(150),
    invoice_number       VARCHAR(100),
    acquisition_value    NUMERIC(14,2),
    commissioning_date   DATE,
    warranty_until       DATE,

    -- Etat et statut (prompt maitre section 10)
    condition            VARCHAR(20) NOT NULL DEFAULT 'BON'
                          CHECK (condition IN ('NEUF', 'BON', 'MOYEN', 'A_REPARER', 'HORS_SERVICE', 'REFORME')),
    status                VARCHAR(20) NOT NULL DEFAULT 'EN_STOCK'
                          CHECK (status IN ('EN_STOCK', 'EN_SERVICE', 'EN_MAINTENANCE', 'TRANSFERE', 'REFORME', 'SORTI')),

    -- Complementaire
    comment              TEXT,
    labeled              BOOLEAN     NOT NULL DEFAULT false,
    last_inventory_at    TIMESTAMPTZ,

    -- Suppression logique
    deleted              BOOLEAN     NOT NULL DEFAULT false,
    deleted_at           TIMESTAMPTZ,

    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Un numero de serie ne doit pas etre duplique lorsqu'il est renseigne
-- (prompt maitre section 38) - index unique partiel, NULL autorise en doublon.
CREATE UNIQUE INDEX uq_assets_serial_number ON assets(serial_number) WHERE serial_number IS NOT NULL;

CREATE INDEX idx_assets_site_id ON assets(site_id);
CREATE INDEX idx_assets_category_id ON assets(category_id);
CREATE INDEX idx_assets_status ON assets(status);
CREATE INDEX idx_assets_condition ON assets(condition);
CREATE INDEX idx_assets_current_user_id ON assets(current_user_id);
CREATE INDEX idx_assets_deleted ON assets(deleted);
CREATE INDEX idx_assets_labeled ON assets(labeled);
