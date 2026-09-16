-- Phase 2: categories/sous-categories d'immobilisations (auto-reference pour
-- les sous-categories) et formats d'etiquette. Le prompt maitre section 13
-- exige explicitement que les dimensions d'etiquette soient administrables,
-- jamais codees en dur cote frontend - d'ou une table dediee plutot qu'une
-- simple cle/valeur, pour porter proprement largeur/hauteur/contenu.

CREATE TABLE asset_categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id   UUID REFERENCES asset_categories(id) ON DELETE RESTRICT,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_asset_categories_parent_id ON asset_categories(parent_id);

CREATE TABLE asset_label_formats (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                    VARCHAR(50)  NOT NULL UNIQUE,
    name                    VARCHAR(150) NOT NULL,
    width_mm                NUMERIC(6,2) NOT NULL,
    height_mm               NUMERIC(6,2) NOT NULL,
    show_logo               BOOLEAN      NOT NULL DEFAULT true,
    show_short_designation  BOOLEAN      NOT NULL DEFAULT true,
    show_qr_code             BOOLEAN      NOT NULL DEFAULT true,
    show_barcode            BOOLEAN      NOT NULL DEFAULT false,
    active                  BOOLEAN      NOT NULL DEFAULT true,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO asset_label_formats (code, name, width_mm, height_mm, show_logo, show_short_designation, show_qr_code, show_barcode) VALUES
    ('STANDARD_5X3',  'Format standard (5 x 3 cm)', 50.00, 30.00, true, true, true, false),
    ('COMPACT_4X2',   'Format compact (4 x 2 cm)',  40.00, 20.00, true, false, true, false),
    ('LARGE_7X4',     'Format large (7 x 4 cm)',    70.00, 40.00, true, true, true, true);
