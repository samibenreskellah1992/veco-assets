# VECO ASSETS

Plateforme de gestion, d'étiquetage, d'inventaire et de traçabilité des immobilisations de **VECOPHARM**.

> Processus cible : RECENSEMENT → VÉRIFICATION → CODIFICATION → ÉTIQUETAGE → AFFECTATION → INVENTAIRE → CONTRÔLE → MISE À JOUR DU RÉFÉRENTIEL

Ce dépôt en est à la **Phase 5 — Immobilisations** (voir `docs/ROADMAP.md`). Le backend et le frontend démarrent, la base de données Flyway est câblée (Phase 2), l'authentification JWT et le contrôle d'accès par rôle/permission sont en place (Phase 3, avec une vraie page de connexion côté frontend), l'administration du référentiel (sites, bâtiments, étages, zones, localisations, catégories, utilisateurs) est utilisable de bout en bout (Phase 4), et le registre des immobilisations (création, modification, archivage, recherche/filtres/tri/pagination, fiche détaillée avec historique d'état/statut et d'affectation) l'est également (Phase 5). Les modules restants (étiquetage, inventaire, mouvements, reporting, ...) ne sont pas encore développés — ce sont les phases suivantes. Voir `docs/ROADMAP.md` section 13 pour les limites de vérification propres à l'environnement de développement utilisé jusqu'ici (Maven Central et Docker inaccessibles ; les migrations et la logique métier ont pu être vérifiées en SQL réel, et le frontend a pu être compilé et lint-vérifié pour de vrai).

## 1. Présentation

- **Frontend** : React 18/19, TypeScript, Vite, Tailwind CSS v4, shadcn/ui, React Router, TanStack Query, React Hook Form + Zod, Lucide Icons.
- **Backend** : Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA/Hibernate, Bean Validation, Flyway, springdoc-openapi.
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

`components/ui/` porte les primitives shadcn/ui (bouton, champ, select, dialogue, onglets, ...) ajoutées à la main (voir `docs/ROADMAP.md` section 13). `features/referentiel/` porte les écrans Sites/Bâtiments/Étages/Zones/Localisations/Catégories (Phase 4), `hooks/use-auth.tsx` porte la session (JWT, restauration au démarrage, déconnexion). `pages/AssetsPage.tsx` (liste/filtres/pagination), `pages/AssetFormPage.tsx` (création/modification, localisation en cascade) et `pages/AssetDetailPage.tsx` (fiche à onglets) portent le module Immobilisations (Phase 5).

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

## 7. Migrations et données de démonstration

- Migrations de schéma : Flyway, `backend/src/main/resources/db/migration/V2__*.sql` à `V9__*.sql` (Phase 2) — hiérarchie de localisation (sites/bâtiments/étages/zones/localisations), RBAC (rôles/permissions/utilisateurs), catégories et formats d'étiquette, immobilisations, historique (affectations/mouvements/changements d'état), inventaire (campagnes/scans/anomalies), pièces jointes, audit trail et paramètres. `V1__init.sql` (Phase 1) ne fait qu'activer l'extension `pgcrypto`. `V10__referentiel_permissions.sql` et `V11__audit_action_suppression.sql` (Phase 4) ajoutent les permissions `REFERENTIEL_MANAGE`/`USER_MANAGE` et la valeur d'audit `SUPPRESSION`.
- Seed de démonstration VECOPHARM : `backend/src/main/resources/db/seed/V900__seed_demo_data.sql` — sites VSA/Alger/Oran/Béjaïa/Laghouat avec une hiérarchie de localisation complète, les 7 catégories du prompt maître, 7 utilisateurs de démo, 12 immobilisations réalistes (variées en catégorie/site/état/statut). Chargé **uniquement** en profil `dev` ou `demo` (`spring.flyway.locations` ajoute `classpath:db/seed`), jamais en `prod`. Numéroté à partir de `V900` pour ne jamais entrer en collision avec les futures migrations de schéma.
- Ces migrations ont été rejouées à froid sur une base PostgreSQL 16 vierge (V1→V11→seed) : contraintes uniques (code immobilisation, numéro de série, affectation courante unique par bien, code de bâtiment/étage/zone/localisation unique par parent), contraintes `CHECK` (énumérations état/statut, actions d'audit), intégrité des clés étrangères (y compris le blocage `ON DELETE RESTRICT` d'une suppression de site/catégorie encore utilisée) et stockage JSONB (`audit_logs`) tous vérifiés en conditions réelles.
- La Phase 5 (Immobilisations) n'ajoute **aucune nouvelle migration** : la table `assets`, la séquence `asset_code_seq` et les paramètres `asset_code.*` existent depuis la Phase 2 et le seed. La logique métier ajoutée (génération de code, historique d'état/statut, historique d'affectation) a néanmoins été simulée directement en SQL contre cette même base rejouée à froid — voir `docs/ROADMAP.md` section 13 pour le détail, y compris un risque d'ordonnancement Hibernate reproduit et corrigé à cette occasion.

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

Endpoints disponibles à ce stade (Phases 1-5) :

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

Toute autre route est protégée par défaut (`anyRequest().authenticated()`) : un JWT valide (`Authorization: Bearer <token>`) est requis. Les endpoints métier restants (`/api/labels`, `/api/inventories`, `/api/reports`, ...) sont ajoutés phase par phase, voir `docs/ROADMAP.md`.

## 10. Structure du projet

```
veco-assets/
├── docs/
│   ├── ARCHITECTURE.md
│   └── ROADMAP.md
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

## 12. Notes pour la suite

- Ne pas contourner l'ordre des phases (`docs/ROADMAP.md`) : chaque phase doit compiler, migrer et démarrer réellement avant de passer à la suivante.
- Toute règle métier (unicité de code/série, immobilisation réformée non ré-affectable, campagne clôturée sans nouveaux scans, etc.) se contrôle côté backend, jamais uniquement côté frontend.
- Aucune suppression physique d'immobilisation : suppression logique (`deleted = true`, endpoint `/api/assets/{id}/archive`) uniquement, historique toujours conservé. Les données de référentiel tolèrent une suppression physique, mais seulement si elles ne sont encore référencées nulle part (voir `docs/ARCHITECTURE.md` section 5) ; les comptes utilisateurs ne sont eux jamais supprimés physiquement (désactivation uniquement).
- Tout changement d'état physique, de statut opérationnel ou d'affectation d'une immobilisation écrit une ligne d'historique (`asset_status_history` / `asset_assignments`) — jamais un écrasement silencieux, voir `docs/ARCHITECTURE.md` section 6.
- Avant la Phase 6 : lancer `mvn test` (avec Docker) dans un environnement normal pour confirmer les Phases 2 à 5 côté backend (voir `docs/ROADMAP.md` section 13).
