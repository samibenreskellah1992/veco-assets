# VECO ASSETS — Roadmap V1

Développement par phases. Une phase n'est considérée terminée que si son critère de fin (section 11) est vérifié — on ne passe pas à la phase suivante si la phase courante ne fonctionne pas réellement (compilation, migrations, API, frontend, responsive).

## Phase 1 — Initialisation du projet *(terminée, backend non compilé faute d'accès Maven Central — voir section 13)*
- Structure `backend/` (Spring Boot, Maven, Java 21) et `frontend/` (Vite/React/TS).
- `docker-compose.yml` (postgres, backend, frontend), `.env.example`.
- Configuration de base (`application.yml`, Tailwind/shadcn, routing).
- Git initialisé, premier commit.
- README complet.
- Vérification : le backend démarre, le frontend démarre, `docker compose config` est valide.

## Phase 2 — Base de données *(terminée, migrations vérifiées à froid — voir section 13)*
- Migrations Flyway `V2`→`V9` pour l'ensemble des tables (sites/buildings/floors/zones/locations, roles/permissions/role_permissions/users/user_roles, asset_categories, asset_label_formats, assets, asset_assignments, asset_movements, asset_status_history, inventory_campaigns, inventory_scans, inventory_anomalies, asset_labels, attachments, audit_logs, settings) : PK/FK, index, contraintes uniques (dont partielles : numéro de série non dupliqué, affectation courante unique), contraintes `CHECK` pour les énumérations métier.
- 21 entités JPA correspondantes (`entity/`), relations mappées, deux classes de base (`BaseEntity`/`BaseCreatedEntity`) pour l'auditing `created_at`/`updated_at`, 21 repositories Spring Data (`repository/`), `AssetRepository` déjà `JpaSpecificationExecutor` pour les filtres combinables de la Phase 5.
- Seed de démonstration VECOPHARM (`db/seed/V900__seed_demo_data.sql`, profils `dev`/`demo` uniquement) : 5 sites avec hiérarchie de localisation complète, 7 catégories, 7 utilisateurs, 12 immobilisations réalistes.
- Vérification : migrations rejouées à froid sur PostgreSQL 16 vierge (V1→V9→seed), intégrité FK et contraintes métier (code/série uniques, énumérations, affectation courante unique) testées avec des insertions invalides qui échouent bien. Compilation Maven du backend **non vérifiable** dans l'environnement de développement utilisé (voir section 13) — à confirmer en priorité dans un environnement avec accès normal à Maven Central avant la Phase 3.

## Phase 3 — Authentification
- Login, JWT (génération/validation), utilisateurs, rôles, permissions.
- `GlobalExceptionHandler`, réponses d'erreur standardisées.
- Vérification : restrictions d'accès testées par rôle/permission (backend, pas seulement frontend).

## Phase 4 — Référentiel
- Sites, bâtiments, étages, zones, localisations, catégories, utilisateurs — CRUD complet, administrable.
- Vérification : aucune valeur de référentiel codée en dur côté frontend.

## Phase 5 — Immobilisations
- Liste, recherche globale, filtres avancés combinables, tri, pagination, export.
- Création/modification/consultation, fiche détaillée (onglets Résumé/Affectation/Inventaire/Mouvements/Historique/Documents/Photos).
- Génération automatique du code `VECO-IMM-000001` côté backend, contrainte unique.
- Historique par immobilisation.
- Vérification : parcours création → consultation → modification → historique fonctionnel de bout en bout.

## Phase 6 — Étiquetage
- Génération QR Code (identifie uniquement le code immobilisation) et code-barres.
- Module `/etiquetage` : sélection multiple, prévisualisation, génération PDF, formats configurables en administration (pas codés en dur).
- Vérification : étiquette PDF générée et téléchargeable pour une sélection réelle d'immobilisations.

## Phase 7 — Inventaire
- Campagnes d'inventaire (statuts Brouillon → En préparation → En cours → Terminé → Validé → Clôturé).
- Interface de scan mobile : scan QR, vérification d'appartenance à la campagne, confirmation de présence ou déclaration d'anomalie.
- Module anomalies (types, statuts, photo, rattachement à la campagne et à l'immobilisation).
- Une campagne clôturée n'accepte plus de scans (règle métier backend).
- Vérification : parcours scan → présence/anomalie → progression de campagne mis à jour en base réelle.

## Phase 8 — Mouvements
- Affectation, transfert inter-site, changement de localisation/service/utilisateur, maintenance, sortie, réforme.
- Workflow Demande → Validation → Exécution → Historisation, jamais de modification directe sans mouvement.
- Vérification : un transfert produit une entrée `asset_movements` et met à jour la fiche sans perdre l'historique.

## Phase 9 — Reporting
- Dashboard avec données réelles (totaux, étiquetées/non étiquetées, vérifiées, anomalies, en maintenance, réformées, graphiques par site/catégorie/état, activité récente).
- Module `/rapports` (par site, service, catégorie, utilisateur, état, non étiquetées, non inventoriées, anomalies, mouvements, transferts, réformes).
- Exports Excel/CSV/PDF respectant les filtres appliqués.
- Vérification : chaque chiffre du dashboard est vérifiable contre la base de données.

## Phase 10 — Qualité
- Tests backend (unitaires, services, controllers, repository critiques) et frontend (parcours critiques : création immobilisation, génération code, scan, inventaire, transfert, permissions).
- Revue sécurité, optimisation des requêtes (pagination/recherche backend, éviter N+1), responsive complet (desktop/tablette/mobile, scan optimisé mobile).
- Documentation finale (README, Swagger/OpenAPI à jour), audit trail vérifié sur toutes les actions critiques.
- Vérification : les 19 étapes du parcours cible (section objectif final du prompt maître) sont exécutables de bout en bout sur l'application réelle.

## 11. Critère de fin de phase (appliqué à chaque phase ci-dessus)
1. Code revu. 2. Compilation OK. 3. Tests lancés. 4. Erreurs corrigées. 5. Migrations vérifiées. 6. API vérifiée. 7. Frontend vérifié. 8. Responsive vérifié. 9. README à jour. 10. Résumé des éléments terminés fourni.

## 12. Hors périmètre V1 (préparé dans l'architecture, non développé)
LDAP/Active Directory, application mobile Flutter, mode offline, intégration Power BI, intégration ERP comptable, intégration VECO-GED, notifications email/internes, signature électronique, NFC, RFID.

## 13. Limite connue de l'environnement de développement utilisé pour les Phases 1-2
L'environnement cloud dans lequel les Phases 1 et 2 ont été développées n'a pas accès à `repo.maven.apache.org` (bloqué par sa politique réseau). Résultat : le backend Java/Spring Boot n'a **jamais été compilé par Maven** dans cet environnement — le code a été relu attentivement et les migrations SQL ont été vérifiées en conditions réelles contre un PostgreSQL 16 local (rejeu à froid, contraintes testées avec des insertions invalides), mais `mvn compile` / `mvn test` restent à exécuter dans un environnement avec accès réseau normal (poste de développement, CI Vecopharm) avant de considérer les critères de fin de phase 1 et 2 entièrement satisfaits (voir section 11, point 2 "Compilation OK"). Ceci n'affecte pas le frontend (build vérifié avec succès) ni la validité des migrations SQL elles-mêmes.
