-- Jeu de donnees de demonstration VECOPHARM (prompt maitre sections 42-43).
-- Applique UNIQUEMENT quand ce dossier est ajoute aux locations Flyway,
-- ce qui n'arrive que sur les profils dev/demo (voir application-dev.yml,
-- application-demo.yml) - jamais en prod. Numerote a partir de V900 pour
-- ne jamais entrer en collision avec les migrations de schema (V2..V899
-- reserve pour l'evolution du schema sur les phases suivantes).

-- --- Sites (prompt maitre section 42) ---------------------------------------------------
INSERT INTO sites (id, code, name, city, address) VALUES
    ('a0000000-0000-4000-8000-000000000001', 'VSA',  'VSA',      'Alger',   'Zone industrielle, VSA'),
    ('a0000000-0000-4000-8000-000000000002', 'ALG',  'Alger',    'Alger',   'Siege social, Alger'),
    ('a0000000-0000-4000-8000-000000000003', 'ORA',  'Oran',     'Oran',    'Site de distribution, Oran'),
    ('a0000000-0000-4000-8000-000000000004', 'BEJ',  'Bejaia',   'Bejaia',  'Site de distribution, Bejaia'),
    ('a0000000-0000-4000-8000-000000000005', 'LAG',  'Laghouat', 'Laghouat','Site de distribution, Laghouat');

-- --- Hierarchie de localisation : un chemin complet par site --------------------------------
INSERT INTO buildings (id, site_id, code, name) VALUES
    ('b0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001', 'ADM', 'Bloc administratif'),
    ('b0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000002', 'SIEGE', 'Batiment siege'),
    ('b0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000003', 'DEPOT', 'Batiment depot'),
    ('b0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000004', 'DEPOT', 'Batiment depot'),
    ('b0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000005', 'DEPOT', 'Batiment depot');

INSERT INTO floors (id, building_id, code, name) VALUES
    ('f0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001', 'E2', '2eme etage'),
    ('f0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000002', 'E1', '1er etage'),
    ('f0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000003', 'RDC', 'Rez-de-chaussee'),
    ('f0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000004', 'RDC', 'Rez-de-chaussee'),
    ('f0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000005', 'RDC', 'Rez-de-chaussee');

INSERT INTO zones (id, floor_id, code, name) VALUES
    ('c0000000-0000-4000-8000-000000000001', 'f0000000-0000-4000-8000-000000000001', 'BUR', 'Bureaux'),
    ('c0000000-0000-4000-8000-000000000002', 'f0000000-0000-4000-8000-000000000002', 'BUR', 'Bureaux'),
    ('c0000000-0000-4000-8000-000000000003', 'f0000000-0000-4000-8000-000000000003', 'STK', 'Stockage'),
    ('c0000000-0000-4000-8000-000000000004', 'f0000000-0000-4000-8000-000000000004', 'STK', 'Stockage'),
    ('c0000000-0000-4000-8000-000000000005', 'f0000000-0000-4000-8000-000000000005', 'STK', 'Stockage');

INSERT INTO locations (id, zone_id, code, name) VALUES
    ('d0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001', 'B204', 'Bureau 204'),
    ('d0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000002', 'B102', 'Bureau 102'),
    ('d0000000-0000-4000-8000-000000000003', 'c0000000-0000-4000-8000-000000000003', 'Z1',   'Zone stockage 1'),
    ('d0000000-0000-4000-8000-000000000004', 'c0000000-0000-4000-8000-000000000004', 'Z1',   'Zone stockage 1'),
    ('d0000000-0000-4000-8000-000000000005', 'c0000000-0000-4000-8000-000000000005', 'Z1',   'Zone stockage 1');

-- --- Categories d'immobilisations (prompt maitre section 42) ---------------------------------------------------
INSERT INTO asset_categories (id, code, name) VALUES
    ('e0000000-0000-4000-8000-000000000001', 'INFO', 'Informatique'),
    ('e0000000-0000-4000-8000-000000000002', 'MOB',  'Mobilier'),
    ('e0000000-0000-4000-8000-000000000003', 'VEH',  'Vehicules'),
    ('e0000000-0000-4000-8000-000000000004', 'TECH', 'Materiel technique'),
    ('e0000000-0000-4000-8000-000000000005', 'EQP',  'Equipements'),
    ('e0000000-0000-4000-8000-000000000006', 'BUR',  'Materiel de bureau'),
    ('e0000000-0000-4000-8000-000000000007', 'AUT',  'Autres');

-- --- Utilisateurs de demonstration --------------------------------------------------------
-- Mot de passe de demo pour tous les comptes ci-dessous : VecoDemo#2026
-- (hash BCrypt reel, verifiable une fois l'authentification cablee en
-- Phase 3 - aucun de ces comptes n'est exploitable avant cette phase).
INSERT INTO users (id, matricule, first_name, last_name, email, password_hash, site_id, department, service, status) VALUES
    ('11111111-0000-4000-8000-000000000001', 'VCP-0001', 'Sami',   'Benreskallah', 'sami.benreskallah@vecopharm.dz',
     '$2b$10$6lyDPcXv4Rn2irgjDW8VNe6BU6M12224UzHBlwhNpLDJI1x.zs2Nu',
     'a0000000-0000-4000-8000-000000000002', 'DSI', 'Systeme d''Information', 'ACTIVE'),
    ('11111111-0000-4000-8000-000000000002', 'VCP-0002', 'Ahmed',  'Benali', 'ahmed.benali@vecopharm.dz',
     '$2b$10$6lyDPcXv4Rn2irgjDW8VNe6BU6M12224UzHBlwhNpLDJI1x.zs2Nu',
     'a0000000-0000-4000-8000-000000000001', 'DSI', 'Infrastructure', 'ACTIVE'),
    ('11111111-0000-4000-8000-000000000003', 'VCP-0003', 'Sarah',  'Gacem', 'sarah.gacem@vecopharm.dz',
     '$2b$10$6lyDPcXv4Rn2irgjDW8VNe6BU6M12224UzHBlwhNpLDJI1x.zs2Nu',
     'a0000000-0000-4000-8000-000000000002', 'DFC', 'Comptabilite', 'ACTIVE'),
    ('11111111-0000-4000-8000-000000000004', 'VCP-0004', 'Karim',  'Bensalah', 'karim.bensalah@vecopharm.dz',
     '$2b$10$6lyDPcXv4Rn2irgjDW8VNe6BU6M12224UzHBlwhNpLDJI1x.zs2Nu',
     'a0000000-0000-4000-8000-000000000002', 'DG', 'Direction Generale', 'ACTIVE'),
    ('11111111-0000-4000-8000-000000000005', 'VCP-0005', 'Nabil',  'Kaci', 'nabil.kaci@vecopharm.dz',
     '$2b$10$6lyDPcXv4Rn2irgjDW8VNe6BU6M12224UzHBlwhNpLDJI1x.zs2Nu',
     'a0000000-0000-4000-8000-000000000003', 'Logistique', 'Transport', 'ACTIVE'),
    ('11111111-0000-4000-8000-000000000006', 'VCP-0006', 'Fatima', 'Zahra', 'fatima.zahra@vecopharm.dz',
     '$2b$10$6lyDPcXv4Rn2irgjDW8VNe6BU6M12224UzHBlwhNpLDJI1x.zs2Nu',
     'a0000000-0000-4000-8000-000000000005', 'RH', 'Ressources Humaines', 'ACTIVE'),
    ('11111111-0000-4000-8000-000000000007', 'VCP-0007', 'Mohamed','Reda', 'mohamed.reda@vecopharm.dz',
     '$2b$10$6lyDPcXv4Rn2irgjDW8VNe6BU6M12224UzHBlwhNpLDJI1x.zs2Nu',
     'a0000000-0000-4000-8000-000000000001', 'DSI', 'Support', 'ACTIVE');

INSERT INTO user_roles (user_id, role_id)
SELECT '11111111-0000-4000-8000-000000000001', id FROM roles WHERE code = 'ADMIN';
INSERT INTO user_roles (user_id, role_id)
SELECT '11111111-0000-4000-8000-000000000002', id FROM roles WHERE code = 'GESTIONNAIRE_PATRIMOINE';
INSERT INTO user_roles (user_id, role_id)
SELECT '11111111-0000-4000-8000-000000000003', id FROM roles WHERE code = 'CONSULTATION';
INSERT INTO user_roles (user_id, role_id)
SELECT '11111111-0000-4000-8000-000000000004', id FROM roles WHERE code = 'RESPONSABLE_SERVICE';
INSERT INTO user_roles (user_id, role_id)
SELECT '11111111-0000-4000-8000-000000000005', id FROM roles WHERE code = 'RESPONSABLE_SITE';
INSERT INTO user_roles (user_id, role_id)
SELECT '11111111-0000-4000-8000-000000000006', id FROM roles WHERE code = 'INVENTORISTE';
INSERT INTO user_roles (user_id, role_id)
SELECT '11111111-0000-4000-8000-000000000007', id FROM roles WHERE code = 'INVENTORISTE';

-- --- Immobilisations de demonstration (prompt maitre section 42) ---------------------------------------------------
INSERT INTO assets (
    asset_code, designation, category_id, brand, model, serial_number,
    site_id, building_id, floor_id, zone_id, location_id,
    direction, department, service, current_user_id, responsible_user_id,
    acquisition_date, supplier, invoice_number, acquisition_value, commissioning_date, warranty_until,
    condition, status, labeled
) VALUES
    ('VECO-IMM-000001', 'Ordinateur portable Dell Latitude 5540', 'e0000000-0000-4000-8000-000000000001',
     'Dell', 'Latitude 5540', 'DL5540FR20260147',
     'a0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001',
     'f0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000001',
     'DSI', 'Systeme d''Information', 'Infrastructure',
     '11111111-0000-4000-8000-000000000002', '11111111-0000-4000-8000-000000000007',
     '2025-03-15', 'Dell Algerie', 'FA-2025-0147', 185000.00, '2025-03-20', '2028-03-15',
     'BON', 'EN_SERVICE', true),

    ('VECO-IMM-000002', 'Ecran Dell 24 pouces', 'e0000000-0000-4000-8000-000000000001',
     'Dell', 'P2422H', 'DLSCR20260231',
     'a0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001',
     'f0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000001',
     'DSI', 'Systeme d''Information', 'Infrastructure',
     '11111111-0000-4000-8000-000000000002', '11111111-0000-4000-8000-000000000007',
     '2025-03-15', 'Dell Algerie', 'FA-2025-0147', 32000.00, '2025-03-20', '2027-03-15',
     'BON', 'EN_SERVICE', true),

    ('VECO-IMM-000003', 'Imprimante HP LaserJet Pro M404',  'e0000000-0000-4000-8000-000000000006',
     'HP', 'LaserJet Pro M404dn', 'HPPRT20250098',
     'a0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000002',
     'f0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000002',
     'DFC', 'Comptabilite', 'Comptabilite generale',
     '11111111-0000-4000-8000-000000000003', '11111111-0000-4000-8000-000000000003',
     '2024-11-05', 'Office Plus Algerie', 'FA-2024-0932', 68000.00, '2024-11-10', '2026-11-05',
     'BON', 'EN_SERVICE', true),

    ('VECO-IMM-000004', 'Bureau de direction', 'e0000000-0000-4000-8000-000000000002',
     'Kimel', 'Serie Executive', NULL,
     'a0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000002',
     'f0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000002',
     'DG', 'Direction Generale', 'Direction Generale',
     '11111111-0000-4000-8000-000000000004', '11111111-0000-4000-8000-000000000004',
     '2023-06-01', 'Ameublement Pro', 'FA-2023-0512', 95000.00, '2023-06-05', NULL,
     'BON', 'EN_SERVICE', false),

    ('VECO-IMM-000005', 'Chaise ergonomique', 'e0000000-0000-4000-8000-000000000002',
     'Kimel', 'ErgoPlus', NULL,
     'a0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000002',
     'f0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000002',
     'DG', 'Direction Generale', 'Direction Generale',
     '11111111-0000-4000-8000-000000000004', '11111111-0000-4000-8000-000000000004',
     '2023-06-01', 'Ameublement Pro', 'FA-2023-0512', 18000.00, '2023-06-05', NULL,
     'MOYEN', 'EN_SERVICE', false),

    ('VECO-IMM-000006', 'Vehicule utilitaire Renault Kangoo', 'e0000000-0000-4000-8000-000000000003',
     'Renault', 'Kangoo Express', 'VF1FW51N123456789',
     'a0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000003',
     'f0000000-0000-4000-8000-000000000003', 'c0000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000003',
     'Logistique', 'Transport', 'Transport',
     '11111111-0000-4000-8000-000000000005', '11111111-0000-4000-8000-000000000005',
     '2022-09-10', 'Renault Algerie', 'FA-2022-0771', 2450000.00, '2022-09-15', NULL,
     'BON', 'EN_SERVICE', true),

    ('VECO-IMM-000007', 'Onduleur APC 3000VA', 'e0000000-0000-4000-8000-000000000004',
     'APC', 'Smart-UPS 3000VA', 'APC3K20250017',
     'a0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000003',
     'f0000000-0000-4000-8000-000000000003', 'c0000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000003',
     'Logistique', 'Infrastructure', 'Infrastructure', NULL, '11111111-0000-4000-8000-000000000005',
     '2025-01-20', 'PowerTech Algerie', 'FA-2025-0044', 210000.00, '2025-01-25', '2027-01-20',
     'BON', 'EN_STOCK', false),

    ('VECO-IMM-000008', 'Groupe electrogene 20kVA', 'e0000000-0000-4000-8000-000000000005',
     'SDMO', 'Diesel 20kVA', 'SDMO20240412',
     'a0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000004',
     'f0000000-0000-4000-8000-000000000004', 'c0000000-0000-4000-8000-000000000004', 'd0000000-0000-4000-8000-000000000004',
     'Logistique', 'Infrastructure', 'Infrastructure', NULL, NULL,
     '2021-04-12', 'Energie Plus', 'FA-2021-0288', 980000.00, '2021-04-18', NULL,
     'BON', 'EN_MAINTENANCE', true),

    ('VECO-IMM-000009', 'Climatiseur split 18000 BTU', 'e0000000-0000-4000-8000-000000000005',
     'LG', 'Dual Cool 18000', 'LGAC20230065',
     'a0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000004',
     'f0000000-0000-4000-8000-000000000004', 'c0000000-0000-4000-8000-000000000004', 'd0000000-0000-4000-8000-000000000004',
     'Logistique', 'Infrastructure', 'Infrastructure', NULL, NULL,
     '2023-05-02', 'ClimaTech', 'FA-2023-0399', 75000.00, '2023-05-08', '2026-05-02',
     'MOYEN', 'EN_SERVICE', false),

    ('VECO-IMM-000010', 'Ordinateur de bureau HP EliteDesk', 'e0000000-0000-4000-8000-000000000001',
     'HP', 'EliteDesk 800 G9', 'HPED20240187',
     'a0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000005',
     'f0000000-0000-4000-8000-000000000005', 'c0000000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-000000000005',
     'RH', 'Ressources Humaines', 'Ressources Humaines',
     '11111111-0000-4000-8000-000000000006', '11111111-0000-4000-8000-000000000006',
     '2024-02-14', 'Office Plus Algerie', 'FA-2024-0156', 145000.00, '2024-02-20', '2026-02-14',
     'A_REPARER', 'EN_MAINTENANCE', true),

    ('VECO-IMM-000011', 'Armoire metallique', 'e0000000-0000-4000-8000-000000000002',
     'Kimel', 'Standard', NULL,
     'a0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000005',
     'f0000000-0000-4000-8000-000000000005', 'c0000000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-000000000005',
     'RH', 'Ressources Humaines', 'Ressources Humaines', NULL, NULL,
     '2022-08-19', 'Ameublement Pro', 'FA-2022-0644', 42000.00, '2022-08-22', NULL,
     'BON', 'EN_STOCK', false),

    ('VECO-IMM-000012', 'Photocopieur Ricoh MP 2014', 'e0000000-0000-4000-8000-000000000006',
     'Ricoh', 'MP 2014', 'RIC20190022',
     'a0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001',
     'f0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000001',
     'DSI', 'Systeme d''Information', 'Support', NULL, NULL,
     '2019-02-10', 'Office Plus Algerie', 'FA-2019-0071', 220000.00, '2019-02-15', '2022-02-10',
     'HORS_SERVICE', 'REFORME', true);

-- Fait avancer la sequence de generation de code au-dela des codes seedes
-- ci-dessus, pour que le prochain code genere par le backend (Phase 5)
-- soit bien VECO-IMM-000013 et non une collision.
SELECT setval('asset_code_seq', 12, true);

-- Affectations courantes correspondant aux immobilisations affectees ci-dessus.
INSERT INTO asset_assignments (asset_id, user_id, direction, department, service, assigned_from, assigned_by)
SELECT a.id, a.current_user_id, a.direction, a.department, a.service, a.commissioning_date::timestamptz, a.responsible_user_id
FROM assets a
WHERE a.current_user_id IS NOT NULL;
