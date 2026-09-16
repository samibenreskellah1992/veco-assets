package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.LocationDto;
import dz.vecopharm.vecoassets.dto.LocationRequest;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.entity.Location;
import dz.vecopharm.vecoassets.entity.Zone;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.mapper.LocationMapper;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import dz.vecopharm.vecoassets.repository.LocationRepository;
import dz.vecopharm.vecoassets.repository.ZoneRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Feuille de la hierarchie de localisation (Site > Batiment > Etage > Zone > Localisation), prompt maitre Phase 4. */
@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final ZoneRepository zoneRepository;
    private final AssetRepository assetRepository;
    private final LocationMapper locationMapper;
    private final AuditRecorder auditRecorder;

    public LocationService(
            LocationRepository locationRepository,
            ZoneRepository zoneRepository,
            AssetRepository assetRepository,
            LocationMapper locationMapper,
            AuditRecorder auditRecorder
    ) {
        this.locationRepository = locationRepository;
        this.zoneRepository = zoneRepository;
        this.assetRepository = assetRepository;
        this.locationMapper = locationMapper;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<LocationDto> findAll(UUID zoneId) {
        List<Location> locations = zoneId != null
                ? locationRepository.findByZoneId(zoneId)
                : locationRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        return locations.stream().map(locationMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public LocationDto findById(UUID id) {
        return locationMapper.toDto(getOrThrow(id));
    }

    @Transactional
    public LocationDto create(LocationRequest request) {
        Zone zone = zoneRepository.findById(request.zoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone introuvable"));
        String code = request.code().trim();
        if (locationRepository.existsByZoneIdAndCodeIgnoreCase(zone.getId(), code)) {
            throw new BusinessRuleException("Une localisation avec le code '" + code + "' existe deja dans cette zone");
        }
        Location location = new Location();
        location.setZone(zone);
        location.setCode(code);
        location.setName(request.name().trim());
        location.setActive(true);
        location = locationRepository.save(location);
        auditRecorder.record(AuditAction.CREATION, "REFERENTIEL", "locations", location.getId(), null, locationMapper.toDto(location));
        return locationMapper.toDto(location);
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
        auditRecorder.record(AuditAction.MODIFICATION, "REFERENTIEL", "locations", location.getId(), before, locationMapper.toDto(location));
        return locationMapper.toDto(location);
    }

    @Transactional
    public LocationDto setActive(UUID id, boolean active) {
        Location location = getOrThrow(id);
        if (location.isActive() == active) {
            return locationMapper.toDto(location);
        }
        LocationDto before = locationMapper.toDto(location);
        location.setActive(active);
        auditRecorder.record(AuditAction.CHANGEMENT_STATUT, "REFERENTIEL", "locations", location.getId(), before, locationMapper.toDto(location));
        return locationMapper.toDto(location);
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

    private Location getOrThrow(UUID id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Localisation introuvable"));
    }
}
