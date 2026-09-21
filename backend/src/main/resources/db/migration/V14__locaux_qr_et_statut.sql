-- Checkpoint 1 de l'evolution "locaux scannables" (Sami, 2026-09) : le
-- referentiel Location (V2) devient un point d'entree QR-scannable pour
-- l'inventaire (scan du local, puis comparaison attendu/scanne - phases
-- suivantes). Conformement a la decision "ne pas casser l'existant... ne
-- pas reecrire inutilement" (instruction #26), on enrichit l'entite
-- Location existante plutot que de creer une entite parallele.

ALTER TABLE locations
    ADD COLUMN status               VARCHAR(20)  NOT NULL DEFAULT 'ACTIF',
    ADD COLUMN qr_code               VARCHAR(50),
    ADD COLUMN description           TEXT,
    ADD COLUMN responsible_user_id   UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN last_inventory_at     TIMESTAMPTZ;

-- Backfill du code QR pour les locaux deja existants : deterministe, prefixe
-- par le code du site (traverse via zone -> floor -> building -> site), avec
-- un numero de sequence par site base sur l'anciennete de creation. On ne
-- peut pas ajouter directement une colonne NOT NULL + UNIQUE sur une table
-- deja peuplee : on peuple d'abord, puis on contraint.
WITH ranked AS (
    SELECT
        l.id,
        'LOC-' || si.code || '-' || lpad(
            (ROW_NUMBER() OVER (PARTITION BY si.code ORDER BY l.created_at))::text,
            6, '0'
        ) AS generated_code
    FROM locations l
    JOIN zones z ON z.id = l.zone_id
    JOIN floors f ON f.id = z.floor_id
    JOIN buildings b ON b.id = f.building_id
    JOIN sites si ON si.id = b.site_id
)
UPDATE locations l
SET qr_code = ranked.generated_code
FROM ranked
WHERE ranked.id = l.id;

ALTER TABLE locations ALTER COLUMN qr_code SET NOT NULL;
ALTER TABLE locations ADD CONSTRAINT uq_locations_qr_code UNIQUE (qr_code);

-- Sequence globale pour la generation du code QR des nouveaux locaux (le
-- code du site, injecte par LocationCodeGenerator, differencie deja les
-- locaux entre sites ; une collision de numero brut entre deux sites est
-- sans consequence car seule la chaine complete doit etre unique).
CREATE SEQUENCE location_code_seq START WITH 1 INCREMENT BY 1;

INSERT INTO settings (key, value, description) VALUES
    ('location_code.format', 'LOC-%s-%06d', 'Format du code local genere automatiquement (site + numero)');
