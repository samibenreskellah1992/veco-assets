# VECO ASSETS — Revue sécurité (Phase 10)

Revue de sécurité menée le 16/09/2026 sur l'ensemble du backend (les 10 modules livrés, Phases 2 à 9). Méthode : lecture de chaque composant de sécurité transverse (authentification, RBAC, CORS, upload, gestion des erreurs, secrets) puis vérification systématique, contrôleur par contrôleur, que chaque endpoint porte l'autorisation attendue. Ce document liste ce qui a été trouvé, ce qui a été corrigé, et ce qui reste un compromis assumé (avec la justification).

## 1. Authentification (JWT)

- Authentification locale (email + mot de passe BCrypt), JWT stateless signé HS256 (prompt maître section 33). Mot de passe : minimum 8 caractères (`UserCreateRequest`/`ResetPasswordRequest`), pas de règle de complexité au-delà — jugé suffisant pour un outil interne à accès restreint, pas exposé sur Internet public.
- Message d'échec de connexion générique (« Identifiants invalides »), ne révèle jamais si l'email existe — seule exception assumée : un compte désactivé renvoie « Compte désactivé » (distinction volontaire existante avant cette revue, UX prioritaire sur l'énumération de comptes ici jugée acceptable pour un outil interne).
- **Compromis assumé et documenté (pas un bug) : les autorités (rôles + permissions) sont embarquées dans le JWT au moment de la connexion et ne sont plus revérifiées en base à chaque requête** (`JwtAuthenticationFilter` ne fait aucun accès base — voir sa Javadoc). Conséquence concrète : désactiver un compte (`POST /api/users/{id}/deactivate`) ou modifier ses rôles ne prend effet qu'à l'expiration du jeton en cours (`app.jwt.expiration-minutes`, 480 min/8h par défaut) ou à sa prochaine connexion, pas immédiatement. C'est le prix du "stateless" explicitement choisi dès la Phase 3 pour éviter un aller-retour base sur chaque requête. Mitigation recommandée en production : réduire `JWT_EXPIRATION_MINUTES` si une révocation quasi immédiate est requise ; une vraie révocation (liste noire de jetons) sortirait du périmètre stateless actuel et n'a pas été ajoutée sans demande explicite.
- **Corrigé cette phase (gestion des secrets)** : `JwtService` démarrait avec la valeur de secours `change-me-in-env-never-commit-a-real-secret` de `application.yml` sans aucun garde-fou — cette valeur est documentée en clair dans ce dépôt, donc la conserver en dehors du développement permettrait à quiconque de forger un jeton avec les autorisations de son choix. `JwtService` refuse maintenant de démarrer si `app.jwt.secret` vaut toujours cette valeur par défaut et qu'aucun profil `dev`/`demo` n'est actif (échec rapide au démarrage plutôt qu'une faille silencieuse). Le profil `test` fournit déjà son propre secret (`application-test.yml`), non affecté.

## 2. RBAC (autorisations)

- `@EnableMethodSecurity` + `@PreAuthorize("hasAuthority('...')")` sur chaque contrôleur/méthode sensible (jamais côté frontend seul, prompt maître section 28) — revue exhaustive des 20 contrôleurs REST de l'application.
- **Corrigé cette phase** : `RoleController` (`GET /api/roles`) n'avait aucune annotation d'autorisation au-delà de l'authentification globale — seule brèche trouvée lors de cette revue. Cet endpoint alimente uniquement le formulaire d'affectation de rôles d'Administration > Utilisateurs (réservé à `USER_MANAGE`), mais restait appelable directement par n'importe quel compte authentifié. `RoleController` porte désormais `@PreAuthorize("hasAuthority('USER_MANAGE')")` au niveau classe, comme `UserController`. Sévérité limitée (`RoleDto` n'expose que code/libellé/description des rôles, jamais les permissions qui leur sont attachées) mais gap réel, maintenant fermé.
- Le reste du référentiel (sites, bâtiments, étages, zones, locaux, catégories, formats d'étiquette) expose sa lecture (`GET`) à tout compte authentifié — c'est volontaire et cohérent d'un module à l'autre (ces listes alimentent des menus déroulants partout dans l'application), seules les mutations sont réservées à `REFERENTIEL_MANAGE`/`ETIQUETTE_MANAGE`.

## 3. Upload de fichiers (photos d'anomalie)

- Seul point d'upload de l'application : `POST /api/inventory-anomalies/{id}/photos` (`InventoryAnomalyService.attachPhoto`).
- Nom de fichier généré en UUID, chemin de stockage vérifié par `normalize()` + `startsWith()` (défense en profondeur anti-traversée de répertoire, déjà en place depuis la Phase 7, revérifiée cette phase — toujours correcte).
- **Corrigé cette phase** : la validation du type de fichier n'était qu'un `contentType.startsWith("image/")`, qui laissait passer `image/svg+xml`. Un SVG peut embarquer du `<script>`, et `AttachmentController.download` sert le fichier avec `Content-Disposition: inline` — vecteur XSS stocké classique si le fichier est un jour ouvert en navigation directe (le rendu actuel côté frontend passe par un `<img>` sur un blob, qui neutralise déjà l'exécution de script SVG dans la plupart des navigateurs, mais ce n'est pas une garantie à long terme ni pour un futur consommateur de cette API). Remplacé par une liste blanche explicite de formats raster (`image/jpeg`, `image/png`, `image/webp`, `image/gif`, `image/heic`, `image/heif`) — une photo prise depuis un téléphone est toujours un raster, aucun cas d'usage réel perdu. `AttachmentController.download` ajoute en complément l'en-tête `X-Content-Type-Options: nosniff` (défense en profondeur peu coûteuse).
- Taille limitée à 10 Mo (`spring.servlet.multipart.max-file-size`, `application.yml`).

## 4. CORS

Origines autorisées externalisées (`CORS_ALLOWED_ORIGINS`, jamais codées en dur), méthodes limitées à celles réellement utilisées par l'API, `allowCredentials(true)` avec en-têtes reflétés (`*`) — combinaison valide et volontaire (le JWT est porté dans l'en-tête `Authorization`, pas dans un cookie, donc `allowCredentials` sert surtout les en-têtes personnalisés, pas des cookies de session).

## 5. Validation des entrées

- Bean Validation (`@Valid`) sur la quasi-totalité des `@RequestBody` des 20 contrôleurs.
- **Corrigé cette phase** : `MovementController.reject()` recevait son `MovementRejectRequest` (`@Size(max = 2000)` sur le commentaire de rejet) sans `@Valid` — la contrainte était déclarée mais jamais appliquée. `@Valid` ajouté ; reste compatible avec le corps optionnel (`required = false`) existant, Spring Validation ignore silencieusement un argument nul.
- Toutes les règles métier (transitions de statut, cohérence de la hiérarchie de localisation, blocage des mouvements sur une immobilisation réformée, etc.) sont vérifiées côté service, jamais seulement côté frontend — vérifié module par module depuis la Phase 5.

## 6. Gestion des erreurs

`GlobalExceptionHandler` ne renvoie jamais de trace de pile ni le message brut d'une exception non prévue (`handleGeneric` renvoie un message générique fixe, le détail de l'exception ne quitte jamais le backend) — déjà correct, revérifié cette phase, aucun changement nécessaire.

## 7. Suppression logique

Aucune route de suppression physique n'existe sur `assets` (prompt maître section 26) — `AssetRepository` n'expose d'ailleurs aucune méthode `delete` pour cette entité, seule `archive()` (suppression logique) est possible. Vérifié à nouveau cette phase : toujours vrai.

## 8. Ce qui n'a pas été changé (hors périmètre de cette phase)

- **Pas de limitation de débit (rate limiting) sur `/api/auth/login`** : aucune protection anti-brute-force au-delà de la politique de mot de passe. Ajouter un compteur d'échecs / verrouillage temporaire est une évolution de produit (nécessite une décision sur le comportement UX de déverrouillage), pas une simple correction — non ajoutée sans demande explicite.
- **Pas de renouvellement automatique de jeton (refresh token)** : le frontend doit re-authentifier l'utilisateur à l'expiration des 8h par défaut. Comportement volontaire du modèle stateless actuel, pas un oubli.
