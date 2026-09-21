package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.LocationDto;
import dz.vecopharm.vecoassets.dto.LocationRequest;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.entity.Location;
import dz.vecopharm.vecoassets.entity.LocationStatus;
import dz.vecopharm.vecoassets.entity.Site;
import dz.vecopharm.vecoassets.entity.User;
import dz.vecopharm.vecoassets.entity.Zone;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.mapper.LocationMapper;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import dz.vecopharm.vecoassets.repository.LocationRepository;
import dz.vecopharm.vecoassets.repository.UserRepository;
import dz.vecopharm.vecoassets.repository.ZoneRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Feuille de la hierarchie de localisation (Site > Batiment > Etage > Zone > Localisation), prompt maitre Phase 4.
 *
 * <p>Checkpoint 1 de l'evolution "locaux scannables" (2026-09) : chaque
 * local recoit desormais a la creation un code QR unique, site-scope
 * ({@link LocationCodeGenerator}), jamais modifiable ensuite ; le CRUD
 * existant (code/nom/zone/active) n'est pas touche.</p>
 */
@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final ZoneRepository zoneRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final LocationMapper locationMapper;
    private final LocationCodeGenerator locationCodeGenerator;
    private final AuditRecorder auditRecorder;

    public LocationService(
            LocationRepository locationRepository,
            ZoneRepository zoneRepository,
            AssetRepository assetRepository,
            UserRepository userRepository,
            LocationMapper locationMapper,
            LocationCodeGenerator locationCodeGenerator,
            AuditRecorder auditRecorder
    ) {
        this.locationRepository = locationRepository;
        this.zoneRepository = zoneRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.locationMapper = locationMapper;
        this.locationCodeGenerator = locationCodeGenerator;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<LocationDto> findAll(UUID zoneId) {
        List<Location> locations = zoneId != null
                ? locationRepository.findByZoneId(zoneId)
                : locationRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        return withAssetCounts(locations);
    }

    @Transactional(readOnly = true)
    public LocationDto findById(UUID id) {
        Location location = getOrThrow(id);
        return withCount(locationMapper.toDto(location), countFor(id));
    }

    @Transactional(readOnly = true)
    public LocationDto findByQrCode(String qrCode) {
        Location location = locationRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new ResourceNotFoundException("Local introuvable pour ce code QR"));
        return withCount(locationMapper.toDto(location), countFor(location.getId()));
    }

    @Transactional
    public LocationDto create(LocationRequest request) {
        Zone zone = zoneRepository.findById(request.zoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone introuvable"));
        String code = request.code().trim();
        if (locationRepository.existsByZoneIdAndCodeIgnoreCase(zone.getId(), code)) {
            throw new BusinessRuleException("Une localisation avec le code '" + code + "' existe deja dans cette zone");
        }
        Site site = zone.getFloor().getBuilding().getSite();

        Location location = new Location();
        location.setZone(zone);
        location.setCode(code);
        location.setName(request.name().trim());
        location.setActive(true);
        location.setStatus(request.status() != null ? request.status() : LocationStatus.ACTIF);
        location.setDescription(request.description() != null ? request.description().trim() : null);
        location.setResponsibleUser(resolveResponsible(request.responsibleUserId()));
        location.setQrCode(locationCodeGenerator.next(site.getId(), site.getCode()));
        location = locationRepository.save(location);
        auditRecorder.record(AuditAction.CREATION, "REFERENTIEL", "locations", location.getId(), null, locationMapper.toDto(location));
        return withCount(locationMapper.toDto(location), 0L);
    }

    @Transactional
    public LocationDto update(UUID id, LocationRequest request) {
        Location location = getOrThrow(id);
        Zone zone = request.zoneId().equals(location.getZone().getId())
                ? location.getZone()
                : zoneRepository.findById(request.zoneId()).orElseThrow(() -> new ResourceNotFoundException("Zone introuvable"));
        String code = request.code().trim();
        if (locationRepository.existsByZoneIdAndCodeIgnoreCaseAndIdNot(zone.getId(), code, id)) {
            throw new BusinessRuleException("Une localisation avec le code '" + code + "' existe deja dans cette zone");
        }
        LocationDto before = locationMapper.toDto(location);
        location.setZone(zone);
        location.setCode(code);
        location.setName(request.name().trim());
        location.setStatus(request.status() != null ? request.status() : location.getStatus());
        location.setDescription(request.description() != null ? request.description().trim() : null);
        location.setResponsibleUser(resolveResponsible(request.responsibleUserId()));
        // qrCode volontairement jamais reecrit ici : immuable apres creation.
        auditRecorder.record(AuditAction.MODIFICATION, "REFERENTIEL", "locations", location.getId(), before, locationMapper.toDto(location));
        return withCount(locationMapper.toDto(location), countFor(id));
    }

    @Transactional
    public LocationDto setActive(UUID id, boolean active) {
        Location location = getOrThrow(id);
        if (location.isActive() == active) {
            return withCount(locationMapper.toDto(location), countFor(id));
        }
        LocationDto before = locationMapper.toDto(location);
        location.setActive(active);
        auditRecorder.record(AuditAction.CHANGEMENT_STATUT, "REFERENTIEL", "locations", location.getId(), before, locationMapper.toDto(location));
        return withCount(locationMapper.toDto(location), countFor(id));
    }

    @Transactional
    public void delete(UUID id) {
        Location location = getOrThrow(id);
        if (assetRepository.existsByLocationId(id)) {
            throw new BusinessRuleException("Impossible de supprimer cette localisation : des immobilisations y sont rattachees. Desactivez-la plutot.");
        }
        LocationDto before = locationMapper.toDto(location);
        locationRepository.delete(location);
        auditRecorder.record(AuditAction.SUPPRESSION, "REFERENTIEL", "locations", id, before, null);
    }

    private User resolveResponsible(UUID responsibleUserId) {
        if (responsibleUserId == null) {
            return null;
        }
        return userRepository.findById(responsibleUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur responsable introuvable"));
    }

    private Location getOrThrow(UUID id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Localisation introuvable"));
    }

    private long countFor(UUID id) {
        return batchCounts(List.of(id)).getOrDefault(id, 0L);
    }

    private Map<UUID, Long> batchCounts(List<UUID> ids) {
        Map<UUID, Long> counts = new HashMap<>();
        if (ids.isEmpty()) {
            return counts;
        }
        for (Object[] row : locationRepository.countAssetsByLocationIds(ids)) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    private List<LocationDto> withAssetCounts(List<Location> locations) {
        List<UUID> ids = locations.stream().map(Location::getId).toList();
        Map<UUID, Long> counts = batchCounts(ids);
        return locations.stream()
                .map(location -> withCount(locationMapper.toDto(location), counts.getOrDefault(location.getId(), 0L)))
                .toList();
    }

    private LocationDto withCount(LocationDto dto, long assetCount) {
        return new LocationDto(
                dto.id(), dto.zoneId(), dto.zoneName(), dto.code(), dto.name(), dto.active(),
                dto.status(), dto.qrCode(), dto.description(), dto.responsibleUserId(), dto.responsibleUserName(),
                dto.lastInventoryAt(), assetCount,
                dto.siteId(), dto.siteName(), dto.buildingId(), dto.buildingName(), dto.floorId(), dto.floorName(),
                dto.labeled()
        );
    }
}
