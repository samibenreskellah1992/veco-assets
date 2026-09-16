# VECO ASSETS

Plateforme de gestion, d'étiquetage, d'inventaire et de traçabilité des immobilisations de **VECOPHARM**.

> Processus cible : RECENSEMENT → VÉRIFICATION → CODIFICATION → ÉTIQUETAGE → AFFECTATION → INVENTAIRE → CONTRÔLE → MISE À JOUR DU RÉFÉRENTIEL

Ce dépôt en est à la **Phase 10 — Qualité** (voir `docs/ROADMAP.md`), la dernière phase du prompt maître V1. Le backend et le frontend démarrent, la base de données Flyway est câblée (Phase 2), l'authentification JWT et le contrôle d'accès par rôle/permission sont en place (Phase 3, avec une vraie page de connexion côté frontend), l'administration du référentiel (sites, bâtiments, étages, zones, localisations, catégories, formats d'étiquette, utilisateurs) est utilisable de bout en bout (Phase 4), le registre des immobilisations (création, modification, archivage, recherche/filtres/tri/pagination, fiche détaillée avec historique d'état/statut et d'affectation) l'est également (Phase 5), la génération d'étiquettes (sélection d'immobilisations, PDF réel avec QR code — et code-barres selon le format — aux dimensions administrables du format choisi) l'est aussi (Phase 6), les campagnes d'inventaire (workflow de statut, scan d'immobilisations avec vérification d'appartenance à la campagne, déclaration d'anomalies avec photo) le sont également (Phase 7), les mouvements d'immobilisation (affectation, transfert inter-site, changement de localisation/service/utilisateur, maintenance, sortie, réforme — workflow Demande → Validation → Exécution → Historisation) le sont aussi (Phase 8), le tableau de bord (indicateurs réels, répartitions par site/catégorie/état, activité récente) ainsi que le module `/rapports` (11 types de rapport, exports CSV/Excel/PDF respectant les filtres appliqués) le sont aussi (Phase 9), et la Phase 10 ajoute des tests (backend : 38 unitaires + 12 d'intégration, écrits mais non exécutés ici ; frontend : 29 tests Vitest, **réellement exécutés avec succès**), une revue de sécurité (`docs/SECURITY.md`), l'élimination des N+1 les plus significatifs (`@EntityGraph`), un correctif d'audit trail (scan hors périmètre) et une navigation mobile enfin responsive (tiroir hors-champ sous 1024px). Voir `docs/ROADMAP.md` section 13 pour les limites de vérification propres à l'environnement de développement utilisé jusqu'ici (Maven Central et Docker inaccessibles ; les migrations et la logique métier ont pu être vérifiées en SQL réel — dont des bugs réels trouvés et corrigés en Phase 7 (progression de campagne) —, le frontend a pu être compilé, lint-vérifié et, depuis la Phase 10, testé pour de vrai, et les nouvelles dépendances de génération de document — ZXing/PDFBox en Phase 6, Apache POI en Phase 9 — n'ont pu être vérifiées que par revue manuelle de leur API, pas par compilation réelle).

## 1. Présentation

- **Frontend** : React 18/19, TypeScript, Vite, Tailwind CSS v4, shadcn/ui, React Router, TanStack Query, React Hook Form + Zod, Lucide Icons.
- **Backend** : Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA/Hibernate, Bean Validation, Flyway, springdoc-openapi, ZXing (QR code / code-barres Code128) et Apache PDFBox (génération de PDF d'étiquettes, Phase 6).
- **Base de données** : PostgreSQL 16+.
- **Authentification (V1)** : locale (JWT), rôles/permissions contrôlés côté backend. LDAP/Active Directory est préparé architecturalement mais **non implémenté** en V1.

Voir `docs/ARCHITECTURE.md` pour l'architecture détaillée (couches, modèle de données, flux métier, sécurité, déploiement) et `docs/ROADMAP.md` pour le détail des 10 phases.

## 2. Architecture

```
Frontend (React/Vite) ──REST/JSON──▶ Backend (Spring Boot) ──JDBC──▶ PostgreSQL
```

```
backend/src/main/java/dz/vecopharm/vecoassets/
  config/         audit/
  controller/     specification/
  dto/            security/
  entity/         exception/
  mapper/
  repository/
  service/

frontend/src/
  components/  layouts/  pages/  features/
  hooks/       services/ types/  utils/
  routes/      lib/
```

`components/ui/` porte les primitives shadcn/ui (bouton, champ, select, dialogue, onglets, ...) ajoutées à la main (voir `docs/ROADMAP.md` section 13). `features/referentiel/` porte les écrans Sites/Bâtiments/Étages/Zones/Localisations/Catégories/Formats d'étiquette (Phase 4 et Phase 6 pour `LabelFormatsPanel.tsx`), `hooks/use-auth.tsx` porte la session (JWT, restauration au démarrage, déconnexion). `pages/AssetsPage.tsx` (liste/filtres/pagination), `pages/AssetFormPage.tsx` (création/modification, localisation en cascade) et `pages/AssetDetailPage.tsx` (fiche à onglets, dont l'onglet "Inventaire" branché depuis la Phase 7 sur l'historique réel de scan du bien et l'onglet "Mouvements" branché depuis la Phase 8 sur l'historique réel des mouvements) portent le module Immobilisations (Phase 5). `pages/EtiquetagePage.tsx` (sélection multi-immobilisations, filtres, choix du format, génération et aperçu/téléchargement du PDF) porte le module Étiquetage (Phase 6). `pages/InventoryCampaignsPage.tsx` (liste des campagnes, création avec localisation en cascade), `pages/InventoryCampaignDetailPage.tsx` (progression, transitions de statut, scan, historique des scans, immobilisations restantes) et `pages/AnomaliesPage.tsx` (liste globale des anomalies, filtre par statut, photos) portent le module Inventaire (Phase 7). `pages/MouvementsPage.tsx` (liste filtrable par statut/type, demande de mouvement avec sélection d'immobilisation par recherche et champs conditionnels selon le type, validation/rejet/exécution) porte le module Mouvements (Phase 8). `pages/DashboardPage.tsx` (10 indicateurs chiffrés, 3 graphiques en barres horizontales faits main — aucune bibliothèque de graphiques ajoutée, activité récente) et `pages/ReportsPage.tsx` (sélecteur parmi 11 types de rapport, barre de filtres n'affichant que les champs pertinents pour le type choisi, tableau à colonnes dynamiques, exports CSV/Excel/PDF) portent le module Reporting (Phase 9).

## 3. Prérequis

- Java 21 (`java -version`)
- Maven 3.9+ (`mvn -version`) — ou le wrapper `./mvnw` si ajouté ultérieurement
- Node.js 22+ et npm 10+ (`node -v`, `npm -v`)
- Docker + Docker Compose v2 (`docker compose version`)
- PostgreSQL 16 client (`psql --version`) si vous voulez inspecter la base hors Docker

## 4. Installation

```bash
git clone <url-du-depot> veco-assets
cd veco-assets
cp .env.example .env
# Éditez .env : définissez au minimum POSTGRES_PASSWORD et JWT_SECRET
# (openssl rand -base64 48 pour générer un secret JWT)
```

## 5. Lancement avec Docker Compose (recommandé)

```bash
docker compose up -d
```

- Frontend : http://localhost:5173
- Backend (API) : http://localhost:8080/api
- Swagger UI : http://localhost:8080/swagger-ui.html
- PostgreSQL : localhost:5432 (utilisateur/mot de passe définis dans `.env`)

Les migrations Flyway (`backend/src/main/resources/db/migration`) s'exécutent automatiquement au démarrage du backend.

```bash
docker compose down          # arrêter
docker compose down -v       # arrêter + supprimer les données PostgreSQL
docker compose logs -f backend
```

## 6. Lancement en développement (sans Docker)

### Base de données

```bash
createuser --pwprompt vecoassets      # mot de passe : voir .env
createdb -O vecoassets vecoassets
```

### Backend

```bash
cd backend
export $(grep -v '^#' ../.env | xargs)   # ou configurez ces variables dans votre IDE
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

L'API démarre sur http://localhost:8080 (préfixe `/api`), Swagger sur `/swagger-ui.html`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Le frontend démarre sur http://localhost:5173 et proxifie `/api` vers `http://localhost:8080` (voir `frontend/vite.config.ts`).

### Tests (Phase 10)

```bash
cd backend
mvn test                 # unitaires (Mockito) + intégration (Testcontainers, nécessite Docker)
```

```bash
cd frontend
npm run test              # Vitest — 29 tests, réellement exécutés dans cet environnement de développement (voir docs/ROADMAP.md section 13)
```

Les tests backend (38 unitaires + 12 d'intégration, Phase 10, plus `AuthenticationIntegrationTest` de la Phase 3) n'ont **jamais pu être exécutés** dans l'environnement où ce dépôt a été développé (ni Maven ni démon Docker accessibles) — écrits et relus avec rigueur, `mvn test` reste à lancer en priorité sur un poste avec Docker avant toute mise en production. Les tests frontend, eux, ont réellement tourné ici (`npm run test` → `vitest run`) et sont verts.

## 7. Migrations et données de démonstration

- Migrations de schéma : Flyway, `backend/src/main/resources/db/migration/V2__*.sql` à `V9__*.sql` (Phase 2) — hiérarchie de localisation (sites/bâtiments/étages/zones/localisations), RBAC (rôles/permissions/utilisateurs), catégories et formats d'étiquette, immobilisations, historique (affectations/mouvements/changements d'état), inventaire (campagnes/scans/anomalies), pièces jointes, audit trail et paramètres. `V1__init.sql` (Phase 1) ne fait qu'activer l'extension `pgcrypto`. `V10__referentiel_permissions.sql` et `V11__audit_action_suppression.sql` (Phase 4) ajoutent les permissions `REFERENTIEL_MANAGE`/`USER_MANAGE` et la valeur d'audit `SUPPRESSION`. `V12__etiquetage_permissions.sql` (Phase 6) ajoute les permissions `ETIQUETTE_GENERATE` (module `ETIQUETAGE`, accordée à `ADMIN` et `GESTIONNAIRE_PATRIMOINE`) et `ETIQUETTE_MANAGE` (module `ADMIN`, réservée à `ADMIN`). `V13__asset_movements_service_fields.sql` (Phase 8) ajoute six colonnes texte (`from_direction`/`to_direction`/`from_department`/`to_department`/`from_service`/`to_service`) sur `asset_movements`, symétriques aux colonnes site/local/utilisateur déjà présentes depuis la Phase 2. La Phase 9 (Reporting) n'ajoute **aucune migration** : `REPORT_VIEW`/`REPORT_EXPORT` (Phase 2) existaient déjà, et le tableau de bord/les rapports ne font que lire les tables existantes.
- Seed de démonstration VECOPHARM : `backend/src/main/resources/db/seed/V900__seed_demo_data.sql` — sites VSA/Alger/Oran/Béjaïa/Laghouat avec une hiérarchie de localisation complète, les 7 catégories du prompt maître, 7 utilisateurs de démo, 12 immobilisations réalistes (variées en catégorie/site/état/statut). Chargé **uniquement** en profil `dev` ou `demo` (`spring.flyway.locations` ajoute `classpath:db/seed`), jamais en `prod`. Numéroté à partir de `V900` pour ne jamais entrer en collision avec les futures migrations de schéma.
- Ces migrations ont été rejouées à froid sur une base PostgreSQL 16 vierge (V1→V12→seed) : contraintes uniques (code immobilisation, numéro de série, affectation courante unique par bien, code de bâtiment/étage/zone/localisation unique par parent), contraintes `CHECK` (énumérations état/statut, actions d'audit), intégrité des clés étrangères (y compris le blocage `ON DELETE RESTRICT` d'une suppression de site/catégorie/format d'étiquette encore utilisé) et stockage JSONB (`audit_logs`) tous vérifiés en conditions réelles.
- La Phase 5 (Immobilisations) n'ajoute **aucune nouvelle migration** : la table `assets`, la séquence `asset_code_seq` et les paramètres `asset_code.*` existent depuis la Phase 2 et le seed. La logique métier ajoutée (génération de code, historique d'état/statut, historique d'affectation) a néanmoins été simulée directement en SQL contre cette même base rejouée à froid — voir `docs/ROADMAP.md` section 13 pour le détail, y compris un risque d'ordonnancement Hibernate reproduit et corrigé à cette occasion.
- La Phase 6 (Étiquetage) ajoute `V12` (permissions) et s'appuie sur les tables `asset_label_formats`/`asset_labels` créées dès la Phase 2. La logique métier ajoutée (CRUD des formats, génération d'étiquettes avec pose du flag `labeled` et écriture d'audit `GENERATION_ETIQUETTE`) a de même été simulée en SQL contre la base rejouée à froid. La génération réelle du PDF (ZXing + PDFBox) n'a pu être vérifiée que par revue manuelle de l'API — Maven Central inaccessible pour une compilation réelle, voir `docs/ROADMAP.md` section 13.
- La Phase 9 (Reporting) **n'ajoute aucune migration**, comme la Phase 7 : `REPORT_VIEW`/`REPORT_EXPORT` (Phase 2, déjà réparties par rôle) existaient déjà. Le tableau de bord et les 11 types de rapport recalculent tout à la demande depuis les tables existantes ; le même `ReportResultDto` alimente à la fois l'affichage écran et les trois exports (CSV, Excel via Apache POI — seule nouvelle dépendance Maven de cette phase —, PDF via PDFBox déjà présent depuis la Phase 6), garantissant qu'il ne peut jamais y avoir de divergence entre ce qui s'affiche et ce qui se télécharge. Chaque indicateur/ligne de rapport a été confronté à une requête SQL équivalente contre la base rejouée à froid, avec des mouvements/scans/anomalies insérés pour couvrir des cas réels — voir `docs/ROADMAP.md` section 13.
- La Phase 8 (Mouvements) ajoute `V13` (six colonnes texte, aucune nouvelle table ni permission) et s'appuie sur `asset_movements` créée dès la Phase 2. Le workflow Demande → Validation → Exécution → Historisation et les 9 types de mouvement ont été simulés en SQL contre la base rejouée à froid, y compris la fermeture/ouverture d'affectation avec la même précaution de `saveAndFlush` qu'en Phase 5 et le blocage définitif de tout mouvement sur une immobilisation déjà réformée — voir `docs/ROADMAP.md` section 13.
- La Phase 7 (Inventaire) **n'ajoute aucune migration** — fait inédit dans ce projet, chaque phase précédente en avait ajouté au moins une : `inventory_campaigns`/`inventory_scans`/`inventory_anomalies` (Phase 2) et les permissions `INVENTAIRE_VIEW/CREATE/EXECUTE/VALIDATE` (Phase 2, déjà réparties par rôle) existaient déjà et n'attendaient que ce module pour être utilisées. La logique métier (workflow de statut, scan avec vérification de périmètre, calcul de progression, gestion des anomalies) a été simulée en SQL contre la base rejouée à froid, avec la découverte et la correction d'un bug réel sur le calcul de progression (voir `docs/ROADMAP.md` section 13 pour le détail).

## 8. Comptes de démonstration

Créés par le seed (Phase 2), et l'authentification (`POST /api/auth/login`, Phase 3) est censée les accepter — le hash BCrypt en base correspond bien au mot de passe ci-dessous — mais **non confirmé par une exécution réelle** dans cet environnement (ni Maven ni Docker disponibles pour compiler/lancer le backend, voir `docs/ROADMAP.md` section 13). La page de connexion frontend (Phase 4, `/login`) est prête à les utiliser dès que le backend tourne réellement. Mot de passe de démo pour tous les comptes ci-dessous : `VecoDemo#2026`.

| Email | Rôle | Site |
|---|---|---|
| sami.benreskallah@vecopharm.dz | ADMIN | Alger |
| ahmed.benali@vecopharm.dz | GESTIONNAIRE_PATRIMOINE | VSA |
| sarah.gacem@vecopharm.dz | CONSULTATION | Alger |
| karim.bensalah@vecopharm.dz | RESPONSABLE_SERVICE | Alger |
| nabil.kaci@vecopharm.dz | RESPONSABLE_SITE | Oran |
| fatima.zahra@vecopharm.dz | INVENTORISTE | Laghouat |
| mohamed.reda@vecopharm.dz | INVENTORISTE | VSA |

## 9. API

Documentation interactive : `/swagger-ui.html` (spec OpenAPI sur `/v3/api-docs`).

Endpoints disponibles à ce stade (Phases 1-9 — la Phase 10 n'ajoute aucun nouvel endpoint, uniquement des tests/durcissements sur l'existant) :

| Méthode | Chemin | Accès | Description |
|---|---|---|---|
| GET | `/api/health` | public | vérifie que le backend répond réellement (Phase 1) |
| POST | `/api/auth/login` | public | authentifie un utilisateur (email + mot de passe), renvoie un JWT et le profil (email, nom, rôles) |
| GET | `/api/auth/me` | authentifié | profil de l'utilisateur courant, déduit du JWT |
| GET | `/api/admin/audit-logs` | authentifié + permission `ADMIN_ACCESS` | dernières entrées du journal d'audit (`audit_logs`), première fonctionnalité réelle protégée par permission (pas seulement par rôle) |
| GET / POST / PUT / `{id}/activate` / `{id}/deactivate` / DELETE | `/api/sites` | GET : authentifié — écriture : `REFERENTIEL_MANAGE` | CRUD sites (Phase 4) |
| GET (`?siteId=`) / POST / PUT / activate / deactivate / DELETE | `/api/buildings` | idem | CRUD bâtiments |
| GET (`?buildingId=`) / POST / PUT / activate / deactivate / DELETE | `/api/floors` | idem | CRUD étages |
| GET (`?floorId=`) / POST / PUT / activate / deactivate / DELETE | `/api/zones` | idem | CRUD zones |
| GET (`?zoneId=`) / POST / PUT / activate / deactivate / DELETE | `/api/locations` | idem | CRUD localisations |
| GET (`?parentId=`) / POST / PUT / activate / deactivate / DELETE | `/api/asset-categories` | idem | CRUD catégories/sous-catégories d'immobilisation |
| GET | `/api/roles` | authentifié | liste des 6 rôles V1 (lecture seule, pas de CRUD rôle en V1) |
| GET / POST / PUT / `{id}/activate` / `{id}/deactivate` / `{id}/reset-password` | `/api/users` | permission `USER_MANAGE` (lecture et écriture) | gestion des comptes utilisateurs — jamais de suppression physique, voir `docs/ARCHITECTURE.md` section 5 |
| GET (`?page=&size=&sort=&siteId=&categoryId=&condition=&status=&labeled=&search=&includeDeleted=`) | `/api/assets` | permission `IMMOBILISATION_VIEW` | liste paginée/filtrée/triée des immobilisations (Phase 5), enveloppe `PageResponse` |
| GET | `/api/assets/{id}` | `IMMOBILISATION_VIEW` | fiche d'une immobilisation |
| GET | `/api/assets/{id}/status-history` | `IMMOBILISATION_VIEW` | historique des changements d'état/statut |
| GET | `/api/assets/{id}/assignments` | `IMMOBILISATION_VIEW` | historique des affectations |
| POST | `/api/assets` | `IMMOBILISATION_CREATE` | création (code généré automatiquement, jamais saisi) |
| PUT | `/api/assets/{id}` | `IMMOBILISATION_EDIT` | modification (écrit l'historique d'état/statut/affectation si ces champs changent) |
| POST | `/api/assets/{id}/archive` | `IMMOBILISATION_ARCHIVE` | archivage (suppression logique — jamais physique, voir `docs/ARCHITECTURE.md` section 5) |
| GET | `/api/asset-label-formats` | authentifié | liste des formats d'étiquette (Phase 6) |
| POST / PUT / `{id}/activate` / `{id}/deactivate` / DELETE | `/api/asset-label-formats` | permission `ETIQUETTE_MANAGE` | CRUD des formats d'étiquette (dimensions mm, contenu affiché — jamais codées en dur, voir `docs/ARCHITECTURE.md` section 5) ; suppression bloquée si le format a déjà servi à une génération |
| POST | `/api/labels/generate` | permission `ETIQUETTE_GENERATE` | génère un PDF réel (une page par immobilisation, aux dimensions du format choisi) pour la liste d'immobilisations fournie, écrit l'historique `asset_labels` et l'audit `GENERATION_ETIQUETTE` |
| GET | `/api/assets/{id}/labels` | `IMMOBILISATION_VIEW` | historique des étiquettes générées pour une immobilisation |
| GET | `/api/inventory-campaigns` | `INVENTAIRE_VIEW` | liste des campagnes d'inventaire (Phase 7) |
| GET | `/api/inventory-campaigns/{id}` | `INVENTAIRE_VIEW` | fiche d'une campagne |
| GET | `/api/inventory-campaigns/{id}/progress` | `INVENTAIRE_VIEW` | progression calculée à la demande (immobilisations du périmètre scannées/présentes/restantes, anomalies) — jamais un compteur stocké, voir `docs/ARCHITECTURE.md` section 6 |
| GET | `/api/inventory-campaigns/{id}/pending-assets` | `INVENTAIRE_VIEW` | immobilisations du périmètre pas encore scannées |
| GET | `/api/inventory-campaigns/{id}/scans` | `INVENTAIRE_VIEW` | historique des scans de la campagne |
| GET | `/api/inventory-campaigns/{id}/anomalies` | `INVENTAIRE_VIEW` | anomalies de la campagne |
| POST | `/api/inventory-campaigns` | `INVENTAIRE_CREATE` | création d'une campagne (statut initial `BROUILLON`) |
| PUT | `/api/inventory-campaigns/{id}` | `INVENTAIRE_CREATE` | modification d'une campagne |
| PUT | `/api/inventory-campaigns/{id}/status` | `INVENTAIRE_CREATE` | fait avancer le statut d'une étape (`BROUILLON→EN_PREPARATION→EN_COURS→TERMINE`), jamais un saut d'étape |
| POST | `/api/inventory-campaigns/{id}/validate` | `INVENTAIRE_VALIDATE` | fait passer une campagne `TERMINE` à `VALIDE` |
| POST | `/api/inventory-campaigns/{id}/close` | `INVENTAIRE_VALIDATE` | clôture une campagne `VALIDE` (`CLOTURE`) — plus aucun scan accepté ensuite |
| POST | `/api/inventory-campaigns/{id}/scans` | `INVENTAIRE_EXECUTE` | enregistre un scan (campagne `EN_COURS` uniquement) ; vérifie l'appartenance au périmètre et force une anomalie `MAUVAISE_LOCALISATION`/`NON_REFERENCEE` si besoin, voir `docs/ARCHITECTURE.md` section 6 |
| GET | `/api/inventory-anomalies` | `INVENTAIRE_VIEW` | liste globale des anomalies, toutes campagnes confondues |
| PUT | `/api/inventory-anomalies/{id}/status` | `INVENTAIRE_VALIDATE` | fait transiter une anomalie vers `RESOLUE`/`REJETEE` (statuts terminaux) |
| POST | `/api/inventory-anomalies/{id}/photos` | `INVENTAIRE_EXECUTE` | ajoute une photo à une anomalie (upload multipart) |
| GET | `/api/inventory-anomalies/{id}/photos` | `INVENTAIRE_VIEW` | métadonnées des photos jointes à une anomalie |
| GET | `/api/attachments/{id}/download` | `INVENTAIRE_VIEW` | télécharge le fichier d'une pièce jointe |
| GET | `/api/assets/{id}/inventory-scans` | `IMMOBILISATION_VIEW` | historique des scans d'inventaire pour une immobilisation (onglet "Inventaire" de la fiche) |
| GET (`?assetId=&status=&type=`) | `/api/movements` | `IMMOBILISATION_VIEW` | liste des mouvements, filtrable par immobilisation/statut/type (Phase 8) — même convention de permission que `/assignments`/`/status-history` : un mouvement est une donnée d'historique d'immobilisation, pas un module à permission de lecture dédiée |
| GET | `/api/movements/{id}` | `IMMOBILISATION_VIEW` | fiche d'un mouvement |
| POST | `/api/movements` | `MOUVEMENT_CREATE` | demande d'un mouvement (statut initial `DEMANDE`) — l'état "avant" est capturé depuis l'immobilisation elle-même, jamais depuis le client |
| POST | `/api/movements/{id}/validate` | `MOUVEMENT_VALIDATE` | fait passer un mouvement `DEMANDE` à `VALIDE` |
| POST | `/api/movements/{id}/reject` | `MOUVEMENT_VALIDATE` | fait passer un mouvement `DEMANDE` à `REJETE` |
| POST | `/api/movements/{id}/execute` | `MOUVEMENT_VALIDATE` | exécute un mouvement `VALIDE` (`EXECUTE`) — seule étape où l'immobilisation est réellement modifiée, après revérification des règles métier |
| GET | `/api/dashboard` | `REPORT_VIEW` | tableau de bord (Phase 9) : totaux, étiquetées/inventoriées, anomalies ouvertes, répartition par statut, valeur d'acquisition totale, répartitions par site/catégorie/état physique, activité récente — tout recalculé à la demande, voir `docs/ARCHITECTURE.md` section 6 |
| GET (`?siteId=&categoryId=&condition=&status=&movementType=&anomalyStatus=&dateFrom=&dateTo=`) | `/api/reports/{type}` | `REPORT_VIEW` | calcule l'un des 11 rapports (`PAR_SITE`, `PAR_CATEGORIE`, `PAR_SERVICE`, `PAR_UTILISATEUR`, `PAR_ETAT`, `NON_ETIQUETEES`, `NON_INVENTORIEES`, `ANOMALIES`, `MOUVEMENTS`, `TRANSFERTS`, `REFORMES` — Phase 9), chaque filtre n'étant retenu que s'il est pertinent pour le type demandé |
| GET (mêmes filtres `+ format=CSV\|XLSX\|PDF`) | `/api/reports/{type}/export` | `REPORT_EXPORT` | exporte le même rapport que ci-dessus (CSV, Excel ou PDF) — mêmes lignes que celles affichées à l'écran, jamais de logique dupliquée |

Toute autre route est protégée par défaut (`anyRequest().authenticated()`) : un JWT valide (`Authorization: Bearer <token>`) est requis.

## 10. Structure du projet

```
veco-assets/
├── docs/
│   ├── ARCHITECTURE.md
│   ├── ROADMAP.md
│   └── SECURITY.md    revue de sécurité Phase 10
├── backend/            Spring Boot (Maven)
├── frontend/           React (Vite)
├── docker-compose.yml
├── .env.example
└── README.md
```

## 11. Dépannage (Troubleshooting)

| Symptôme | Cause probable | Solution |
|---|---|---|
| `docker compose up` échoue sur `postgres` avec "password authentication failed" | `.env` non créé ou incohérent avec un volume PostgreSQL déjà initialisé | `cp .env.example .env` puis `docker compose down -v` pour repartir d'un volume propre |
| Backend : `Flyway ... Unable to obtain connection` | PostgreSQL pas encore prêt ou variables `DATABASE_*` absentes | Vérifiez `docker compose ps` (le service `postgres` doit être `healthy`) et le contenu de `.env` |
| Backend ne compile pas / dépendances Maven introuvables | Registre Maven Central inaccessible depuis l'environnement de build | Vérifiez l'accès réseau sortant vers `repo.maven.apache.org` (proxy/pare-feu d'entreprise) |
| Frontend : erreurs sur `@tailwindcss/vite` ou `tailwindcss-animate` | Tailwind v4 utilise `@plugin "..."` dans `src/index.css`, pas `tailwind.config.js` | Ne pas régénérer `tailwind.config.js` / `postcss.config.js` — la config v4 vit dans `vite.config.ts` et `src/index.css` |
| `401`/`403` inattendus une fois l'auth branchée (Phase 3) | Rôle/permission manquant côté backend | Les permissions sont vérifiées côté backend (`@PreAuthorize`), pas seulement dans la sidebar frontend — vérifiez `security/` |
| `mvn test` échoue à démarrer les tests d'intégration (`AuthenticationIntegrationTest`, `VecoAssetsApplicationTests`) | Testcontainers a besoin d'un démon Docker accessible | Vérifiez `docker info` ; ces tests démarrent un vrai conteneur PostgreSQL 16 (voir `AbstractIntegrationTest`), ils ne peuvent pas tourner sans Docker |
| Impossible de supprimer un site/bâtiment/étage/zone/localisation/catégorie (`422 BUSINESS_RULE_VIOLATION`) | La donnée est encore référencée (enfant, utilisateur ou immobilisation) | C'est voulu (Phase 4) — désactivez-la (`active = false`) plutôt que de la supprimer ; voir `docs/ARCHITECTURE.md` section 5 |
| Le menu "Utilisateurs" n'apparaît pas dans la sidebar | Le compte connecté n'a pas la permission `USER_MANAGE` (seul `ADMIN` l'a par défaut) | Attendu — le menu suit les permissions réelles du compte, voir `hooks/use-auth.tsx` |
| Impossible de supprimer un format d'étiquette (`422 BUSINESS_RULE_VIOLATION`) | Le format a déjà servi à générer au moins une étiquette (`asset_labels`) | C'est voulu (Phase 6) — désactivez-le (`active = false`) plutôt que de le supprimer |
| `POST /api/labels/generate` échoue mais le message d'erreur n'apparaît pas dans le toast frontend | La réponse est demandée en `blob` (pour recevoir le PDF), donc le corps d'erreur JSON arrive lui aussi en `Blob`, pas en JSON déjà parsé | Géré via `lib/api-error.ts` → `extractBlobApiErrorMessage`, qui relit le blob et parse le JSON avant affichage — si un nouvel appel API en `responseType: 'blob'` est ajouté, réutiliser cette fonction plutôt que `extractApiErrorMessage` |
| `PUT /api/inventory-campaigns/{id}/status` (ou `/validate`, `/close`) renvoie `422 BUSINESS_RULE_VIOLATION` | Tentative de saut d'étape (ex. `BROUILLON` → `EN_COURS` directement) ou de retour en arrière | Voulu (Phase 7) — le workflow de statut n'avance que d'une étape à la fois et n'est jamais réversible, voir `docs/ARCHITECTURE.md` section 6 |
| `POST /api/inventory-campaigns/{id}/scans` renvoie `422 BUSINESS_RULE_VIOLATION` | La campagne n'est pas au statut `EN_COURS` (trop tôt ou déjà clôturée) | Voulu (Phase 7) — seules les campagnes `EN_COURS` acceptent des scans |
| Un scan valide revient avec le résultat `MAUVAISE_LOCALISATION` alors que l'immobilisation existe bien | L'immobilisation scannée n'appartient pas au site (ou à la zone) de la campagne | Voulu (Phase 7) — le backend revérifie systématiquement l'appartenance au périmètre, jamais seulement sur la base du résultat demandé par le client, voir `docs/ARCHITECTURE.md` section 6 |
| `POST /api/movements` renvoie `422 BUSINESS_RULE_VIOLATION` avec un message sur une immobilisation réformée | L'immobilisation est déjà au statut `REFORME` | Voulu (Phase 8) — une immobilisation réformée ne peut plus faire l'objet d'aucun nouveau mouvement, voir `docs/ARCHITECTURE.md` section 6 |
| `POST /api/movements/{id}/execute` renvoie `422 BUSINESS_RULE_VIOLATION` | Le mouvement n'est pas au statut `VALIDE` (encore `DEMANDE`, déjà `EXECUTE`, ou `REJETE`) | Voulu (Phase 8) — seul un mouvement `VALIDE` peut être exécuté ; les transitions de statut ne sont jamais sautées |
| `POST /api/movements` avec un type `CHANGEMENT_LOCALISATION` renvoie `422 BUSINESS_RULE_VIOLATION` sur le local choisi | Le local sélectionné appartient à un autre site que l'immobilisation | Voulu (Phase 8) — un changement de localisation reste sur le même site ; utilisez un transfert inter-site pour changer de site, voir `docs/ARCHITECTURE.md` section 6 |
| `GET /api/dashboard` ou `/api/reports/{type}` renvoie `403 Forbidden` | Le compte n'a pas la permission `REPORT_VIEW` (cas d'`INVENTORISTE`, qui ne l'a jamais eue) | Voulu (Phase 9) — le tableau de bord et les rapports sont réservés aux rôles disposant de `REPORT_VIEW` |
| `GET /api/reports/{type}/export` renvoie `403 Forbidden` alors que `GET /api/reports/{type}` fonctionne | Le compte a `REPORT_VIEW` mais pas `REPORT_EXPORT` (cas de `RESPONSABLE_SITE`/`RESPONSABLE_SERVICE`/`CONSULTATION`) | Voulu (Phase 9) — consulter un rapport à l'écran et en télécharger un fichier sont deux permissions distinctes |
| Le rapport `REFORMES` affiche `-` sur la date/le motif de réforme pour une immobilisation | L'immobilisation est reformée depuis le jeu de données de démonstration (Phase 2), sans mouvement `REFORME` associé | Voulu (Phase 9) — seule une réforme réalisée via le workflow de mouvement (Phase 8) porte une date/un motif réels, voir `docs/ARCHITECTURE.md` section 6 |

## 12. Parcours de bout en bout (checklist)

Le prompt maître définit un parcours cible en 19 étapes (RECENSEMENT → VÉRIFICATION → CODIFICATION → ÉTIQUETAGE → AFFECTATION → INVENTAIRE → CONTRÔLE → MISE À JOUR DU RÉFÉRENTIEL, décliné en actions concrètes). **Avertissement honnête** : la checklist ci-dessous correspond à ce que le code de ce dépôt permet, vérifié endpoint par endpoint et écran par écran au fil des Phases 3 à 9 (voir `docs/ROADMAP.md` pour le détail phase par phase) — elle n'a **pas** été rejouée sur une application réellement démarrée dans l'environnement où ce dépôt a été développé (Maven Central et Docker tous deux inaccessibles ici, voir `docs/ROADMAP.md` section 13). À dérouler sur un poste/CI avec Docker et un accès Maven Central normal, dans l'ordre, avant toute mise en production :

1. `docker compose up -d` (ou lancement manuel backend/frontend/PostgreSQL, section 5/6) démarre les trois services sans erreur.
2. Connexion (`/login`) avec un compte de démonstration (section 8) réussit, redirige vers le tableau de bord, et le menu affiche uniquement les entrées permises par le rôle connecté.
3. Référentiel (`/sites`) : créer un site, un bâtiment, un étage, une zone, une localisation, une catégorie — chacun apparaît immédiatement dans les listes déroulantes des formulaires Immobilisation/Inventaire.
4. Utilisateurs (`/utilisateurs`, `ADMIN`) : créer un utilisateur, lui affecter un rôle, vérifier que son menu change en conséquence à sa connexion.
5. Immobilisations (`/immobilisations/nouveau`) : créer une immobilisation — le code `VECO-IMM-XXXXXX` est généré automatiquement (jamais saisi), état `NEUF`, statut `EN_STOCK` par défaut.
6. Fiche immobilisation (`/immobilisations/:id`) : modifier l'état physique ou le statut, vérifier qu'une ligne apparaît dans l'onglet historique d'état.
7. Formats d'étiquette (`/sites`, onglet dédié) : vérifier les 3 formats de démonstration, ou en créer un nouveau (dimensions mm, contenu affiché).
8. Étiquetage (`/etiquetage`) : sélectionner une ou plusieurs immobilisations non étiquetées, générer les étiquettes — un vrai PDF s'ouvre (QR code + éventuellement code-barres), `assets.labeled` passe à `true`.
9. Vérifier que le PDF généré s'ouvre correctement dans un lecteur PDF réel et que le QR code scanné (téléphone) redonne bien le code immobilisation, rien d'autre.
10. Mouvements (`/mouvements`) : demander une affectation initiale sur l'immobilisation créée à l'étape 5 — vérifier qu'elle reste `EN_STOCK` tant que le mouvement n'est que `DEMANDE`.
11. Valider puis exécuter ce mouvement (`GESTIONNAIRE_PATRIMOINE`/`ADMIN`) — vérifier que le statut ne passe à `EN_SERVICE` (et l'affectation courante ne change) qu'à l'exécution, jamais avant.
12. Inventaire (`/inventaires`) : créer une campagne sur le site de l'immobilisation, la faire avancer jusqu'à `EN_COURS`.
13. Onglet Scanner : scanner (douchette/caméra configurée en sortie clavier, ou saisie manuelle) le code de l'immobilisation — résultat `PRESENT`, progression de la campagne mise à jour immédiatement.
14. Scanner un code inexistant, puis une immobilisation d'un autre site — vérifier respectivement une anomalie `NON_REFERENCEE` et `MAUVAISE_LOCALISATION` créées automatiquement.
15. Anomalies (`/anomalies`) : ajouter une photo à une anomalie, changer son statut vers `RESOLUE`.
16. Clôturer la campagne (`INVENTAIRE_VALIDATE`) — vérifier qu'un nouveau scan sur cette campagne est bien rejeté (`422`).
17. Tableau de bord (`/`) : vérifier que les indicateurs (totaux, étiquetées/inventoriées, anomalies ouvertes, répartitions) reflètent bien toutes les actions ci-dessus.
18. Rapports (`/rapports`) : consulter `PAR_SITE` et `MOUVEMENTS`, vérifier que les filtres pertinents s'affichent selon le type choisi, puis exporter en CSV/Excel/PDF (`REPORT_EXPORT`) — comparer les trois fichiers téléchargés au tableau affiché à l'écran (doivent être identiques, même méthode de génération des deux côtés, voir `docs/ARCHITECTURE.md` section 6).
19. Se connecter avec un compte `CONSULTATION` : vérifier que la création/modification/export sont bien refusés côté backend (`403`), pas seulement masqués côté frontend — puis réduire la fenêtre du navigateur sous 1024px de large et vérifier le tiroir de navigation mobile (bouton hamburger, overlay, fermeture au clic sur un lien, Phase 10).

## 13. Notes pour la suite

- Ne pas contourner l'ordre des phases (`docs/ROADMAP.md`) : chaque phase doit compiler, migrer et démarrer réellement avant de passer à la suivante.
- Toute règle métier (unicité de code/série, immobilisation réformée non ré-affectable, campagne clôturée sans nouveaux scans, etc.) se contrôle côté backend, jamais uniquement côté frontend.
- Aucune suppression physique d'immobilisation : suppression logique (`deleted = true`, endpoint `/api/assets/{id}/archive`) uniquement, historique toujours conservé. Les données de référentiel tolèrent une suppression physique, mais seulement si elles ne sont encore référencées nulle part (voir `docs/ARCHITECTURE.md` section 5) ; les comptes utilisateurs ne sont eux jamais supprimés physiquement (désactivation uniquement).
- Tout changement d'état physique, de statut opérationnel ou d'affectation d'une immobilisation écrit une ligne d'historique (`asset_status_history` / `asset_assignments`) — jamais un écrasement silencieux, voir `docs/ARCHITECTURE.md` section 6.
- Le QR code (et le code-barres) d'une étiquette n'encode jamais que le code d'immobilisation, jamais une URL ni une donnée personnelle (prompt maître section 13) ; les dimensions de la page PDF viennent toujours du format sélectionné, jamais d'une taille fixe codée en dur — voir `docs/ARCHITECTURE.md` section 6.
- Une campagne d'inventaire clôturée (`CLOTURE`) n'accepte plus aucun scan, et le workflow de statut n'avance que d'une étape à la fois, jamais en arrière ni par saut — contrôlé exclusivement côté backend (`InventoryCampaignService.transition`), voir `docs/ARCHITECTURE.md` section 6.
- La progression d'une campagne (immobilisations scannées/restantes) est toujours recalculée à la demande à partir des scans réels croisés avec le périmètre, jamais un compteur stocké — voir `docs/ARCHITECTURE.md` section 6 pour le bug réel trouvé et corrigé sur ce point en Phase 7.
- Un mouvement d'immobilisation (Phase 8) ne modifie jamais `assets` directement à la demande ou à la validation : seule l'exécution (`MovementService.execute`, statut `VALIDE` → `EXECUTE`) applique l'effet réel, après revérification des règles métier — jamais de confiance aveugle dans l'état capturé à la demande, voir `docs/ARCHITECTURE.md` section 6.
- Chaque indicateur du tableau de bord et chaque ligne de rapport (Phase 9) est recalculé à la demande depuis les tables existantes, jamais un chiffre stocké — et le même calcul (`ReportResultDto`) alimente à la fois l'écran et les exports CSV/Excel/PDF, ce qui rend impossible toute divergence entre ce qui s'affiche et ce qui se télécharge, voir `docs/ARCHITECTURE.md` section 6.
- La Phase 10 (Qualité, dernière phase V1) ajoute 38 tests unitaires + 12 tests d'intégration backend (écrits, non exécutés ici), 29 tests frontend Vitest (**réellement exécutés, tous verts**), une revue de sécurité (`docs/SECURITY.md`), l'élimination des N+1 les plus significatifs par `@EntityGraph`, un correctif d'audit trail et un tiroir de navigation mobile — voir `docs/ROADMAP.md` Phase 10 pour le détail complet et la section 12 ci-dessus pour la checklist de parcours de bout en bout.
- Avant toute mise en production : lancer `mvn test` (avec Docker) dans un environnement normal pour confirmer les Phases 2 à 10 côté backend — en particulier la génération réelle du PDF via ZXing/PDFBox (Phase 6), le fichier Excel via Apache POI (Phase 9) et l'ensemble des 50 tests écrits en Phase 10, jamais exécutés dans cet environnement (voir `docs/ROADMAP.md` section 13) — puis dérouler la checklist de la section 12 sur l'application réellement démarrée.
