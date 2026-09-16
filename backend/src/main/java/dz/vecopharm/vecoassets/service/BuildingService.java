package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.BuildingDto;
import dz.vecopharm.vecoassets.dto.BuildingRequest;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.entity.Building;
import dz.vecopharm.vecoassets.entity.Site;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.mapper.BuildingMapper;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import dz.vecopharm.vecoassets.repository.BuildingRepository;
import dz.vecopharm.vecoassets.repository.FloorRepository;
import dz.vecopharm.vecoassets.repository.SiteRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Niveau 2 de la hierarchie de localisation (Site > Batiment > ...), prompt maitre Phase 4. */
@Service
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final SiteRepository siteRepository;
    private final FloorRepository floorRepository;
    private final AssetRepository assetRepository;
    private final BuildingMapper buildingMapper;
    private final AuditRecorder auditRecorder;

    public BuildingService(
            BuildingRepository buildingRepository,
            SiteRepository siteRepository,
            FloorRepository floorRepository,
            AssetRepository assetRepository,
            BuildingMapper buildingMapper,
            AuditRecorder auditRecorder
    ) {
        this.buildingRepository = buildingRepository;
        this.siteRepository = siteRepository;
        this.floorRepository = floorRepository;
        this.assetRepository = assetRepository;
        this.buildingMapper = buildingMapper;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<BuildingDto> findAll(UUID siteId) {
        List<Building> buildings = siteId != null
                ? buildingRepository.findBySiteId(siteId)
                : buildingRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        return buildings.stream().map(buildingMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BuildingDto findById(UUID id) {
        return buildingMapper.toDto(getOrThrow(id));
    }

    @Transactional
    public BuildingDto create(BuildingRequest request) {
        Site site = siteRepository.findById(request.siteId())
                .orElseThrow(() -> new ResourceNotFoundException("Site introuvable"));
        String code = request.code().trim();
        if (buildingRepository.existsBySiteIdAndCodeIgnoreCase(site.getId(), code)) {
            throw new BusinessRuleException("Un batiment avec le code '" + code + "' existe deja sur ce site");
        }
        Building building = new Building();
        building.setSite(site);
        building.setCode(code);
        building.setName(request.name().trim());
        building.setActive(true);
        building = buildingRepository.save(building);
        auditRecorder.record(AuditAction.CREATION, "REFERENTIEL", "buildings", building.getId(), null, buildingMapper.toDto(building));
        return buildingMapper.toDto(building);
    }

    @Transactional
    public BuildingDto update(UUID id, BuildingRequest request) {
        Building building = getOrThrow(id);
        Site site = request.siteId().equals(building.getSite().getId())
                ? building.getSite()
                : siteRepository.findById(request.siteId()).orElseThrow(() -> new ResourceNotFoundException("Site introuvable"));
        String code = request.code().trim();
        if (buildingRepository.existsBySiteIdAndCodeIgnoreCaseAndIdNot(site.getId(), code, id)) {
            throw new BusinessRuleException("Un batiment avec le code '" + code + "' existe deja sur ce site");
        }
        BuildingDto before = buildingMapper.toDto(building);
        building.setSite(site);
        building.setCode(code);
        building.setName(request.name().trim());
        auditRecorder.record(AuditAction.MODIFICATION, "REFERENTIEL", "buildings", building.getId(), before, buildingMapper.toDto(building));
        return buildingMapper.toDto(building);
    }

    @Transactional
    public BuildingDto setActive(UUID id, boolean active) {
        Building building = getOrThrow(id);
        if (building.isActive() == active) {
            return buildingMapper.toDto(building);
        }
        BuildingDto before = buildingMapper.toDto(building);
        building.setActive(active);
        auditRecorder.record(AuditAction.CHANGEMENT_STATUT, "REFERENTIEL", "buildings", building.getId(), before, buildingMapper.toDto(building));
        return buildingMapper.toDto(building);
    }

    @Transactional
    public void delete(UUID id) {
        Building building = getOrThrow(id);
        if (floorRepository.existsByBuildingId(id)) {
            throw new BusinessRuleException("Impossible de supprimer ce batiment : des etages y sont rattaches. Desactivez-le plutot.");
        }
        if (assetRepository.existsByBuildingId(id)) {
            throw new BusinessRuleException("Impossible de supprimer ce batiment : des immobilisations y sont rattachees. Desactivez-le plutot.");
        }
        BuildingDto before = buildingMapper.toDto(building);
        buildingRepository.delete(building);
        auditRecorder.record(AuditAction.SUPPRESSION, "REFERENTIEL", "buildings", id, before, null);
    }

    private Building getOrThrow(UUID id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batiment introuvable"));
    }
}
