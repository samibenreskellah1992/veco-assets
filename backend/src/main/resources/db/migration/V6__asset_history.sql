-- Phase 2: tables de tracabilite pour les immobilisations. Ce sont ces
-- tables qui repondent a qui/quoi/quand/ou/pourquoi/ancienne-nouvelle valeur
-- (prompt maitre section 3). Le service layer ecrit ici AVANT ou EN MEME
-- TEMPS que la mise a jour de l'etat courant dans `assets` - jamais l'un
-- sans l'autre (workflow Demande -> Validation -> Execution -> Historisation,
-- prompt maitre section 18).

CREATE TABLE asset_assignments (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id       UUID NOT NULL REFERENCES assets(id) ON DELETE RESTRICT,
    user_id        UUID REFERENCES users(id) ON DELETE SET NULL,
    direction      VARCHAR(150),
    department     VARCHAR(150),
    service        VARCHAR(150),
    assigned_from  TIMESTAMPTZ NOT NULL DEFAULT now(),
    assigned_until TIMESTAMPTZ,
    assigned_by    UUID REFERENCES users(id) ON DELETE SET NULL,
    comment        TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_asset_assignments_asset_id ON asset_assignments(asset_id);
CREATE INDEX idx_asset_assignments_user_id ON asset_assignments(user_id);
-- Au plus une affectation courante (assigned_until NULL) par immobilisation.
CREATE UNIQUE INDEX uq_asset_assignments_current ON asset_assignments(asset_id) WHERE assigned_until IS NULL;

CREATE TABLE asset_movements (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id          UUID NOT NULL REFERENCES assets(id) ON DELETE RESTRICT,
    movement_type     VARCHAR(30) NOT NULL
                      CHECK (movement_type IN ('AFFECTATION', 'CHANGEMENT_UTILISATEUR', 'CHANGEMENT_SERVICE',
                             'CHANGEMENT_LOCALISATION', 'TRANSFERT_INTER_SITE', 'RETOUR', 'MAINTENANCE',
                             'SORTIE', 'REFORME')),
    from_site_id      UUID REFERENCES sites(id) ON DELETE SET NULL,
    to_site_id        UUID REFERENCES sites(id) ON DELETE SET NULL,
    from_location_id  UUID REFERENCES locations(id) ON DELETE SET NULL,
    to_location_id    UUID REFERENCES locations(id) ON DELETE SET NULL,
    from_user_id      UUID REFERENCES users(id) ON DELETE SET NULL,
    to_user_id        UUID REFERENCES users(id) ON DELETE SET NULL,
    requested_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    validated_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'DEMANDE'
                      CHECK (status IN ('DEMANDE', 'VALIDE', 'EXECUTE', 'REJETE')),
    reason            TEXT,
    comment           TEXT,
    requested_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    validated_at      TIMESTAMPTZ,
    executed_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_asset_movements_asset_id ON asset_movements(asset_id);
CREATE INDEX idx_asset_movements_status ON asset_movements(status);
CREATE INDEX idx_asset_movements_type ON asset_movements(movement_type);
CREATE INDEX idx_asset_movements_requested_at ON asset_movements(requested_at);

CREATE TABLE asset_status_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id    UUID NOT NULL REFERENCES assets(id) ON DELETE RESTRICT,
    field_name  VARCHAR(20) NOT NULL CHECK (field_name IN ('CONDITION', 'STATUS')),
    old_value   VARCHAR(50),
    new_value   VARCHAR(50) NOT NULL,
    changed_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    changed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    comment     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_asset_status_history_asset_id ON asset_status_history(asset_id);
CREATE INDEX idx_asset_status_history_changed_at ON asset_status_history(changed_at);
