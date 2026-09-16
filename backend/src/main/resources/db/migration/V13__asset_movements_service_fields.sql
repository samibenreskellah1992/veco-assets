-- Phase 8: colonnes complementaires pour tracer un changement de
-- direction/departement/service (prompt maitre section 17 - "changement de
-- localisation/service/utilisateur"), symetriques aux colonnes
-- site/localisation/utilisateur deja presentes depuis la Phase 2 (V6).
-- Asset.direction/department/service sont des champs texte libres (aucune
-- table de reference dediee, contrairement a site/localisation), d'ou des
-- colonnes texte ici plutot qu'une cle etrangere - meme choix que sur
-- `assets` lui-meme.
ALTER TABLE asset_movements
    ADD COLUMN from_direction  VARCHAR(150),
    ADD COLUMN to_direction    VARCHAR(150),
    ADD COLUMN from_department VARCHAR(150),
    ADD COLUMN to_department   VARCHAR(150),
    ADD COLUMN from_service    VARCHAR(150),
    ADD COLUMN to_service      VARCHAR(150);
