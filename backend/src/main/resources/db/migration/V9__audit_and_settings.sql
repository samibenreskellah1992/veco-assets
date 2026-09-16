-- Phase 2: audit trail generique (prompt maitre section 25) et parametres
-- applicatifs (section 53). `entity_id` n'a pas de FK : l'audit trail doit
-- rester lisible meme si l'objet audite est plus tard modifie/archive, et
-- il couvre des tables tres variees (assets, inventory_campaigns, users...).

CREATE TABLE audit_logs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID REFERENCES users(id) ON DELETE SET NULL,
    action       VARCHAR(50)  NOT NULL
                 CHECK (action IN ('CONNEXION', 'CREATION', 'MODIFICATION', 'SUPPRESSION_LOGIQUE',
                        'AFFECTATION', 'TRANSFERT', 'INVENTAIRE', 'VALIDATION', 'CHANGEMENT_STATUT',
                        'GENERATION_ETIQUETTE')),
    module       VARCHAR(50)  NOT NULL,
    entity_name  VARCHAR(100),
    entity_id    UUID,
    old_value    JSONB,
    new_value    JSONB,
    ip_address   VARCHAR(45),
    occurred_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_module ON audit_logs(module);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_name, entity_id);
CREATE INDEX idx_audit_logs_occurred_at ON audit_logs(occurred_at);

CREATE TABLE settings (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key          VARCHAR(100) NOT NULL UNIQUE,
    value        TEXT         NOT NULL,
    description  VARCHAR(255),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Parametres par defaut (prompt maitre section 53).
INSERT INTO settings (key, value, description) VALUES
    ('asset_code.prefix',       'VECO-IMM', 'Prefixe du code immobilisation genere automatiquement'),
    ('asset_code.start_number', '1',        'Numero de depart de la sequence de code immobilisation'),
    ('asset_code.format',       'VECO-IMM-%06d', 'Format d''affichage du code (prefixe + numero sur 6 chiffres)'),
    ('label.qr_enabled',        'true',     'Generation du QR Code activee'),
    ('label.barcode_enabled',   'true',     'Generation du code-barres activee');

-- Compteur transactionnel pour la generation du code immobilisation
-- (prompt maitre section 11 : jamais de reutilisation d'un code deja
-- attribue). Une sequence SQL classique suffit et est concurrency-safe ;
-- le prefixe/format restent pilotes depuis `settings` ci-dessus.
CREATE SEQUENCE asset_code_seq START WITH 1 INCREMENT BY 1;
