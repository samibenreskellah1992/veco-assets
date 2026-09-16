package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.FloorDto;
import dz.vecopharm.vecoassets.dto.FloorRequest;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.entity.Building;
import dz.vecopharm.vecoassets.entity.Floor;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.mapper.FloorMapper;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import dz.vecopharm.vecoassets.repository.BuildingRepository;
import dz.vecopharm.vecoassets.repository.FloorRepository;
import dz.vecopharm.vecoassets.repository.ZoneRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Niveau 3 de la hierarchie de localisation (Site > Batiment > Etage > ...), prompt maitre Phase 4. */
@Service
public class FloorService {

    private final FloorRepository floorRepository;
    private final BuildingRepository buildingRepository;
    private final ZoneRepository zoneRepository;
    private final AssetRepository assetRepository;
    private final FloorMapper floorMapper;
    private final AuditRecorder auditRecorder;

    public FloorService(
            FloorRepository floorRepository,
            BuildingRepository buildingRepository,
            ZoneRepository zoneRepository,
            AssetRepository assetRepository,
            FloorMapper floorMapper,
            AuditRecorder auditRecorder
    ) {
        this.floorRepository = floorRepository;
        this.buildingRepository = buildingRepository;
        this.zoneRepository = zoneRepository;
        this.assetRepository = assetRepository;
        this.floorMapper = floorMapper;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<FloorDto> findAll(UUID buildingId) {
        List<Floor> floors = buildingId != null
                ? floorRepository.findByBuildingId(buildingId)
                : floorRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        return floors.stream().map(floorMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public FloorDto findById(UUID id) {
        return floorMapper.toDto(getOrThrow(id));
    }

    @Transactional
    public FloorDto create(FloorRequest request) {
        Building building = buildingRepository.findById(request.buildingId())
                .orElseThrow(() -> new ResourceNotFoundException("Batiment introuvable"));
        String code = request.code().trim();
        if (floorRepository.existsByBuildingIdAndCodeIgnoreCase(building.getId(), code)) {
            throw new BusinessRuleException("Un etage avec le code '" + code + "' existe deja dans ce batiment");
        }
        Floor floor = new Floor();
        floor.setBuilding(building);
        floor.setCode(code);
        floor.setName(request.name().trim());
        floor.setActive(true);
        floor = floorRepository.save(floor);
        auditRecorder.record(AuditAction.CREATION, "REFERENTIEL", "floors", floor.getId(), null, floorMapper.toDto(floor));
        return floorMapper.toDto(floor);
    }

    @Transactional
    public FloorDto update(UUID id, FloorRequest request) {
        Floor floor = getOrThrow(id);
        Building building = request.buildingId().equals(floor.getBuilding().getId())
                ? floor.getBuilding()
                : buildingRepository.findById(request.buildingId()).orElseThrow(() -> new ResourceNotFoundException("Batiment introuvable"));
        String code = request.code().trim();
        if (floorRepository.existsByBuildingIdAndCodeIgnoreCaseAndIdNot(building.getId(), code, id)) {
            throw new BusinessRuleException("Un etage avec le code '" + code + "' existe deja dans ce batiment");
        }
        FloorDto before = floorMapper.toDto(floor);
        floor.setBuilding(building);
        floor.setCode(code);
        floor.setName(request.name().trim());
        auditRecorder.record(AuditAction.MODIFICATION, "REFERENTIEL", "floors", floor.getId(), before, floorMapper.toDto(floor));
        return floorMapper.toDto(floor);
    }

    @Transactional
    public FloorDto setActive(UUID id, boolean active) {
        Floor floor = getOrThrow(id);
        if (floor.isActive() == active) {
            return floorMapper.toDto(floor);
        }
        FloorDto before = floorMapper.toDto(floor);
        floor.setActive(active);
        auditRecorder.record(AuditAction.CHANGEMENT_STATUT, "REFERENTIEL", "floors", floor.getId(), before, floorMapper.toDto(floor));
        return floorMapper.toDto(floor);
    }

    @Transactional
    public void delete(UUID id) {
        Floor floor = getOrThrow(id);
        if (zoneRepository.existsByFloorId(id)) {
            throw new BusinessRuleException("Impossible de supprimer cet etage : des zones y sont rattachees. Desactivez-le plutot.");
        }
        if (assetRepository.existsByFloorId(id)) {
            throw new BusinessRuleException("Impossible de supprimer cet etage : des immobilisations y sont rattachees. Desactivez-le plutot.");
        }
        FloorDto before = floorMapper.toDto(floor);
        floorRepository.delete(floor);
        auditRecorder.record(AuditAction.SUPPRESSION, "REFERENTIEL", "floors", id, before, null);
    }

    private Floor getOrThrow(UUID id) {
        return floorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Etage introuvable"));
    }
}
