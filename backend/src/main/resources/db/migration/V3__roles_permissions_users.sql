-- Phase 2 / Phase 3 groundwork: RBAC (roles, permissions) and users.
-- Permissions are checked in the backend service/controller layer
-- (never in the frontend alone) - see docs/ARCHITECTURE.md section 7.

CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(50)  NOT NULL UNIQUE,
    label       VARCHAR(150) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(100) NOT NULL UNIQUE,
    label       VARCHAR(200) NOT NULL,
    module      VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_permissions_module ON permissions(module);

CREATE TABLE role_permissions (
    role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    matricule      VARCHAR(30)  UNIQUE,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    phone          VARCHAR(30),
    password_hash  VARCHAR(255) NOT NULL,
    site_id        UUID REFERENCES sites(id) ON DELETE SET NULL,
    department     VARCHAR(150),
    service        VARCHAR(150),
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                   CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    last_login_at  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_site_id ON users(site_id);
CREATE INDEX idx_users_status ON users(status);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Roles minimaux (prompt maitre section 27).
INSERT INTO roles (code, label, description) VALUES
    ('ADMIN', 'Administrateur', 'Acces complet a la plateforme'),
    ('GESTIONNAIRE_PATRIMOINE', 'Gestionnaire patrimoine', 'Gestion des immobilisations, inventaires et mouvements'),
    ('RESPONSABLE_SITE', 'Responsable de site', 'Acces aux biens de son site'),
    ('RESPONSABLE_SERVICE', 'Responsable de service', 'Gestion des biens de son service'),
    ('INVENTORISTE', 'Inventoriste', 'Scan et inventaire'),
    ('CONSULTATION', 'Consultation', 'Lecture seule');

-- Permissions minimales (prompt maitre section 28). D'autres sont
-- ajoutees phase par phase, au fur et a mesure que les modules existent.
INSERT INTO permissions (code, label, module) VALUES
    ('IMMOBILISATION_VIEW',    'Consulter les immobilisations',      'IMMOBILISATION'),
    ('IMMOBILISATION_CREATE',  'Creer une immobilisation',           'IMMOBILISATION'),
    ('IMMOBILISATION_EDIT',    'Modifier une immobilisation',        'IMMOBILISATION'),
    ('IMMOBILISATION_ARCHIVE', 'Archiver (suppression logique)',     'IMMOBILISATION'),
    ('INVENTAIRE_VIEW',        'Consulter les inventaires',          'INVENTAIRE'),
    ('INVENTAIRE_CREATE',      'Creer une campagne d''inventaire',   'INVENTAIRE'),
    ('INVENTAIRE_EXECUTE',     'Scanner / executer un inventaire',   'INVENTAIRE'),
    ('INVENTAIRE_VALIDATE',    'Valider une campagne d''inventaire', 'INVENTAIRE'),
    ('MOUVEMENT_CREATE',       'Creer un mouvement',                 'MOUVEMENT'),
    ('MOUVEMENT_VALIDATE',     'Valider un mouvement',               'MOUVEMENT'),
    ('REPORT_VIEW',            'Consulter les rapports',             'REPORT'),
    ('REPORT_EXPORT',          'Exporter les rapports',              'REPORT'),
    ('ADMIN_ACCESS',           'Acceder a l''administration',        'ADMIN');

-- ADMIN recoit toutes les permissions existantes par defaut.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'ADMIN';

-- GESTIONNAIRE_PATRIMOINE: gestion complete du patrimoine hors administration.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'GESTIONNAIRE_PATRIMOINE'
  AND p.code IN ('IMMOBILISATION_VIEW', 'IMMOBILISATION_CREATE', 'IMMOBILISATION_EDIT',
                 'IMMOBILISATION_ARCHIVE', 'INVENTAIRE_VIEW', 'INVENTAIRE_CREATE',
                 'INVENTAIRE_EXECUTE', 'INVENTAIRE_VALIDATE', 'MOUVEMENT_CREATE',
                 'MOUVEMENT_VALIDATE', 'REPORT_VIEW', 'REPORT_EXPORT');

-- RESPONSABLE_SITE / RESPONSABLE_SERVICE: consultation + mouvements sur leur perimetre
-- (le filtrage par site/service est applique par le service layer, pas ici).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('RESPONSABLE_SITE', 'RESPONSABLE_SERVICE')
  AND p.code IN ('IMMOBILISATION_VIEW', 'INVENTAIRE_VIEW', 'MOUVEMENT_CREATE', 'REPORT_VIEW');

-- INVENTORISTE: scan et inventaire uniquement.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'INVENTORISTE'
  AND p.code IN ('IMMOBILISATION_VIEW', 'INVENTAIRE_VIEW', 'INVENTAIRE_EXECUTE');

-- CONSULTATION: lecture seule.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'CONSULTATION'
  AND p.code IN ('IMMOBILISATION_VIEW', 'INVENTAIRE_VIEW', 'REPORT_VIEW');
