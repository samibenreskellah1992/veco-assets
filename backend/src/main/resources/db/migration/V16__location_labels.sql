-- Checkpoint 2 de l'evolution "locaux scannables" (2026-09) : generation
-- d'etiquettes (QR + texte) pour les locaux, meme mecanisme que
-- l'etiquetage d'immobilisation (Phase 6). Le format d'etiquette
-- (asset_label_formats - dimensions, logo, designation courte, QR,
-- code-barres) est reutilise TEL QUEL pour les deux : son contenu est deja
-- generique (aucun champ propre aux immobilisations), et dupliquer la
-- table + l'ecran d'administration "Formats d'etiquette" pour les seuls
-- locaux n'apporterait rien (conforme a l'instruction #26 de Sami : ne
-- pas dupliquer ce qui peut etre reutilise tel quel).

ALTER TABLE locations
    ADD COLUMN labeled BOOLEAN NOT NULL DEFAULT false;

-- Meme structure que asset_labels (V8) : une ligne par generation
-- d'etiquette, jamais ecrasee (historique/audit).
CREATE TABLE location_labels (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id   UUID NOT NULL REFERENCES locations(id) ON DELETE RESTRICT,
    format_id     UUID NOT NULL REFERENCES asset_label_formats(id) ON DELETE RESTRICT,
    generated_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    generated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_location_labels_location_id ON location_labels(location_id);
