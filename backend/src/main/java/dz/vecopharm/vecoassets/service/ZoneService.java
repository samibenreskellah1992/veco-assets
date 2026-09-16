package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.ZoneDto;
import dz.vecopharm.vecoassets.dto.ZoneRequest;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.entity.Floor;
import dz.vecopharm.vecoassets.entity.Zone;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.mapper.ZoneMapper;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import dz.vecopharm.vecoassets.repository.FloorRepository;
import dz.vecopharm.vecoassets.repository.LocationRepository;
import dz.vecopharm.vecoassets.repository.ZoneRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Niveau 4 de la hierarchie de localisation (Site > Batiment > Etage > Zone > ...), prompt maitre Phase 4. */
@Service
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final FloorRepository floorRepository;
    private final LocationRepository locationRepository;
    private final AssetRepository assetRepository;
    private final ZoneMapper zoneMapper;
    private final AuditRecorder auditRecorder;

    public ZoneService(
            ZoneRepository zoneRepository,
            FloorRepository floorRepository,
            LocationRepository locationRepository,
            AssetRepository assetRepository,
            ZoneMapper zoneMapper,
            AuditRecorder auditRecorder
    ) {
        this.zoneRepository = zoneRepository;
        this.floorRepository = floorRepository;
        this.locationRepository = locationRepository;
        this.assetRepository = assetRepository;
        this.zoneMapper = zoneMapper;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<ZoneDto> findAll(UUID floorId) {
        List<Zone> zones = floorId != null
                ? zoneRepository.findByFloorId(floorId)
                : zoneRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        return zones.stream().map(zoneMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ZoneDto findById(UUID id) {
        return zoneMapper.toDto(getOrThrow(id));
    }

    @Transactional
    public ZoneDto create(ZoneRequest request) {
        Floor floor = floorRepository.findById(request.floorId())
                .orElseThrow(() -> new ResourceNotFoundException("Etage introuvable"));
        String code = request.code().trim();
        if (zoneRepository.existsByFloorIdAndCodeIgnoreCase(floor.getId(), code)) {
            throw new BusinessRuleException("Une zone avec le code '" + code + "' existe deja dans cet etage");
        }
        Zone zone = new Zone();
        zone.setFloor(floor);
        zone.setCode(code);
        zone.setName(request.name().trim());
        zone.setActive(true);
        zone = zoneRepository.save(zone);
        auditRecorder.record(AuditAction.CREATION, "REFERENTIEL", "zones", zone.getId(), null, zoneMapper.toDto(zone));
        return zoneMapper.toDto(zone);
    }

    @Transactional
    public ZoneDto update(UUID id, ZoneRequest request) {
        Zone zone = getOrThrow(id);
        Floor floor = request.floorId().equals(zone.getFloor().getId())
                ? zone.getFloor()
                : floorRepository.findById(request.floorId()).orElseThrow(() -> new ResourceNotFoundException("Etage introuvable"));
        String code = request.code().trim();
        if (zoneRepository.existsByFloorIdAndCodeIgnoreCaseAndIdNot(floor.getId(), code, id)) {
            throw new BusinessRuleException("Une zone avec le code '" + code + "' existe deja dans cet etage");
        }
        ZoneDto before = zoneMapper.toDto(zone);
        zone.setFloor(floor);
        zone.setCode(code);
        zone.setName(request.name().trim());
        auditRecorder.record(AuditAction.MODIFICATION, "REFERENTIEL", "zones", zone.getId(), before, zoneMapper.toDto(zone));
        return zoneMapper.toDto(zone);
    }

    @Transactional
    public ZoneDto setActive(UUID id, boolean active) {
        Zone zone = getOrThrow(id);
        if (zone.isActive() == active) {
            return zoneMapper.toDto(zone);
        }
        ZoneDto before = zoneMapper.toDto(zone);
        zone.setActive(active);
        auditRecorder.record(AuditAction.CHANGEMENT_STATUT, "REFERENTIEL", "zones", zone.getId(), before, zoneMapper.toDto(zone));
        return zoneMapper.toDto(zone);
    }

    @Transactional
    public void delete(UUID id) {
        Zone zone = getOrThrow(id);
        if (locationRepository.existsByZoneId(id)) {
            throw new BusinessRuleException("Impossible de supprimer cette zone : des localisations y sont rattachees. Desactivez-la plutot.");
        }
        if (assetRepository.existsByZoneId(id)) {
            throw new BusinessRuleException("Impossible de supprimer cette zone : des immobilisations y sont rattachees. Desactivez-la plutot.");
        }
        ZoneDto before = zoneMapper.toDto(zone);
        zoneRepository.delete(zone);
        auditRecorder.record(AuditAction.SUPPRESSION, "REFERENTIEL", "zones", id, before, null);
    }

    private Zone getOrThrow(UUID id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone introuvable"));
    }
}
