-- Phase 2: etiquettes generees et pieces jointes. Les fichiers eux-memes
-- sont stockes hors PostgreSQL (systeme de fichiers/objet) - cette table
-- ne porte que les metadonnees (prompt maitre section 47).
--
-- `attachments` est polymorphe (owner_type/owner_id) car une piece jointe
-- peut se rattacher a une immobilisation, une anomalie ou un mouvement
-- (PV, facture, photo d'anomalie...). Pas de FK directe possible sur une
-- cible polymorphe : l'integrite reference est controlee par le service
-- layer, l'index (owner_type, owner_id) garde les lectures rapides.

CREATE TABLE asset_labels (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id      UUID NOT NULL REFERENCES assets(id) ON DELETE RESTRICT,
    format_id     UUID NOT NULL REFERENCES asset_label_formats(id) ON DELETE RESTRICT,
    generated_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    generated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_asset_labels_asset_id ON asset_labels(asset_id);

CREATE TABLE attachments (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_type    VARCHAR(30) NOT NULL CHECK (owner_type IN ('ASSET', 'ANOMALY', 'MOVEMENT')),
    owner_id      UUID NOT NULL,
    category      VARCHAR(30) NOT NULL DEFAULT 'AUTRE'
                  CHECK (category IN ('PHOTO', 'FACTURE', 'ACQUISITION', 'TRANSFERT', 'PV', 'REFORME', 'AUTRE')),
    file_name     VARCHAR(255) NOT NULL,
    storage_path  VARCHAR(500) NOT NULL,
    content_type  VARCHAR(100),
    size_bytes    BIGINT,
    uploaded_by   UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_attachments_owner ON attachments(owner_type, owner_id);
