-- Phase 6: nouvelles permissions pour le module Etiquetage.
--
-- Deux permissions distinctes, meme logique que V10 (Referentiel) :
-- generer des etiquettes pour des immobilisations existantes est une
-- operation courante de gestion de patrimoine (ADMIN + GESTIONNAIRE_PATRIMOINE,
-- comme IMMOBILISATION_CREATE/EDIT) tandis que definir les FORMATS
-- d'etiquette (dimensions, contenu affiche - prompt maitre section 13,
-- jamais code en dur) est une configuration d'administration reservee a
-- ADMIN, comme USER_MANAGE.
INSERT INTO permissions (code, label, module) VALUES
    ('ETIQUETTE_GENERATE', 'Generer des etiquettes pour des immobilisations', 'ETIQUETAGE'),
    ('ETIQUETTE_MANAGE',   'Gerer les formats d''etiquette',                  'ADMIN');

-- ADMIN recoit automatiquement toutes les permissions existantes au moment
-- de V3, mais cette regle n'est pas retroactive : on la reapplique ici
-- explicitement pour les deux nouvelles (meme remarque que V10/V11).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code IN ('ETIQUETTE_GENERATE', 'ETIQUETTE_MANAGE');

-- GESTIONNAIRE_PATRIMOINE gere deja la creation/modification des
-- immobilisations (V3) : generer leurs etiquettes suit la meme logique.
-- La definition des formats reste reservee a ADMIN.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'GESTIONNAIRE_PATRIMOINE' AND p.code = 'ETIQUETTE_GENERATE';
