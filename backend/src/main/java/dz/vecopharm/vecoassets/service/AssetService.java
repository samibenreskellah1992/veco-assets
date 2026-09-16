package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.AssetAssignmentDto;
import dz.vecopharm.vecoassets.dto.AssetCreateRequest;
import dz.vecopharm.vecoassets.dto.AssetDto;
import dz.vecopharm.vecoassets.dto.AssetStatusHistoryDto;
import dz.vecopharm.vecoassets.dto.AssetUpdateRequest;
import dz.vecopharm.vecoassets.dto.PageResponse;
import dz.vecopharm.vecoassets.entity.Asset;
import dz.vecopharm.vecoassets.entity.AssetAssignment;
import dz.vecopharm.vecoassets.entity.AssetCategory;
import dz.vecopharm.vecoassets.entity.AssetCondition;
import dz.vecopharm.vecoassets.entity.AssetStatus;
import dz.vecopharm.vecoassets.entity.AssetStatusHistory;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.entity.Building;
import dz.vecopharm.vecoassets.entity.Floor;
import dz.vecopharm.vecoassets.entity.Location;
import dz.vecopharm.vecoassets.entity.Site;
import dz.vecopharm.vecoassets.entity.StatusHistoryField;
import dz.vecopharm.vecoassets.entity.User;
import dz.vecopharm.vecoassets.entity.Zone;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.mapper.AssetAssignmentMapper;
import dz.vecopharm.vecoassets.mapper.AssetMapper;
import dz.vecopharm.vecoassets.mapper.AssetStatusHistoryMapper;
import dz.vecopharm.vecoassets.repository.AssetAssignmentRepository;
import dz.vecopharm.vecoassets.repository.AssetCategoryRepository;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import dz.vecopharm.vecoassets.repository.AssetStatusHistoryRepository;
import dz.vecopharm.vecoassets.repository.BuildingRepository;
import dz.vecopharm.vecoassets.repository.FloorRepository;
import dz.vecopharm.vecoassets.repository.LocationRepository;
import dz.vecopharm.vecoassets.repository.SiteRepository;
import dz.vecopharm.vecoassets.repository.UserRepository;
import dz.vecopharm.vecoassets.repository.ZoneRepository;
import dz.vecopharm.vecoassets.specification.AssetSpecification;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Coeur du module Immobilisations (prompt maitre Phase 5 / section 10-11).
 * Ne porte que l'etat courant sur {@link Asset} ; tout changement d'etat
 * (condition/statut) ou d'affectation ecrit une ligne d'historique
 * correspondante ({@link AssetStatusHistory} / {@link AssetAssignment})
 * plutot que d'ecraser silencieusement l'ancienne valeur - c'est la seule
 * facon de reconstituer "qui avait cette immobilisation le 12 mars" plus
 * tard (Phase 8 - Mouvements s'appuiera sur les memes tables).
 *
 * Suppression : toujours logique ({@link #archive(UUID)}), jamais physique
 * (prompt maitre section 26) - {@code AssetRepository} n'expose d'ailleurs
 * aucune methode delete pour cette entite.
 */
@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetStatusHistoryRepository assetStatusHistoryRepository;
    private final AssetAssignmentRepository assetAssignmentRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    private final SiteRepository siteRepository;
    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final ZoneRepository zoneRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final AssetMapper assetMapper;
    private final AssetStatusHistoryMapper assetStatusHistoryMapper;
    private final AssetAssignmentMapper assetAssignmentMapper;
    private final AssetCodeGenerator assetCodeGenerator;
    private final AuditRecorder auditRecorder;

    public AssetService(
            AssetRepository assetRepository,
            AssetStatusHistoryRepository assetStatusHistoryRepository,
            AssetAssignmentRepository assetAssignmentRepository,
            AssetCategoryRepository assetCategoryRepository,
            SiteRepository siteRepository,
            BuildingRepository buildingRepository,
            FloorRepository floorRepository,
            ZoneRepository zoneRepository,
            LocationRepository locationRepository,
            UserRepository userRepository,
            AssetMapper assetMapper,
            AssetStatusHistoryMapper assetStatusHistoryMapper,
            AssetAssignmentMapper assetAssignmentMapper,
            AssetCodeGenerator assetCodeGenerator,
            AuditRecorder auditRecorder
    ) {
        this.assetRepository = assetRepository;
        this.assetStatusHistoryRepository = assetStatusHistoryRepository;
        this.assetAssignmentRepository = assetAssignmentRepository;
        this.assetCategoryRepository = assetCategoryRepository;
        this.siteRepository = siteRepository;
        this.buildingRepository = buildingRepository;
        this.floorRepository = floorRepository;
        this.zoneRepository = zoneRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.assetMapper = assetMapper;
        this.assetStatusHistoryMapper = assetStatusHistoryMapper;
        this.assetAssignmentMapper = assetAssignmentMapper;
        this.assetCodeGenerator = assetCodeGenerator;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public PageResponse<AssetDto> list(
            Pageable pageable,
            boolean includeDeleted,
            UUID siteId,
            UUID categoryId,
            AssetCondition condition,
            AssetStatus status,
            Boolean labeled,
            String search
    ) {
        var specification = AssetSpecification.withFilters(includeDeleted, siteId, categoryId, condition, status, labeled, search);
        return PageResponse.from(assetRepository.findAll(specification, pageable).map(assetMapper::toDto));
    }

    @Transactional(readOnly = true)
    public AssetDto findById(UUID id) {
        return assetMapper.toDto(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<AssetStatusHistoryDto> statusHistory(UUID id) {
        getOrThrow(id);
        return assetStatusHistoryRepository.findByAssetIdOrderByChangedAtDesc(id).stream()
                .map(assetStatusHistoryMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssetAssignmentDto> assignments(UUID id) {
        getOrThrow(id);
        return assetAssignmentRepository.findByAssetIdOrderByAssignedFromDesc(id).stream()
                .map(assetAssignmentMapper::toDto)
                .toList();
    }

    @Transactional
    public AssetDto create(AssetCreateRequest request) {
        checkSerialNumberAvailable(request.serialNumber(), null);

        Asset asset = new Asset();
        asset.setAssetCode(assetCodeGenerator.next());
        asset.setDesignation(request.designation().trim());
        asset.setCategory(resolveCategory(request.categoryId()));
        asset.setBrand(request.brand());
        asset.setModel(request.model());
        asset.setSerialNumber(blankToNull(request.serialNumber()));

        LocationChain location = resolveLocationChain(
                request.siteId(), request.buildingId(), request.floorId(), request.zoneId(), request.locationId());
        asset.setSite(location.site());
        asset.setBuilding(location.building());
        asset.setFloor(location.floor());
        asset.setZone(location.zone());
        asset.setLocation(location.location());

        asset.setDirection(request.direction());
        asset.setDepartment(request.department());
        asset.setService(request.service());
        User currentUser = resolveUser(request.currentUserId());
        asset.setCurrentUser(currentUser);
        asset.setResponsibleUser(resolveUser(request.responsibleUserId()));

        asset.setAcquisitionDate(request.acquisitionDate());
        asset.setSupplier(request.supplier());
        asset.setInvoiceNumber(request.invoiceNumber());
        asset.setAcquisitionValue(request.acquisitionValue());
        asset.setCommissioningDate(request.commissioningDate());
        asset.setWarrantyUntil(request.warrantyUntil());

        asset.setCondition(request.condition() != null ? request.condition() : AssetCondition.NEUF);
        asset.setComment(request.comment());

        asset = assetRepository.save(asset);

        if (currentUser != null || hasText(request.direction()) || hasText(request.department()) || hasText(request.service())) {
            openAssignment(asset, currentUser, request.direction(), request.department(), request.service(), null);
        }

        auditRecorder.record(AuditAction.CREATION, "IMMOBILISATION", "assets", asset.getId(), null, assetMapper.toDto(asset));
        return assetMapper.toDto(asset);
    }

    @Transactional
    public AssetDto update(UUID id, AssetUpdateRequest request) {
        Asset asset = getOrThrow(id);
        if (asset.isDeleted()) {
            throw new BusinessRuleException("Impossible de modifier une immobilisation archivee.");
        }
        checkSerialNumberAvailable(request.serialNumber(), id);

        AssetDto before = assetMapper.toDto(asset);

        AssetCondition previousCondition = asset.getCondition();
        AssetStatus previousStatus = asset.getStatus();
        UUID previousUserId = asset.getCurrentUser() != null ? asset.getCurrentUser().getId() : null;
        String previousDirection = asset.getDirection();
        String previousDepartment = asset.getDepartment();
        String previousService = asset.getService();

        asset.setDesignation(request.designation().trim());
        asset.setCategory(resolveCategory(request.categoryId()));
        asset.setBrand(request.brand());
        asset.setModel(request.model());
        asset.setSerialNumber(blankToNull(request.serialNumber()));

        LocationChain location = resolveLocationChain(
                request.siteId(), request.buildingId(), request.floorId(), request.zoneId(), request.locationId());
        asset.setSite(location.site());
        asset.setBuilding(location.building());
        asset.setFloor(location.floor());
        asset.setZone(location.zone());
        asset.setLocation(location.location());

        asset.setAcquisitionDate(request.acquisitionDate());
        asset.setSupplier(request.supplier());
        asset.setInvoiceNumber(request.invoiceNumber());
        asset.setAcquisitionValue(request.acquisitionValue());
        asset.setCommissioningDate(request.commissioningDate());
        asset.setWarrantyUntil(request.warrantyUntil());
        asset.setComment(request.comment());

        User newCurrentUser = resolveUser(request.currentUserId());
        asset.setDirection(request.direction());
        asset.setDepartment(request.department());
        asset.setService(request.service());
        asset.setCurrentUser(newCurrentUser);
        asset.setResponsibleUser(resolveUser(request.responsibleUserId()));

        asset.setCondition(request.condition());
        asset.setStatus(request.status());

        User actor = currentUser();

        if (request.condition() != previousCondition) {
            recordStatusChange(asset, StatusHistoryField.CONDITION, previousCondition.name(), request.condition().name(), actor, request.changeComment());
        }
        if (request.status() != previousStatus) {
            recordStatusChange(asset, StatusHistoryField.STATUS, previousStatus.name(), request.status().name(), actor, request.changeComment());
        }

        UUID newUserId = newCurrentUser != null ? newCurrentUser.getId() : null;
        boolean assignmentChanged = !Objects.equals(previousUserId, newUserId)
                || !Objects.equals(blankToNull(previousDirection), blankToNull(request.direction()))
                || !Objects.equals(blankToNull(previousDepartment), blankToNull(request.department()))
                || !Objects.equals(blankToNull(previousService), blankToNull(request.service()));
        if (assignmentChanged) {
            closeCurrentAssignment(asset.getId());
            if (newCurrentUser != null || hasText(request.direction()) || hasText(request.department()) || hasText(request.service())) {
                openAssignment(asset, newCurrentUser, request.direction(), request.department(), request.service(), request.changeComment());
            }
        }

        auditRecorder.record(AuditAction.MODIFICATION, "IMMOBILISATION", "assets", asset.getId(), before, assetMapper.toDto(asset));
        return assetMapper.toDto(asset);
    }

    @Transactional
    public AssetDto archive(UUID id) {
        Asset asset = getOrThrow(id);
        if (asset.isDeleted()) {
            throw new BusinessRuleException("Cette immobilisation est deja archivee.");
        }
        AssetDto before = assetMapper.toDto(asset);
        asset.setDeleted(true);
        asset.setDeletedAt(Instant.now());
        auditRecorder.record(AuditAction.SUPPRESSION_LOGIQUE, "IMMOBILISATION", "assets", asset.getId(), before, assetMapper.toDto(asset));
        return assetMapper.toDto(asset);
    }

    // --- Helpers ---------------------------------------------------

    private Asset getOrThrow(UUID id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Immobilisation introuvable"));
    }

    private AssetCategory resolveCategory(UUID categoryId) {
        return assetCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessRuleException("Categorie introuvable"));
    }

    private User resolveUser(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("Utilisateur introuvable"));
    }

    /**
     * Resout toute la chaine de localisation et verifie sa coherence
     * (un etage doit appartenir au batiment fourni, etc.) plutot que de se
     * fier aveuglement a des identifiants independants venus du frontend.
     */
    private LocationChain resolveLocationChain(UUID siteId, UUID buildingId, UUID floorId, UUID zoneId, UUID locationId) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new BusinessRuleException("Site introuvable"));

        Building building = null;
        if (buildingId != null) {
            building = buildingRepository.findById(buildingId)
                    .orElseThrow(() -> new BusinessRuleException("Batiment introuvable"));
            if (!building.getSite().getId().equals(siteId)) {
                throw new BusinessRuleException("Le batiment selectionne n'appartient pas au site selectionne");
            }
        }

        Floor floor = null;
        if (floorId != null) {
            if (building == null) {
                throw new BusinessRuleException("Un etage ne peut etre selectionne sans batiment");
            }
            floor = floorRepository.findById(floorId)
                    .orElseThrow(() -> new BusinessRuleException("Etage introuvable"));
            if (!floor.getBuilding().getId().equals(buildingId)) {
                throw new BusinessRuleException("L'etage selectionne n'appartient pas au batiment selectionne");
            }
        }

        Zone zone = null;
        if (zoneId != null) {
            if (floor == null) {
                throw new BusinessRuleException("Une zone ne peut etre selectionnee sans etage");
            }
            zone = zoneRepository.findById(zoneId)
                    .orElseThrow(() -> new BusinessRuleException("Zone introuvable"));
            if (!zone.getFloor().getId().equals(floorId)) {
                throw new BusinessRuleException("La zone selectionnee n'appartient pas a l'etage selectionne");
            }
        }

        Location loc = null;
        if (locationId != null) {
            if (zone == null) {
                throw new BusinessRuleException("Un local ne peut etre selectionne sans zone");
            }
            loc = locationRepository.findById(locationId)
                    .orElseThrow(() -> new BusinessRuleException("Local introuvable"));
            if (!loc.getZone().getId().equals(zoneId)) {
                throw new BusinessRuleException("Le local selectionne n'appartient pas a la zone selectionnee");
            }
        }

        return new LocationChain(site, building, floor, zone, loc);
    }

    private void checkSerialNumberAvailable(String serialNumber, UUID excludingAssetId) {
        String value = blankToNull(serialNumber);
        if (value == null) {
            return;
        }
        Optional<Asset> existing = assetRepository.findBySerialNumber(value);
        if (existing.isPresent() && !existing.get().getId().equals(excludingAssetId)) {
            throw new BusinessRuleException("Une immobilisation avec le numero de serie '" + value + "' existe deja");
        }
    }

    private void recordStatusChange(Asset asset, StatusHistoryField field, String oldValue, String newValue, User actor, String comment) {
        AssetStatusHistory history = new AssetStatusHistory();
        history.setAsset(asset);
        history.setFieldName(field);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setChangedBy(actor);
        history.setChangedAt(Instant.now());
        history.setComment(comment);
        assetStatusHistoryRepository.save(history);
    }

    /**
     * Closes the current assignment (if any) and flushes immediately.
     * Hibernate's default flush order runs INSERTs before UPDATEs, which
     * would otherwise let the new (open) row and the not-yet-closed old
     * row briefly coexist in the same flush - violating the partial
     * unique index {@code uq_asset_assignments_current} (at most one row
     * per asset with {@code assigned_until IS NULL}). Forcing this UPDATE
     * to hit the database before {@link #openAssignment} issues its
     * INSERT keeps that invariant true at every point in the transaction.
     */
    private void closeCurrentAssignment(UUID assetId) {
        assetAssignmentRepository.findByAssetIdAndAssignedUntilIsNull(assetId)
                .ifPresent(assignment -> {
                    assignment.setAssignedUntil(Instant.now());
                    assetAssignmentRepository.saveAndFlush(assignment);
                });
    }

    private void openAssignment(Asset asset, User user, String direction, String department, String service, String comment) {
        AssetAssignment assignment = new AssetAssignment();
        assignment.setAsset(asset);
        assignment.setUser(user);
        assignment.setDirection(direction);
        assignment.setDepartment(department);
        assignment.setService(service);
        assignment.setAssignedFrom(Instant.now());
        assignment.setAssignedBy(currentUser());
        assignment.setComment(comment);
        assetAssignmentRepository.save(assignment);
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

    private record LocationChain(Site site, Building building, Floor floor, Zone zone, Location location) {
    }
}
