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
Java 21, Spring Boot 3.x (Web, Security, Data JPA), Hibernate, Bean Validation, driver PostgreSQL, Flyway (migrations), springdoc-openapi (Swagger). Génération de documents (Phase 6) : ZXing (QR code / code-barres Code128) et Apache PDFBox (construction du PDF d'étiquettes), toutes deux en licence Apache 2.0.

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

Pagination (introduite en Phase 5 pour `GET /api/assets`, premier endpoint de liste à en avoir besoin) : `dto/PageResponse<T>` enveloppe `org.springframework.data.domain.Page` dans une forme JSON stable et documentable (`content`, `page`, `size`, `totalElements`, `totalPages`) plutôt que de sérialiser directement le type Spring Data — réutilisable telle quelle par toute future liste paginée (inventaires, mouvements, ...).

## 5. Modèle de données (V1)

Tables principales (créées progressivement en Phase 2 via migrations Flyway) :

`users`, `roles`, `permissions`, `user_roles`, `role_permissions` — identité et RBAC.
`sites`, `buildings`, `floors`, `zones`, `locations` — hiérarchie de localisation (Site → Bâtiment → Étage → Zone → Localisation), entièrement administrable, jamais codée en dur côté frontend.
`asset_categories` — catégories/sous-catégories d'immobilisations (auto-référence pour les sous-catégories).
`asset_label_formats` — formats d'étiquette administrables (largeur/hauteur, contenu affiché) ; ajoutée en Phase 2 pour honorer l'exigence « dimensions jamais codées en dur » (prompt maître section 13), au lieu d'un simple couple clé/valeur générique. Utilisée pour de vrai depuis la Phase 6 : ce sont ces dimensions exactes (converties mm → points PDF) qui pilotent la taille de chaque page du PDF généré, jamais une taille de page fixe.
`assets` — immobilisation (identification, désignation, localisation courante, affectation courante, acquisition, état, statut).
`asset_assignments` — historique des affectations (utilisateur/service/département responsable dans le temps).
`asset_movements` — mouvements (affectation, transfert, changement de localisation, maintenance, sortie, réforme), avec ancien/nouveau site, ancienne/nouvelle localisation, ancien/nouvel utilisateur, demandeur, validateur, motif.
`asset_status_history` — historique des changements d'état/statut.
`inventory_campaigns` — campagnes d'inventaire (site, zone, responsable, dates, statut).
`inventory_scans` — scans réalisés pendant une campagne (immobilisation, utilisateur scanneur, date/heure, résultat).
`inventory_anomalies` — anomalies détectées (type, description, immobilisation, campagne, statut).
`asset_labels` — étiquettes générées (format, date de génération). Alimentée depuis la Phase 6 : une ligne par immobilisation à chaque génération de PDF (`AssetLabelService.generate`), jamais modifiée après coup (voir principes transverses ci-dessous) — c'est l'historique qui permet de répondre à « quand et avec quel format cette immobilisation a-t-elle été étiquetée ».
`attachments` — métadonnées des fichiers joints (photos, factures, PV) ; les fichiers eux-mêmes sont stockés hors PostgreSQL (système de fichiers/objet), la base ne stocke que les métadonnées (chemin, type, taille, propriétaire).
`audit_logs` — piste d'audit générique (utilisateur, date/heure, action, module, objet, ID objet, ancienne valeur, nouvelle valeur, IP).
`settings` — paramètres applicatifs (préfixe de code, format d'étiquette, etc.).

Principes transverses :
- Toute table métier porte `created_at` / `updated_at` ; les tables d'événements append-only (`asset_status_history`, `inventory_scans`, `asset_labels`, `attachments`, `audit_logs`) ne portent que leur horodatage de création — une ligne d'historique n'est jamais modifiée après coup.
- Contraintes `UNIQUE` sur le code immobilisation (`VECO-IMM-XXXXXX`) et sur le numéro de série quand renseigné (index unique partiel : NULL autorisé en doublon), ainsi qu'une affectation courante unique par immobilisation dans `asset_assignments`.
- Contraintes `CHECK` en base sur les colonnes d'énumération métier (état, statut, type de mouvement, statut de campagne, ...) en complément du contrôle applicatif — défense en profondeur, la source de vérité reste le service layer.
- Suppression **logique uniquement** (`deleted = true` ou équivalent) sur les immobilisations : l'historique n'est jamais détruit. Les données de référentiel (sites, bâtiments, étages, zones, localisations, catégories, Phase 4) tolèrent une suppression **physique**, mais seulement quand la ligne n'est encore référencée nulle part (aucun enfant dans la hiérarchie, aucun utilisateur, aucune immobilisation) — le service layer le vérifie avant toute suppression et la contrainte `FOREIGN KEY ... ON DELETE RESTRICT` reste le filet de sécurité final ; dans tous les autres cas, on désactive (`active = false`) plutôt que de casser l'historique. Les comptes utilisateurs, eux, ne sont **jamais** supprimés physiquement (seulement désactivés) : ils sont référencés par l'audit trail et par l'historique des immobilisations (utilisateur courant/responsable), et les effacer casserait cette traçabilité.
- Index sur les colonnes de recherche/filtre fréquentes (site, catégorie, état, statut, code).
- `attachments` est une association polymorphe (`owner_type` + `owner_id`, sans contrainte FK SQL puisque la cible varie par table) pour se rattacher indifféremment à une immobilisation, une anomalie ou un mouvement.

## 6. Flux métier clés

### Identifiant unique
Génération côté backend exclusivement (`service/AssetCodeGenerator`, Phase 5), format `VECO-IMM-000001` piloté par le paramètre administrable `settings.asset_code.format` (repli sur ce même format par défaut si le paramètre est absent ou mal formé — un paramètre invalide ne bloque jamais la création). Jamais de réutilisation d'un code déjà attribué : la séquence PostgreSQL `asset_code_seq` (créée en Phase 2) est incrémentée via `nextval()`, jamais un `count(*) + 1` qui se réutiliserait après une suppression. Contrainte `UNIQUE` en base en plus du contrôle applicatif.

### Traçabilité (qui/quoi/quand/où/pourquoi/ancienne-nouvelle valeur)
Toute modification critique (création, modification, affectation, transfert, changement de statut, validation, génération d'étiquette) écrit une entrée dans `audit_logs` et, pour les immobilisations, une entrée dans `asset_movements` ou `asset_status_history` selon le cas. Aucune écriture destructive : une mise à jour de champ métier significatif s'accompagne d'un enregistrement d'historique.

Depuis la Phase 5, `AssetService.update` applique concrètement ce principe : un changement d'état physique (`condition`) ou de statut opérationnel (`status`) écrit une ligne `asset_status_history` (ancienne/nouvelle valeur, auteur, date, commentaire optionnel) ; un changement d'affectation (utilisateur courant, direction/département/service) clôt la ligne `asset_assignments` courante (`assigned_until = now()`) et en ouvre une nouvelle plutôt que d'écraser les colonnes d'affectation courante de `assets` sans laisser de trace. Point d'attention vérifié en Phase 5 (voir `docs/ROADMAP.md` section 13) : l'ordre de flush par défaut d'Hibernate exécute les `INSERT` avant les `UPDATE` au sein d'une même transaction, ce qui violerait l'index unique partiel `uq_asset_assignments_current` si la clôture de l'ancienne affectation et l'ouverture de la nouvelle étaient flushées ensemble sans précaution — `AssetService` force donc un `saveAndFlush` sur la clôture avant d'insérer la nouvelle ligne.

### Étiquetage (Phase 6)
```
Sélection d'immobilisations + format d'étiquette → LabelPdfBuilder (une page par
immobilisation, aux dimensions réelles du format, mm → points) → pour chaque
immobilisation : ligne asset_labels + asset.labeled = true + audit
GENERATION_ETIQUETTE → PDF renvoyé (aperçu et téléchargement réutilisent le même
PDF déjà généré, sans nouvel appel ni nouvelle trace)
```
Le QR code (et, selon le format, le code-barres Code128) n'encode jamais que le code d'immobilisation (`VECO-IMM-000001`), jamais une URL ni une donnée personnelle (prompt maître section 13). Les dimensions de la page PDF ne sont jamais codées en dur : elles viennent des colonnes `width_mm`/`height_mm` du format sélectionné (`asset_label_formats`, administrable en Référentiel), converties en points PDF à la génération (`LabelPdfBuilder`). `LabelImageGenerator` produit les images QR/code-barres via ZXing ; `LabelPdfBuilder` construit le PDF via Apache PDFBox — voir `docs/ROADMAP.md` section 13 pour le niveau de vérification de ces deux dépendances (Maven Central bloqué dans l'environnement de développement, revue manuelle de l'API en lieu de compilation réelle). Génération soumise à la permission `ETIQUETTE_GENERATE` ; administration des formats à `ETIQUETTE_MANAGE` (migration `V12`).

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

- JWT pour l'authentification API (stateless), mots de passe hashés avec BCrypt. Implémentation (Phase 3) : `POST /api/auth/login` authentifie via `AuthenticationManager`/`DaoAuthenticationProvider` (backés par `CustomUserDetailsService`, qui charge rôles et permissions réels depuis la base), puis émet un JWT dont les autorités (`ROLE_<code>` + codes de permission) sont **embarquées dans les claims** au moment de la connexion. `JwtAuthenticationFilter` reconstruit ensuite le contexte de sécurité à chaque requête à partir de la signature du token, sans nouvel accès base — cohérent avec le choix stateless.
- RBAC avec rôles (`ADMIN`, `GESTIONNAIRE_PATRIMOINE`, `RESPONSABLE_SITE`, `RESPONSABLE_SERVICE`, `INVENTORISTE`, `CONSULTATION`) et permissions granulaires (`IMMOBILISATION_VIEW`, `IMMOBILISATION_CREATE`, …), contrôlées côté backend sur chaque endpoint (`@PreAuthorize` + `@EnableMethodSecurity`, jamais uniquement côté frontend). `GET /api/admin/audit-logs` (`ADMIN_ACCESS`) sert de premier exemple réel de ce contrôle, et est couvert par un test d'intégration positif (rôle avec la permission) et négatif (rôle sans la permission → 403).
- Phase 4 (`V10__referentiel_permissions.sql`) ajoute deux permissions : `REFERENTIEL_MANAGE` (écriture sur sites/bâtiments/étages/zones/localisations/catégories — `ADMIN` et `GESTIONNAIRE_PATRIMOINE`) et `USER_MANAGE` (gestion des comptes utilisateurs — `ADMIN` uniquement). La **lecture** de ces ressources référentiel n'est volontairement pas verrouillée par une permission dédiée : elle est nécessaire à tout utilisateur authentifié pour les listes déroulantes des futurs modules métier (Phase 5+) ; seule l'écriture est contrôlée. `/api/users` fait exception et reste protégé en lecture comme en écriture (`USER_MANAGE`), la liste des comptes étant plus sensible qu'une donnée géographique.
- Phase 6 (`V12__etiquetage_permissions.sql`) ajoute `ETIQUETTE_GENERATE` (générer des étiquettes pour des immobilisations existantes — `ADMIN` et `GESTIONNAIRE_PATRIMOINE`, même périmètre que la création/modification d'immobilisations) et `ETIQUETTE_MANAGE` (définir les formats d'étiquette — `ADMIN` uniquement, même logique que `USER_MANAGE`). Comme pour le référentiel, la lecture de `/api/asset-label-formats` reste ouverte à tout utilisateur authentifié (nécessaire au sélecteur de format du module `/etiquetage`) ; seules l'écriture des formats et la génération elle-même sont contrôlées.
- `audit/AuditRecorder` (Phase 4) centralise l'écriture dans `audit_logs` — résolution de l'utilisateur courant et de l'adresse IP depuis le contexte de la requête HTTP, sérialisation JSON de l'ancienne/nouvelle valeur — pour que les services du référentiel (et, dans les phases suivantes, ceux des immobilisations/inventaire/mouvements) n'aient pas chacun à reconstruire un `AuditLog` à la main comme le faisait `AuthService` en Phase 3.
- Réponses 401/403 systématiquement au format `ApiError` (jamais la page par défaut de Spring Security) : `JsonAuthenticationEntryPoint` pour l'absence/invalidité de token, `JsonAccessDeniedHandler` en repli au niveau filtre, `GlobalExceptionHandler` pour les échecs de `authenticate()` (identifiants invalides, compte désactivé) et les refus `@PreAuthorize` levés pendant l'exécution d'un contrôleur.
- CORS configuré explicitement (origines autorisées via configuration).
- `GlobalExceptionHandler` pour des réponses d'erreur standardisées (`timestamp`, `status`, `error`, `message`, `path`), jamais de fuite de stacktrace ou de secret. Le message d'échec de connexion ne révèle jamais si c'est l'email ou le mot de passe qui est incorrect.
- Secrets (URL base de données, identifiants, `JWT_SECRET`) exclusivement via variables d'environnement (`application.yml` avec `${VAR}`, `.env` non versionné, `.env.example` fourni).
- Logs applicatifs (SLF4J/Logback) : jamais de mot de passe, token ou donnée sensible en clair.
- Point d'extension prévu (non implémenté V1) : `AuthenticationProvider` LDAP/Active Directory.
- Côté frontend (Phase 4, rattrape un point non traité en Phase 3) : `hooks/use-auth.tsx` (`AuthProvider`/`useAuth`) restaure la session depuis le token stocké (`localStorage`, `lib/token-storage.ts`) au chargement, `services/api-client.ts` attache `Authorization: Bearer <token>` sur chaque requête et déclenche un événement `veco-assets:unauthorized` sur toute réponse 401 (hors échec de login lui-même) ; `routes/ProtectedRoute.tsx` redirige vers `/login` en l'absence de session. Les contrôles de permission côté UI (`hasPermission`, ex. masquer le menu Utilisateurs sans `USER_MANAGE`) ne sont qu'un confort d'affichage — le backend reste la seule source de vérité (`@PreAuthorize`).

## 8. Stratégie de déploiement

- Conteneurisation via Docker Compose : services `postgres`, `backend`, `frontend`, réseau interne dédié.
- Configuration par variables d'environnement (`.env`), aucun secret en dur dans les images ou le code.
- Migrations de schéma exclusivement via Flyway, exécutées automatiquement au démarrage du backend.
- Environnement de démonstration : jeu de données réalistes VECOPHARM (sites VSA, Alger, Oran, Béjaïa, Laghouat ; catégories Informatique, Mobilier, Véhicules, Matériel technique, Équipements, Matériel de bureau, Autres) chargé via un seed dédié (profil `dev`/`demo`), jamais en profil `prod`.
- Évolutivité prévue sans refonte : application mobile Flutter, mode offline, LDAP/AD, Power BI, intégration ERP comptable (via une couche `IntegrationService` découplée du domaine métier), intégration VECO-GED, notifications email/internes, signature électronique, NFC/RFID. Aucune de ces briques n'est développée en V1 ; l'architecture ne doit pas leur faire obstacle.

## 9. Ce que ce document n'est pas

Ce document décrit l'architecture cible et les décisions structurantes. Le détail des phases de développement et leurs critères de fin sont dans `docs/ROADMAP.md`. Les décisions de modélisation fine (colonnes exactes, types, contraintes complètes) seront affinées et documentées au fil des migrations Flyway en Phase 2, sans dévier des principes énoncés ici.
