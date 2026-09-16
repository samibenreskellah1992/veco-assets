# VECO ASSETS

Plateforme de gestion, d'étiquetage, d'inventaire et de traçabilité des immobilisations de **VECOPHARM**.

> Processus cible : RECENSEMENT → VÉRIFICATION → CODIFICATION → ÉTIQUETAGE → AFFECTATION → INVENTAIRE → CONTRÔLE → MISE À JOUR DU RÉFÉRENTIEL

Ce dépôt en est à la **Phase 3 — Authentification** (voir `docs/ROADMAP.md`). Le backend et le frontend démarrent, la base de données Flyway est câblée (Phase 2), l'authentification JWT et le contrôle d'accès par rôle/permission sont en place côté backend (Phase 3), mais les modules métier (immobilisations, inventaire, étiquetage, ...) ne sont pas encore développés — ce sont les phases suivantes. Voir `docs/ROADMAP.md` section 13 pour les limites de vérification propres à l'environnement de développement utilisé jusqu'ici (Maven Central et Docker inaccessibles).

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

- Migrations de schéma : Flyway, `backend/src/main/resources/db/migration/V2__*.sql` à `V9__*.sql` (Phase 2) — hiérarchie de localisation (sites/bâtiments/étages/zones/localisations), RBAC (rôles/permissions/utilisateurs), catégories et formats d'étiquette, immobilisations, historique (affectations/mouvements/changements d'état), inventaire (campagnes/scans/anomalies), pièces jointes, audit trail et paramètres. `V1__init.sql` (Phase 1) ne fait qu'activer l'extension `pgcrypto`.
- Seed de démonstration VECOPHARM : `backend/src/main/resources/db/seed/V900__seed_demo_data.sql` — sites VSA/Alger/Oran/Béjaïa/Laghouat avec une hiérarchie de localisation complète, les 7 catégories du prompt maître, 7 utilisateurs de démo, 12 immobilisations réalistes (variées en catégorie/site/état/statut). Chargé **uniquement** en profil `dev` ou `demo` (`spring.flyway.locations` ajoute `classpath:db/seed`), jamais en `prod`. Numéroté à partir de `V900` pour ne jamais entrer en collision avec les futures migrations de schéma.
- Ces migrations ont été rejouées à froid sur une base PostgreSQL 16 vierge (V1→V9→seed) dans le cadre de cette session : contraintes uniques (code immobilisation, numéro de série, affectation courante unique par bien), contraintes `CHECK` (énumérations état/statut), intégrité des clés étrangères et stockage JSONB (`audit_logs`) tous vérifiés en conditions réelles.

## 8. Comptes de démonstration

Créés par le seed (Phase 2), et l'authentification (`POST /api/auth/login`) construite en Phase 3 est censée les accepter — le hash BCrypt en base correspond bien au mot de passe ci-dessous — mais **non confirmé par une exécution réelle** dans cet environnement (ni Maven ni Docker disponibles pour compiler/lancer le backend, voir `docs/ROADMAP.md` section 13). Mot de passe de démo pour tous les comptes ci-dessous : `VecoDemo#2026`.

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

Endpoints disponibles à ce stade (Phases 1-3) :

| Méthode | Chemin | Accès | Description |
|---|---|---|---|
| GET | `/api/health` | public | vérifie que le backend répond réellement (Phase 1) |
| POST | `/api/auth/login` | public | authentifie un utilisateur (email + mot de passe), renvoie un JWT et le profil (email, nom, rôles) |
| GET | `/api/auth/me` | authentifié | profil de l'utilisateur courant, déduit du JWT |
| GET | `/api/admin/audit-logs` | authentifié + permission `ADMIN_ACCESS` | dernières entrées du journal d'audit (`audit_logs`), première fonctionnalité réelle protégée par permission (pas seulement par rôle) |

Toute autre route est protégée par défaut (`anyRequest().authenticated()`) : un JWT valide (`Authorization: Bearer <token>`) est requis. Les endpoints métier (`/api/assets`, `/api/inventories`, `/api/reports`, ...) sont ajoutés phase par phase, voir `docs/ROADMAP.md`.

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

## 12. Notes pour la suite

- Ne pas contourner l'ordre des phases (`docs/ROADMAP.md`) : chaque phase doit compiler, migrer et démarrer réellement avant de passer à la suivante.
- Toute règle métier (unicité de code/série, immobilisation réformée non ré-affectable, campagne clôturée sans nouveaux scans, etc.) se contrôle côté backend, jamais uniquement côté frontend.
- Aucune suppression physique d'immobilisation : suppression logique (`deleted = true`) uniquement, historique toujours conservé.
