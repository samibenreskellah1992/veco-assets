-- Phase 4: la valeur 'SUPPRESSION' (suppression physique) s'ajoute a
-- 'SUPPRESSION_LOGIQUE' (reservee aux immobilisations, prompt maitre
-- section 26) pour couvrir la suppression physique d'une donnee de
-- referentiel (site/batiment/etage/zone/localisation/categorie) jamais
-- utilisee - le seul cas ou ce depot autorise un DELETE physique en base,
-- et seulement quand aucun enfant ni aucune reference (utilisateur,
-- immobilisation) n'existe (verifie par le service layer, Phase 4).
ALTER TABLE audit_logs DROP CONSTRAINT audit_logs_action_check;
ALTER TABLE audit_logs ADD CONSTRAINT audit_logs_action_check
    CHECK (action IN ('CONNEXION', 'CREATION', 'MODIFICATION', 'SUPPRESSION_LOGIQUE', 'SUPPRESSION',
           'AFFECTATION', 'TRANSFERT', 'INVENTAIRE', 'VALIDATION', 'CHANGEMENT_STATUT',
           'GENERATION_ETIQUETTE'));
