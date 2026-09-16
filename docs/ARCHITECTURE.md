# VECO ASSETS — Architecture

Plateforme de gestion, d'étiquetage, d'inventaire et de traçabilité des immobilisations de VECOPHARM.

## 1. État initial du projet

Analyse effectuée le 16/09/2026 : aucun projet VECO ASSETS n'existait dans l'environnement de développement (répertoire vide, pas de dépôt Git). Il s'agit donc d'une initialisation complète, sans code existant à intégrer.

Toolchain disponible et validé sur l'environnement de build :
- Java 21 (OpenJDK 21.0.10), Maven 3.9.11, Gradle 8.14.3
- Node.js 22.22.2, npm 10.9.7
- Docker 29.4.3, Docker Compose v5.1.3
- PostgreSQL 16.13 (client)

## 2. Vue d'ensemble

```
┌─────────────┐      HTTPS/JSON      ┌──────────────┐      JDBC      ┌──────────────┐
│   Frontend   │ ───────────────────▶ │   Backend    │ ─────────────▶ │  PostgreSQL  │
│ React + Vite │ ◀─────────────────── │ Spring Boot  │ ◀───────────── │      16      │
└─────────────┘      REST API         └──────────────┘                └──────────────┘
                                              │
                                              ▼
                                     ┌──────────────────┐
                                     │ Stockage fichiers │
                                     │ (photos/documents)│
                                     └──────────────────┘
```

Frontend et backend communiquent exclusivement via une API REST JSON documentée (OpenAPI/Swagger). Le backend est la seule couche qui parle à PostgreSQL ; aucune logique métier n'est déportée côté client.

## 3. Stack technique

### Frontend
React 18, TypeScript, Vite, Tailwind CSS, shadcn/ui, React Router, TanStack Query (état serveur/cache), React Hook Form + Zod (formulaires/validation), Lucide Icons.

### Backend
Java 21, Spring Boot 3.x (Web, Security, Data JPA), Hibernate, Bean Validation, driver PostgreSQL, Flyway (migrations), springdoc-openapi (Swagger).

### Base de données
PostgreSQL 16+.

### Authentification (V1)
Authentification locale par identifiant/mot de passe, JWT, rôles et permissions contrôlés côté backend. L'architecture prévoit un point d'extension pour un futur fournisseur LDAP/Active Directory (interface `AuthenticationProvider` additionnelle), mais LDAP n'est **pas** implémenté en V1.

## 4. Architecture en couches (backend)

```
Controller (REST, validation d'entrée, mapping DTO)
   ↓
Service (logique métier, règles de gestion, transactions)
   ↓
Repository (Spring Data JPA, Specifications pour filtres dynamiques)
   ↓
Entity (JPA, PostgreSQL)
```

Séparation stricte des responsabilités :
- `controller/` — endpoints REST, aucune logique métier.
- `service/` — logique métier, orchestration, transactions (`@Transactional`).
- `repository/` — accès données, `JpaSpecificationExecutor` pour les filtres avancés.
- `entity/` — modèle JPA.
- `dto/` — objets d'entrée/sortie API (jamais d'exposition directe des entités).
- `mapper/` — conversion entity ↔ dto (MapStruct).
- `security/` — configuration Spring Security, JWT, permissions.
- `exception/` — `GlobalExceptionHandler`, erreurs API standardisées.
- `audit/` — intercepteurs/écouteurs d'audit trail (qui/quoi/quand/où/ancienne-nouvelle valeur).
- `specification/` — `Specification<T>` réutilisables pour les filtres combinés (site + catégorie + état + …).
- `config/` — CORS, OpenAPI, beans applicatifs.

Règle absolue : jamais de logique métier dans les controllers, jamais dans les composants React.

## 5. Modèle de données (V1)

Tables principales (créées progressivement en Phase 2 via migrations Flyway) :

`users`, `roles`, `permissions`, `user_roles`, `role_permissions` — identité et RBAC.
`sites`, `buildings`, `floors`, `zones`, `locations` — hiérarchie de localisation (Site → Bâtiment → Étage → Zone → Localisation), entièrement administrable, jamais codée en dur côté frontend.
`asset_categories` — catégories/sous-catégories d'immobilisations.
`assets` — immobilisation (identification, désignation, localisation courante, affectation courante, acquisition, état, statut).
`asset_assignments` — historique des affectations (utilisateur/service/département responsable dans le temps).
`asset_movements` — mouvements (affectation, transfert, changement de localisation, maintenance, sortie, réforme), avec ancien/nouveau site, ancienne/nouvelle localisation, ancien/nouvel utilisateur, demandeur, validateur, motif.
`asset_status_history` — historique des changements d'état/statut.
`inventory_campaigns` — campagnes d'inventaire (site, zone, responsable, dates, statut).
`inventory_scans` — scans réalisés pendant une campagne (immobilisation, utilisateur scanneur, date/heure, résultat).
`inventory_anomalies` — anomalies détectées (type, description, immobilisation, campagne, statut).
`asset_labels` — étiquettes générées (format, date de génération).
`attachments` — métadonnées des fichiers joints (photos, factures, PV) ; les fichiers eux-mêmes sont stockés hors PostgreSQL (système de fichiers/objet), la base ne stocke que les métadonnées (chemin, type, taille, propriétaire).
`audit_logs` — piste d'audit générique (utilisateur, date/heure, action, module, objet, ID objet, ancienne valeur, nouvelle valeur, IP).
`settings` — paramètres applicatifs (préfixe de code, format d'étiquette, etc.).

Principes transverses :
- Toute table métier porte `created_at` / `updated_at`.
- Contraintes `UNIQUE` sur le code immobilisation (`VECO-IMM-XXXXXX`) et sur le numéro de série quand renseigné.
- Suppression **logique uniquement** (`deleted = true` ou équivalent) sur les immobilisations : l'historique n'est jamais détruit.
- Index sur les colonnes de recherche/filtre fréquentes (site, catégorie, état, statut, code).

## 6. Flux métier clés

### Identifiant unique
Génération côté backend exclusivement, format `VECO-IMM-000001`, jamais de réutilisation d'un code déjà attribué (séquence PostgreSQL ou table de compteur transactionnelle). Contrainte `UNIQUE` en base en plus du contrôle applicatif.

### Traçabilité (qui/quoi/quand/où/pourquoi/ancienne-nouvelle valeur)
Toute modification critique (création, modification, affectation, transfert, changement de statut, validation, génération d'étiquette) écrit une entrée dans `audit_logs` et, pour les immobilisations, une entrée dans `asset_movements` ou `asset_status_history` selon le cas. Aucune écriture destructive : une mise à jour de champ métier significatif s'accompagne d'un enregistrement d'historique.

### Workflow de transfert
```
Demande → Validation → Exécution → Historisation
```
Le transfert n'est jamais appliqué directement sur l'entité `asset` sans passer par un `asset_movements` : le mouvement est la source de vérité, la mise à jour de l'état courant de l'immobilisation en est la conséquence.

### Inventaire par scan
```
Scan QR → Identification du bien → Vérification d'appartenance à la campagne
        → Affichage fiche → Enregistrement du contrôle (date, heure, utilisateur)
        → Confirmation présence OU déclaration d'anomalie
```

### Import Excel
```
Fichier → Validation (doublons, code/série existants, site/catégorie inexistants,
           champs obligatoires, formats) → Rapport (lignes valides / lignes en erreur)
        → Import des lignes valides uniquement (+ téléchargement du rapport d'erreurs)
```

## 7. Sécurité

- JWT pour l'authentification API (stateless), mots de passe hashés avec BCrypt.
- RBAC avec rôles (`ADMIN`, `GESTIONNAIRE_PATRIMOINE`, `RESPONSABLE_SITE`, `RESPONSABLE_SERVICE`, `INVENTORISTE`, `CONSULTATION`) et permissions granulaires (`IMMOBILISATION_VIEW`, `IMMOBILISATION_CREATE`, …), contrôlées côté backend sur chaque endpoint (annotations `@PreAuthorize` + vérification service), jamais uniquement côté frontend.
- CORS configuré explicitement (origines autorisées via configuration).
- `GlobalExceptionHandler` pour des réponses d'erreur standardisées (`timestamp`, `status`, `error`, `message`, `path`), jamais de fuite de stacktrace ou de secret.
- Secrets (URL base de données, identifiants, `JWT_SECRET`) exclusivement via variables d'environnement (`application.yml` avec `${VAR}`, `.env` non versionné, `.env.example` fourni).
- Logs applicatifs (SLF4J/Logback) : jamais de mot de passe, token ou donnée sensible en clair.
- Point d'extension prévu (non implémenté V1) : `AuthenticationProvider` LDAP/Active Directory.

## 8. Stratégie de déploiement

- Conteneurisation via Docker Compose : services `postgres`, `backend`, `frontend`, réseau interne dédié.
- Configuration par variables d'environnement (`.env`), aucun secret en dur dans les images ou le code.
- Migrations de schéma exclusivement via Flyway, exécutées automatiquement au démarrage du backend.
- Environnement de démonstration : jeu de données réalistes VECOPHARM (sites VSA, Alger, Oran, Béjaïa, Laghouat ; catégories Informatique, Mobilier, Véhicules, Matériel technique, Équipements, Matériel de bureau, Autres) chargé via un seed dédié (profil `dev`/`demo`), jamais en profil `prod`.
- Évolutivité prévue sans refonte : application mobile Flutter, mode offline, LDAP/AD, Power BI, intégration ERP comptable (via une couche `IntegrationService` découplée du domaine métier), intégration VECO-GED, notifications email/internes, signature électronique, NFC/RFID. Aucune de ces briques n'est développée en V1 ; l'architecture ne doit pas leur faire obstacle.

## 9. Ce que ce document n'est pas

Ce document décrit l'architecture cible et les décisions structurantes. Le détail des phases de développement et leurs critères de fin sont dans `docs/ROADMAP.md`. Les décisions de modélisation fine (colonnes exactes, types, contraintes complètes) seront affinées et documentées au fil des migrations Flyway en Phase 2, sans dévier des principes énoncés ici.
