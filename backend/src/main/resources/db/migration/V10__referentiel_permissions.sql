-- Phase 4: nouvelles permissions pour le module Referentiel (sites,
-- batiments, etages, zones, localisations, categories) et pour
-- l'administration des utilisateurs.
--
-- La LECTURE (GET) de ces ressources n'est pas gardee par une permission
-- dediee : elle est necessaire a tout utilisateur authentifie (menus
-- deroulants des futurs modules metier - creation d'immobilisation,
-- affectation, etc. - Phase 5+). Seules les operations d'ECRITURE
-- (creation / modification / desactivation) sont controlees par les
-- permissions ci-dessous, verifiees cote backend (@PreAuthorize), jamais
-- seulement cote frontend (prompt maitre section 28).
INSERT INTO permissions (code, label, module) VALUES
    ('REFERENTIEL_MANAGE', 'Gerer le referentiel (sites, localisations, categories)', 'REFERENTIEL'),
    ('USER_MANAGE',        'Gerer les utilisateurs et leurs roles',                   'ADMIN');

-- ADMIN recoit automatiquement toutes les permissions existantes au moment
-- de V3, mais cette regle n'est pas retroactive sur des permissions creees
-- plus tard : on la reapplique explicitement ici pour les deux nouvelles.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code IN ('REFERENTIEL_MANAGE', 'USER_MANAGE');

-- GESTIONNAIRE_PATRIMOINE gere deja les immobilisations/inventaires/mouvements
-- de bout en bout (V3) : le referentiel qui les sous-tend (sites, zones,
-- categories...) suit la meme logique. La gestion des comptes utilisateurs
-- reste strictement reservee a ADMIN.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'GESTIONNAIRE_PATRIMOINE' AND p.code = 'REFERENTIEL_MANAGE';
