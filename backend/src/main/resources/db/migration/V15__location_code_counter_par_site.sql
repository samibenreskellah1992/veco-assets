-- Checkpoint 1 (correctif) : la numerotation du backfill de V14 (par site,
-- ROW_NUMBER demarrant a 1 pour chaque site) et celle du generateur de code
-- QR pour les nouveaux locaux (sequence PostgreSQL UNIQUE et GLOBALE,
-- location_code_seq, elle aussi demarree a 1) etaient deux sources
-- independantes qui demarraient toutes deux a 1. Resultat : la creation du
-- tout premier local apres la migration reutilise le numero 1, qui entre en
-- collision avec le code deja backfille de n'importe quel site ayant deja
-- au moins un local (ex. doublon sur LOC-ALG-000001), et le probleme se
-- reproduit ensuite de facon non deterministe a chaque nouvelle creation.
--
-- Correctif : remplacer la sequence globale par un compteur PAR SITE
-- (coherent avec le format de code qui est deja site-scope,
-- LOC-{SITE}-{SEQUENCE}), initialise a la valeur la plus haute deja
-- utilisee par le backfill de V14 pour ce site.

CREATE TABLE location_code_counters (
    site_id    UUID PRIMARY KEY REFERENCES sites(id) ON DELETE CASCADE,
    last_value BIGINT NOT NULL DEFAULT 0
);

INSERT INTO location_code_counters (site_id, last_value)
SELECT si.id, COALESCE(MAX(substring(l.qr_code from '\d+$')::bigint), 0)
FROM sites si
JOIN buildings b ON b.site_id = si.id
JOIN floors f ON f.building_id = b.id
JOIN zones z ON z.floor_id = f.id
JOIN locations l ON l.zone_id = z.id
GROUP BY si.id;

DROP SEQUENCE IF EXISTS location_code_seq;
