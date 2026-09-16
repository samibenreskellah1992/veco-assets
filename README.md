# VECO ASSETS

Plateforme de gestion, d'étiquetage, d'inventaire et de traçabilité des immobilisations de **VECOPHARM**.

> Processus cible : RECENSEMENT → VÉRIFICATION → CODIFICATION → ÉTIQUETAGE → AFFECTATION → INVENTAIRE → CONTRÔLE → MISE À JOUR DU RÉFÉRENTIEL

Ce dépôt en est à la **Phase 1 — Initialisation du projet** (voir `docs/ROADMAP.md`). Le backend et le frontend démarrent, la base de données Flyway est câblée, mais aucun module métier (immobilisations, inventaire, étiquetage, ...) n'est encore développé — ce sont les phases suivantes.

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

- Migrations : Flyway, `backend/src/main/resources/db/migration/V*__*.sql`. `V1__init.sql` (Phase 1) active l'extension `pgcrypto` ; le schéma métier complet (users, roles, sites, assets, movements, inventories, audit_logs, ...) arrive en Phase 2.
- Seed de démonstration VECOPHARM (sites VSA/Alger/Oran/Béjaïa/Laghouat, catégories, immobilisations) : prévu en Phase 2, chargé uniquement en profil `dev`/`demo`, jamais en `prod`.

## 8. Comptes de démonstration

Pas encore disponibles — l'authentification est construite en Phase 3. Ce README sera mis à jour avec les comptes de démo (login/mot de passe/rôle) à ce moment-là.

## 9. API

Documentation interactive : `/swagger-ui.html` (spec OpenAPI sur `/v3/api-docs`). En Phase 1, seul `GET /api/health` est exposé, pour vérifier que le backend répond réellement. Les endpoints métier (`/api/assets`, `/api/inventories`, `/api/reports`, ...) sont ajoutés phase par phase, voir `docs/ROADMAP.md`.

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

## 12. Notes pour la suite

- Ne pas contourner l'ordre des phases (`docs/ROADMAP.md`) : chaque phase doit compiler, migrer et démarrer réellement avant de passer à la suivante.
- Toute règle métier (unicité de code/série, immobilisation réformée non ré-affectable, campagne clôturée sans nouveaux scans, etc.) se contrôle côté backend, jamais uniquement côté frontend.
- Aucune suppression physique d'immobilisation : suppression logique (`deleted = true`) uniquement, historique toujours conservé.
