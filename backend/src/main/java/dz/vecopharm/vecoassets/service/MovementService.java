package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.AssetDto;
import dz.vecopharm.vecoassets.dto.MovementCreateRequest;
import dz.vecopharm.vecoassets.dto.MovementDto;
import dz.vecopharm.vecoassets.dto.MovementRejectRequest;
import dz.vecopharm.vecoassets.entity.Asset;
import dz.vecopharm.vecoassets.entity.AssetAssignment;
import dz.vecopharm.vecoassets.entity.AssetCondition;
import dz.vecopharm.vecoassets.entity.AssetMovement;
import dz.vecopharm.vecoassets.entity.AssetStatus;
import dz.vecopharm.vecoassets.entity.AssetStatusHistory;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.entity.Location;
import dz.vecopharm.vecoassets.entity.MovementStatus;
import dz.vecopharm.vecoassets.entity.MovementType;
import dz.vecopharm.vecoassets.entity.Site;
import dz.vecopharm.vecoassets.entity.StatusHistoryField;
import dz.vecopharm.vecoassets.entity.User;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.mapper.AssetMapper;
import dz.vecopharm.vecoassets.mapper.MovementMapper;
import dz.vecopharm.vecoassets.repository.AssetAssignmentRepository;
import dz.vecopharm.vecoassets.repository.AssetMovementRepository;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import dz.vecopharm.vecoassets.repository.AssetStatusHistoryRepository;
import dz.vecopharm.vecoassets.repository.LocationRepository;
import dz.vecopharm.vecoassets.repository.SiteRepository;
import dz.vecopharm.vecoassets.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mouvements d'immobilisation (prompt maitre Phase 8 / sections 17-18).
 * Workflow strict Demande -> Validation -> Execution -> Historisation :
 * {@link #request} cree la ligne {@code DEMANDE}, {@link #validate}/
 * {@link #reject} la font passer a {@code VALIDE}/{@code REJETE},
 * {@link #execute} (uniquement depuis {@code VALIDE}) applique enfin l'effet
 * du mouvement sur {@link Asset} - jamais avant. L'immobilisation elle-meme
 * n'est donc modifiee qu'a la toute derniere etape, jamais des la demande
 * (meme principe que {@code AssetMovement} : "l'entite ne porte que l'etat
 * courant", prompt maitre section 18).
 */
@Service
public class MovementService {

    private final AssetMovementRepository movementRepository;
    private final AssetRepository assetRepository;
    private final AssetAssignmentRepository assignmentRepository;
    private final AssetStatusHistoryRepository statusHistoryRepository;
    private final SiteRepository siteRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final MovementMapper movementMapper;
    private final AssetMapper assetMapper;
    private final AuditRecorder auditRecorder;

    public MovementService(
            AssetMovementRepository movementRepository,
            AssetRepository assetRepository,
            AssetAssignmentRepository assignmentRepository,
            AssetStatusHistoryRepository statusHistoryRepository,
            SiteRepository siteRepository,
            LocationRepository locationRepository,
            UserRepository userRepository,
            MovementMapper movementMapper,
            AssetMapper assetMapper,
            AuditRecorder auditRecorder
    ) {
        this.movementRepository = movementRepository;
        this.assetRepository = assetRepository;
        this.assignmentRepository = assignmentRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.siteRepository = siteRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.movementMapper = movementMapper;
        this.assetMapper = assetMapper;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<MovementDto> list(UUID assetId, MovementStatus status, MovementType type) {
        List<AssetMovement> movements = assetId != null
                ? movementRepository.findByAssetIdOrderByRequestedAtDesc(assetId)
                : movementRepository.findAllByOrderByRequestedAtDesc();
        return movements.stream()
                .filter(m -> status == null || m.getStatus() == status)
                .filter(m -> type == null || m.getMovementType() == type)
                .map(movementMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MovementDto findById(UUID id) {
        return movementMapper.toDto(getOrThrow(id));
    }

    /**
     * Demande d'un mouvement. L'etat "avant" est capture depuis
     * l'immobilisation elle-meme (jamais depuis le client) ; l'etat "apres"
     * demande est valide selon le type de mouvement (voir
     * {@link #applyTargetFields}) mais n'est PAS encore applique a
     * l'immobilisation - il ne le sera qu'a {@link #execute}.
     */
    @Transactional
    public MovementDto request(MovementCreateRequest request) {
        Asset asset = assetRepository.findById(request.assetId())
                .orElseThrow(() -> new BusinessRuleException("Immobilisation introuvable"));
        if (asset.isDeleted()) {
            throw new BusinessRuleException("Impossible de creer un mouvement pour une immobilisation archivee");
        }
        if (asset.getStatus() == AssetStatus.REFORME) {
            throw new BusinessRuleException("Une immobilisation reformee ne peut plus faire l'objet d'un mouvement");
        }

        AssetMovement movement = new AssetMovement();
        movement.setAsset(asset);
        movement.setMovementType(request.movementType());

        movement.setFromSite(asset.getSite());
        movement.setFromLocation(asset.getLocation());
        movement.setFromUser(asset.getCurrentUser());
        movement.setFromDirection(asset.getDirection());
        movement.setFromDepartment(asset.getDepartment());
        movement.setFromService(asset.getService());

        applyTargetFields(movement, asset, request);

        movement.setRequestedBy(currentUser());
        movement.setRequestedAt(Instant.now());
        movement.setStatus(MovementStatus.DEMANDE);
        movement.setReason(blankToNull(request.reason()));
        movement.setComment(blankToNull(request.comment()));

        movement = movementRepository.save(movement);
        auditRecorder.record(AuditAction.CREATION, "MOUVEMENT", "asset_movements", movement.getId(), null, movementMapper.toDto(movement));
        return movementMapper.toDto(movement);
    }

    @Transactional
    public MovementDto validate(UUID id) {
        AssetMovement movement = getOrThrow(id);
        requireStatus(movement, MovementStatus.DEMANDE, "validee");
        MovementDto before = movementMapper.toDto(movement);
        movement.setStatus(MovementStatus.VALIDE);
        movement.setValidatedBy(currentUser());
        movement.setValidatedAt(Instant.now());
        movement = movementRepository.save(movement);
        auditRecorder.record(AuditAction.VALIDATION, "MOUVEMENT", "asset_movements", movement.getId(), before, movementMapper.toDto(movement));
        return movementMapper.toDto(movement);
    }

    @Transactional
    public MovementDto reject(UUID id, MovementRejectRequest request) {
        AssetMovement movement = getOrThrow(id);
        requireStatus(movement, MovementStatus.DEMANDE, "rejetee");
        MovementDto before = movementMapper.toDto(movement);
        movement.setStatus(MovementStatus.REJETE);
        movement.setValidatedBy(currentUser());
        movement.setValidatedAt(Instant.now());
        String rejectionComment = request != null ? blankToNull(request.comment()) : null;
        if (rejectionComment != null) {
            movement.setComment(hasText(movement.getComment()) ? movement.getComment() + " | Rejet : " + rejectionComment : "Rejet : " + rejectionComment);
        }
        movement = movementRepository.save(movement);
        auditRecorder.record(AuditAction.VALIDATION, "MOUVEMENT", "asset_movements", movement.getId(), before, movementMapper.toDto(movement));
        return movementMapper.toDto(movement);
    }

    /**
     * Execution : seule etape ou {@link Asset} est reellement modifiee.
     * Revalide les regles metier au lieu de faire confiance a l'etat capture
     * lors de la demande (l'immobilisation a pu changer entre-temps) - meme
     * discipline de reverification que le scan d'inventaire en Phase 7.
     */
    @Transactional
    public MovementDto execute(UUID id) {
        AssetMovement movement = getOrThrow(id);
        requireStatus(movement, MovementStatus.VALIDE, "executee");
        Asset asset = movement.getAsset();
        if (asset.isDeleted()) {
            throw new BusinessRuleException("Impossible d'executer un mouvement sur une immobilisation archivee");
        }
        AssetDto assetBefore = assetMapper.toDto(asset);

        applyEffects(movement, asset);
        asset = assetRepository.save(asset);

        movement.setStatus(MovementStatus.EXECUTE);
        movement.setExecutedAt(Instant.now());
        movement = movementRepository.save(movement);

        auditRecorder.record(movementAuditAction(movement.getMovementType()), "IMMOBILISATION", "assets", asset.getId(), assetBefore, assetMapper.toDto(asset));
        auditRecorder.record(AuditAction.CHANGEMENT_STATUT, "MOUVEMENT", "asset_movements", movement.getId(), MovementStatus.VALIDE, MovementStatus.EXECUTE);
        return movementMapper.toDto(movement);
    }

    // --- Demande : validation + resolution des champs cibles ---------------------------------------------------

    private void applyTargetFields(AssetMovement movement, Asset asset, MovementCreateRequest request) {
        switch (request.movementType()) {
            case AFFECTATION, CHANGEMENT_UTILISATEUR -> {
                User toUser = requireUser(request.toUserId(), "L'utilisateur destinataire est obligatoire pour ce type de mouvement");
                movement.setToUser(toUser);
                movement.setToDirection(blankToNull(request.toDirection()));
                movement.setToDepartment(blankToNull(request.toDepartment()));
                movement.setToService(blankToNull(request.toService()));
            }
            case CHANGEMENT_SERVICE -> {
                String toDirection = blankToNull(request.toDirection());
                String toDepartment = blankToNull(request.toDepartment());
                String toService = blankToNull(request.toService());
                if (toDirection == null && toDepartment == null && toService == null) {
                    throw new BusinessRuleException(
                            "Au moins une nouvelle direction, un nouveau departement ou un nouveau service est obligatoire pour ce type de mouvement");
                }
                movement.setToDirection(toDirection);
                movement.setToDepartment(toDepartment);
                movement.setToService(toService);
            }
            case CHANGEMENT_LOCALISATION -> {
                Location toLocation = requireLocation(request.toLocationId());
                if (!locationSite(toLocation).getId().equals(asset.getSite().getId())) {
                    throw new BusinessRuleException(
                            "Le nouveau local doit appartenir au site actuel de l'immobilisation - utilisez un transfert inter-site pour changer de site");
                }
                movement.setToLocation(toLocation);
            }
            case TRANSFERT_INTER_SITE -> {
                if (request.toSiteId() == null) {
                    throw new BusinessRuleException("Le site de destination est obligatoire pour un transfert inter-site");
                }
                Site toSite = siteRepository.findById(request.toSiteId())
                        .orElseThrow(() -> new BusinessRuleException("Site de destination introuvable"));
                if (toSite.getId().equals(asset.getSite().getId())) {
                    throw new BusinessRuleException(
                            "Le site de destination doit etre different du site actuel - utilisez un changement de localisation sinon");
                }
                movement.setToSite(toSite);
                if (request.toLocationId() != null) {
                    Location toLocation = requireLocation(request.toLocationId());
                    if (!locationSite(toLocation).getId().equals(toSite.getId())) {
                        throw new BusinessRuleException("Le local de destination doit appartenir au site de destination");
                    }
                    movement.setToLocation(toLocation);
                }
            }
            case RETOUR, MAINTENANCE, SORTIE, REFORME -> {
                // Aucun champ cible obligatoire : l'effet de ces mouvements
                // est uniquement un changement de statut (et, pour RETOUR,
                // la fermeture de l'affectation courante), applique a
                // l'execution - voir applyEffects.
            }
        }
    }

    // --- Execution : effet reel sur l'immobilisation ---------------------------------------------------

    private void applyEffects(AssetMovement movement, Asset asset) {
        User actor = currentUser();
        switch (movement.getMovementType()) {
            case AFFECTATION, CHANGEMENT_UTILISATEUR -> {
                String newDirection = movement.getToDirection() != null ? movement.getToDirection() : asset.getDirection();
                String newDepartment = movement.getToDepartment() != null ? movement.getToDepartment() : asset.getDepartment();
                String newService = movement.getToService() != null ? movement.getToService() : asset.getService();
                closeCurrentAssignment(asset.getId());
                openAssignment(asset, movement.getToUser(), newDirection, newDepartment, newService, movement.getComment(), actor);
                asset.setCurrentUser(movement.getToUser());
                asset.setDirection(newDirection);
                asset.setDepartment(newDepartment);
                asset.setService(newService);
                if (movement.getMovementType() == MovementType.AFFECTATION && asset.getStatus() == AssetStatus.EN_STOCK) {
                    changeStatus(asset, AssetStatus.EN_SERVICE, actor, movement.getComment());
                }
            }
            case CHANGEMENT_SERVICE -> {
                String newDirection = movement.getToDirection() != null ? movement.getToDirection() : asset.getDirection();
                String newDepartment = movement.getToDepartment() != null ? movement.getToDepartment() : asset.getDepartment();
                String newService = movement.getToService() != null ? movement.getToService() : asset.getService();
                closeCurrentAssignment(asset.getId());
                openAssignment(asset, asset.getCurrentUser(), newDirection, newDepartment, newService, movement.getComment(), actor);
                asset.setDirection(newDirection);
                asset.setDepartment(newDepartment);
                asset.setService(newService);
            }
            case CHANGEMENT_LOCALISATION -> {
                if (!locationSite(movement.getToLocation()).getId().equals(asset.getSite().getId())) {
                    throw new BusinessRuleException("Le site de l'immobilisation a change depuis la demande - ce mouvement n'est plus valide, il doit etre rejete");
                }
                applyLocation(asset, movement.getToLocation());
            }
            case TRANSFERT_INTER_SITE -> {
                asset.setSite(movement.getToSite());
                if (movement.getToLocation() != null) {
                    applyLocation(asset, movement.getToLocation());
                } else {
                    asset.setBuilding(null);
                    asset.setFloor(null);
                    asset.setZone(null);
                    asset.setLocation(null);
                }
            }
            case RETOUR -> {
                closeCurrentAssignment(asset.getId());
                asset.setCurrentUser(null);
                changeStatus(asset, AssetStatus.EN_STOCK, actor, movement.getComment());
            }
            case MAINTENANCE -> changeStatus(asset, AssetStatus.EN_MAINTENANCE, actor, movement.getComment());
            case SORTIE -> changeStatus(asset, AssetStatus.SORTI, actor, movement.getComment());
            case REFORME -> {
                changeStatus(asset, AssetStatus.REFORME, actor, movement.getComment());
                if (asset.getCondition() != AssetCondition.REFORME) {
                    AssetCondition previousCondition = asset.getCondition();
                    asset.setCondition(AssetCondition.REFORME);
                    recordFieldChange(asset, StatusHistoryField.CONDITION, previousCondition.name(), AssetCondition.REFORME.name(), actor, movement.getComment());
                }
            }
        }
    }

    private void applyLocation(Asset asset, Location location) {
        asset.setLocation(location);
        asset.setZone(location.getZone());
        asset.setFloor(location.getZone().getFloor());
        asset.setBuilding(location.getZone().getFloor().getBuilding());
    }

    private void changeStatus(Asset asset, AssetStatus target, User actor, String comment) {
        if (asset.getStatus() == target) {
            return;
        }
        AssetStatus previous = asset.getStatus();
        asset.setStatus(target);
        recordFieldChange(asset, StatusHistoryField.STATUS, previous.name(), target.name(), actor, comment);
    }

    private void recordFieldChange(Asset asset, StatusHistoryField field, String oldValue, String newValue, User actor, String comment) {
        AssetStatusHistory history = new AssetStatusHistory();
        history.setAsset(asset);
        history.setFieldName(field);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setChangedBy(actor);
        history.setChangedAt(Instant.now());
        history.setComment(comment);
        statusHistoryRepository.save(history);
    }

    /**
     * Cloture l'affectation courante (si presente) et flush immediatement -
     * meme precaution qu'{@code AssetService.closeCurrentAssignment} (Phase
     * 5) pour eviter que l'ordre de flush par defaut d'Hibernate
     * (INSERT avant UPDATE) ne viole l'index unique partiel
     * {@code uq_asset_assignments_current} en laissant coexister l'ancienne
     * ligne (pas encore fermee) et la nouvelle (pas encore inseree) dans le
     * meme flush.
     */
    private void closeCurrentAssignment(UUID assetId) {
        assignmentRepository.findByAssetIdAndAssignedUntilIsNull(assetId)
                .ifPresent(assignment -> {
                    assignment.setAssignedUntil(Instant.now());
                    assignmentRepository.saveAndFlush(assignment);
                });
    }

    private void openAssignment(Asset asset, User user, String direction, String department, String service, String comment, User actor) {
        AssetAssignment assignment = new AssetAssignment();
        assignment.setAsset(asset);
        assignment.setUser(user);
        assignment.setDirection(direction);
        assignment.setDepartment(department);
        assignment.setService(service);
        assignment.setAssignedFrom(Instant.now());
        assignment.setAssignedBy(actor);
        assignment.setComment(comment);
        assignmentRepository.save(assignment);
    }

    private AuditAction movementAuditAction(MovementType type) {
        return switch (type) {
            case TRANSFERT_INTER_SITE, CHANGEMENT_LOCALISATION -> AuditAction.TRANSFERT;
            case AFFECTATION, CHANGEMENT_UTILISATEUR, CHANGEMENT_SERVICE, RETOUR -> AuditAction.AFFECTATION;
            case MAINTENANCE, SORTIE, REFORME -> AuditAction.CHANGEMENT_STATUT;
        };
    }

    // --- Helpers ---------------------------------------------------

    private AssetMovement getOrThrow(UUID id) {
        return movementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mouvement introuvable"));
    }

    private void requireStatus(AssetMovement movement, MovementStatus expected, String actionLabel) {
        if (movement.getStatus() != expected) {
            throw new BusinessRuleException(
                    "Ce mouvement ne peut pas etre " + actionLabel + " : statut actuel " + movement.getStatus() + ", attendu " + expected);
        }
    }

    private User requireUser(UUID userId, String errorMessage) {
        if (userId == null) {
            throw new BusinessRuleException(errorMessage);
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("Utilisateur introuvable"));
    }

    private Location requireLocation(UUID locationId) {
        if (locationId == null) {
            throw new BusinessRuleException("Le local de destination est obligatoire pour ce type de mouvement");
        }
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new BusinessRuleException("Local introuvable"));
    }

    private Site locationSite(Location location) {
        return location.getZone().getFloor().getBuilding().getSite();
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
