# VECO Assets Mobile — Phase 0 : analyse (23/09/2026)

Analyse préalable au développement de l'application mobile Flutter "VECO Assets Mobile", conformément à la section 56 du prompt maître fourni par Sami. Aucun code Flutter n'a encore été écrit à ce stade — cette analyse doit être validée avant de démarrer la Phase 1 (fondations).

## 1. Environnement

- **Poste de développement** : `it-02-lap` (Windows 11, WSL2 Ubuntu), le même poste que pour le backend/frontend web.
- **Flutter / Dart** : non installés (ni détectés sur le pont utilisé pour lire/écrire les fichiers du dépôt). À vérifier par Sami lui-même avec `flutter doctor -v` sur son poste — comme pour Maven, les commandes Flutter (`pub get`, `analyze`, `test`, `run`, `build`) devront être exécutées par Sami, je n'ai pas accès à un environnement Flutter/Android SDK.
- **Android SDK / JDK** : à vérifier par Sami (`flutter doctor` les liste). Le JDK 21 déjà utilisé pour le backend devrait convenir, mais Android Studio / Android SDK / un émulateur (ou téléphone physique en USB debugging) restent à confirmer.
- **Aucun projet Flutter existant** : le dépôt `veco-assets` ne contient aujourd'hui que `backend/`, `frontend/`, `docs/` — pas de dossier `mobile/`. Projet entièrement à créer.

## 2. Emplacement du projet [À CONFIRMER]

Le dépôt existant est un mono-repo (`backend/` + `frontend/`). Proposition : ajouter un troisième dossier `mobile/` à la racine du même dépôt Git, cohérent avec la structure actuelle — pas de nouveau dépôt séparé, pas de nouvelle configuration CI/CD à dupliquer. **Sami à confirmer** que c'est bien ce qu'il souhaite (sinon : dépôt Git séparé).

## 3. Logo VECOPHARM [À CONFIRMER — bloquant pour le rendu final]

Recherche effectuée dans tout le dépôt (backend, frontend, docs) : **aucun fichier image de logo VECOPHARM n'existe actuellement dans le projet**. Le frontend web n'utilise que `favicon.svg` et `icons.svg` (génériques) ; le nom "VECOPHARM" y est affiché en texte stylisé (wordmark CSS), pas comme image.

La section 2 du prompt maître est explicite : le logo officiel doit être utilisé tel quel, jamais redessiné ni recréé. **Il faut que Sami fournisse les fichiers image réels** (`vecopharm_logo.png`, idéalement une variante blanche `vecopharm_logo_white.png` pour les fonds bleu foncé). En attendant, la Phase 1 pourra utiliser un wordmark texte temporaire respectant la charte de couleurs (voir section 6), clairement marqué comme provisoire, sans jamais inventer de logo graphique.

## 4. Design System — couleurs réelles (extraites du frontend web)

Le frontend web (`frontend/src/index.css`) définit déjà des tokens de couleur HSL (shadcn/ui) pour les modes clair et sombre. Plutôt que d'inventer une palette "bleu VECOPHARM" approximative, la palette mobile reprend ces valeurs exactes pour rester visuellement cohérente entre web et mobile :

| Rôle | Clair (HSL) | Sombre (HSL) |
|---|---|---|
| Primary | `217 91% 40%` | `217 91% 60%` |
| Secondary | `210 40% 96%` | `217 33% 17%` |
| Success | `142 71% 35%` | `142 71% 45%` |
| Warning | `38 92% 50%` | `38 92% 50%` |
| Destructive (erreur/anomalie) | `0 72% 51%` | `0 63% 31%` |
| Background | `0 0% 100%` | `222 47% 8%` |
| Foreground (texte) | `222 47% 11%` | `210 40% 98%` |
| Sidebar / bleu foncé corporate | `222 47% 11%` | `222 47% 6%` |

Ces valeurs seront traduites en `Color`/`ColorScheme` Dart dans `lib/core/theme/app_colors.dart` à la Phase 1. Le mode sombre existe déjà côté web — à confirmer si le mobile doit le supporter dès la V1 ou seulement le mode clair (le prompt ne mentionne pas explicitement de dark mode).

## 5. Architecture (conforme section 6 du prompt)

Clean Architecture + Feature First, Riverpod, GoRouter, Dio, Drift/SQLite, flutter_secure_storage — comme spécifié. Aucune déviation identifiée à ce stade. Structure `lib/core/` + `lib/features/{auth,dashboard,scanner,locations,assets,inventory,anomalies,movements,assignments,sync,profile}/{data,domain,presentation}` reprise telle quelle.

Point de vigilance : le module **`assignments`** n'a pas d'équivalent backend dédié (voir section 8 ci-dessous) — côté domaine mobile, ce sera une spécialisation de `movements` (type `AFFECTATION`), pas un module API séparé.

## 6. Modules et écrans — Phase 1 (première version visuelle, section 57)

Écrans mock à construire en premier, avec vraies données factices, avant tout branchement API réel : Splash, Login, Dashboard, Scanner, Scan Local, Détail Local, Inventaire Local, Scan Immobilisation, Fiche Immobilisation, Anomalie, Mouvements, Affectation, Locaux, Création Local, QR Local, Synchronisation, Profil, Hors connexion. Repris tel quel de la section 57.

## 7. Modèles de données

Les modèles Dart (`freezed`/`json_serializable`) seront calqués sur les DTO backend réels plutôt que réinventés : `LocationDto`, `AssetDto`, `LocationInventorySessionDto`, `LocationSessionProgressDto`, `MovementDto`, `InventoryAnomalyDto`, `LoginResponse`/`UserSummaryDto`, etc. — ces DTO existent déjà et sont stables (utilisés par le frontend web et les tests backend). Détail complet à produire en Phase 1/2 au fur et à mesure du branchement de chaque écran, pas listé exhaustivement ici pour ne pas surcharger cette analyse.

## 8. API — écart entre le prompt maître et l'API réelle [important, conforme à la règle de non-invention section 61/48]

Le prompt maître liste des endpoints indicatifs (section 48). Inspection réelle des contrôleurs Spring Boot (`backend/.../controller/`) :

**Endpoints existants et réutilisables tels quels** :
- `POST /api/auth/login`, `GET /api/auth/me` — authentification, conforme.
- `GET /api/locations/by-code/{code}` — conforme, déjà utilisé par le web (Checkpoint 3).
- `GET /api/locations/{id}`, `GET /api/locations/{id}/... `, `POST /api/locations`, `PUT /api/locations/{id}` — conformes.
- `GET /api/location-inventory-sessions`, `/{id}`, `/{id}/progress`, `/{id}/pending-assets`, `/{id}/scans`, `/{id}/anomalies`, `POST` (ouverture), `POST /{id}/scans` (scan), `POST /{id}/validate` — c'est le moteur exact du workflow d'inventaire par local (sections 15-23 du prompt), déjà livré et testé (Checkpoint 3, 107 tests verts). Le mobile doit consommer **cette** API, pas une API `/api/inventory/sessions/...` distincte comme suggéré section 48.
- `GET /api/assets`, `GET /api/assets/{id}`, `GET /api/assets/{id}/status-history`, `GET /api/assets/{id}/assignments`, `POST /api/assets`, `PUT /api/assets/{id}`, `POST /api/assets/{id}/archive`.
- `GET/POST /api/movements`, `POST /{id}/validate`, `/{id}/reject`, `/{id}/execute` — workflow Demande → Validation → Exécution déjà en place (Phase 8 web), à réutiliser pour les sections 26/27 (mouvements + affectation, cette dernière comme cas particulier d'un mouvement `AFFECTATION`).
- `GET /api/dashboard` — pour les indicateurs de la section 11.
- `GET /api/inventory-anomalies`, `PUT /{id}/status`, `POST/GET /{id}/photos`.

**Écarts identifiés à trancher avec Sami avant de coder les écrans concernés** :
1. **Pas de `GET /api/assets/by-code/{code}`** (contrairement à `locations/by-code`) — nécessaire pour le scan d'immobilisation (section 18). Deux options : (a) ajouter cet endpoint côté backend, symétrique à celui des locaux (changement additif, cohérent avec la philosophie du projet) ; (b) filtrer côté mobile via `GET /api/assets?search=<code>` et vérifier la correspondance exacte. **Recommandation : option (a)**, plus propre et plus rapide côté mobile — à valider.
2. **Pas de création manuelle d'anomalie** (`POST /api/inventory-anomalies` n'existe pas) — aujourd'hui les anomalies sont uniquement générées automatiquement par le moteur de scan (`NON_REFERENCEE`/`MAUVAISE_LOCALISATION`). Or les sections 20/21/24 du prompt prévoient un bouton "Déclarer une anomalie" avec des types manuels (étiquette endommagée, numéro de série incorrect, immobilisation endommagée, etc.) — **ceci nécessite un nouvel endpoint backend**, à planifier comme un ajout (probablement en même temps que la Phase 7 mobile "Anomalies").
3. **Pas de `/api/sync`** — normal, la synchronisation offline est une notion propre au mobile (Phase 11), à concevoir entièrement (format du batch, idempotence par `operation_id`, gestion des conflits) — aucun équivalent backend existant aujourd'hui.
4. **Pas de `POST /api/assignments`** dédié — l'affectation (section 27) est un mouvement de type `AFFECTATION` via `POST /api/movements`, pas un module API séparé.
5. **Permissions** : les codes indicatifs de la section 47 (`ASSET_READ`, `INVENTORY_EXECUTE`, etc.) ne correspondent pas aux permissions réelles. Les vraies permissions (table `permissions`, vérifiées par `@PreAuthorize` sur chaque contrôleur) sont : `IMMOBILISATION_VIEW/CREATE/EDIT/ARCHIVE`, `INVENTAIRE_VIEW/CREATE/EXECUTE/VALIDATE`, `MOUVEMENT_CREATE/VALIDATE`, `REPORT_VIEW/EXPORT`, `REFERENTIEL_MANAGE` (locaux/référentiel, ajoutée pour le module Locaux), `ADMIN_ACCESS`. C'est cette liste réelle qui pilotera le masquage des actions côté mobile (section 47), la sécurité réelle restant côté backend comme prévu.

## 9. Offline / Synchronisation

Conforme aux sections 33-37 du prompt : Drift/SQLite, `CurrentInventoryContext`, `SyncManager` avec `operation_id` UUID par opération. Aucun équivalent backend n'existe encore pour recevoir un batch de synchronisation — ce sera un développement backend + mobile conjoint, à traiter en Phase 11 (pas bloquant pour démarrer la Phase 1 visuelle avec données mock).

## 10. Risques identifiés

- **Logo manquant** (section 3) — bloque le rendu final pixel-perfect tant que Sami ne fournit pas les fichiers réels.
- **Trois endpoints backend manquants** (`assets/by-code`, création manuelle d'anomalie, `/api/sync`) — nécessitent du développement backend en plus du mobile, à budgéter dans les phases concernées plutôt que découverts tardivement.
- **Test sur téléphone physique** : le backend tourne en local sur le poste de Sami (`docker compose`, `localhost:8080`). Pour tester depuis un vrai téléphone Android (pas seulement un émulateur sur la même machine), il faudra soit une IP LAN accessible depuis le téléphone, soit un tunnel (ngrok ou équivalent) — à voir en Phase 2/3, non bloquant pour la Phase 1 (écrans mock).
- **Environnement Flutter non vérifié** : `flutter doctor` n'a pas encore été exécuté par Sami — un souci de configuration (Android SDK, licences, JDK) pourrait apparaître au premier `flutter create`, comme cela avait été le cas avec Testcontainers/Docker pour le backend.

## 11. Points à confirmer avec Sami avant la Phase 1

1. Le projet mobile vit dans le même dépôt `veco-assets` (dossier `mobile/`) — confirmé ou dépôt séparé ?
2. Fournir les fichiers réels du logo VECOPHARM (`vecopharm_logo.png` + variante blanche) — ou démarrer en Phase 1 avec un wordmark texte provisoire ?
3. Dark mode mobile dès la V1, ou clair uniquement pour commencer (le web le supporte déjà) ?
4. Ajout de `GET /api/assets/by-code/{code}` côté backend — d'accord pour cet ajout additif ?
5. Endpoint de création manuelle d'anomalie — à développer en même temps que l'écran Anomalies (Phase 7 mobile), confirmé ?
6. Accès au backend depuis un téléphone physique (IP LAN / tunnel) — à voir plus tard, pas de décision requise maintenant.

## 12. Prochaine étape proposée

Une fois ces points tranchés : Sami exécute `flutter doctor -v` sur son poste pour confirmer l'environnement, puis lance `flutter create mobile` (ou nom convenu) à la racine du dépôt — comme pour Maven, je ne peux pas exécuter Flutter moi-même. Je prends ensuite la main pour écrire à la main le Design System (`lib/core/theme/*`), la structure Clean Architecture, et les premiers écrans mock (Splash, Login, Dashboard, Scanner, Local, Inventaire Local) de la section 57, avec les couleurs réelles de la section 4 ci-dessus. Chaque étape sera vérifiée par Sami (`flutter analyze`, `flutter test`, puis test visuel sur émulateur/téléphone) avant de passer à la suivante, dans la continuité du même mode de fonctionnement par points de contrôle déjà en place pour le backend/frontend web.

## 13. Décisions de Sami (23/09/2026)

1. **Emplacement** : dossier `mobile/` dans le même dépôt `veco-assets` — confirmé.
2. **Logo VECOPHARM** : Sami fournit les fichiers réels (`vecopharm_logo.png` + variante blanche) avant que les écrans Splash/Login ne soient finalisés visuellement — pas de wordmark texte provisoire.
3. **`GET /api/assets/by-code/{code}`** : ajout backend confirmé — sera développé au moment de coder l'écran Scanner (Phase 5 mobile), pas avant (Phase 1 utilise des données mock, pas d'appel API réel).
4. **Création manuelle d'anomalie** : le nouvel endpoint `POST /api/inventory-anomalies` sera développé en même temps que l'écran Anomalies (Phase 7 mobile).

Phase 0 close. Prochaine étape : Sami exécute `flutter doctor -v` sur son poste et partage le résultat ; puis `flutter create mobile` à la racine du dépôt (je ne peux pas exécuter Flutter moi-même, comme pour Maven). J'écrirai ensuite à la main le Design System et les premiers écrans mock de la Phase 1.
