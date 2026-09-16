package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.SiteDto;
import dz.vecopharm.vecoassets.dto.SiteRequest;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.entity.Site;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.mapper.SiteMapper;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import dz.vecopharm.vecoassets.repository.BuildingRepository;
import dz.vecopharm.vecoassets.repository.SiteRepository;
import dz.vecopharm.vecoassets.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Racine de la hierarchie de localisation (prompt maitre Phase 4 / section
 * 9). Suppression physique autorisee uniquement quand le site n'est
 * reference nulle part (aucun batiment, aucun utilisateur, aucune
 * immobilisation) - sinon on desactive ({@link #setActive}) plutot que de
 * casser l'historique. La contrainte FK {@code ON DELETE RESTRICT} en base
 * est le filet de securite final si ces verifications applicatives etaient
 * un jour contournees.
 */
@Service
public class SiteService {

    private final SiteRepository siteRepository;
    private final BuildingRepository buildingRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final SiteMapper siteMapper;
    private final AuditRecorder auditRecorder;

    public SiteService(
            SiteRepository siteRepository,
            BuildingRepository buildingRepository,
            UserRepository userRepository,
            AssetRepository assetRepository,
            SiteMapper siteMapper,
            AuditRecorder auditRecorder
    ) {
        this.siteRepository = siteRepository;
        this.buildingRepository = buildingRepository;
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
        this.siteMapper = siteMapper;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<SiteDto> findAll() {
        return siteRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(siteMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SiteDto findById(UUID id) {
        return siteMapper.toDto(getOrThrow(id));
    }

    @Transactional
    public SiteDto create(SiteRequest request) {
        String code = request.code().trim();
        if (siteRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessRuleException("Un site avec le code '" + code + "' existe deja");
        }
        Site site = new Site();
        site.setCode(code);
        site.setName(request.name().trim());
        site.setAddress(request.address());
        site.setCity(request.city());
        site.setActive(true);
        site = siteRepository.save(site);
        auditRecorder.record(AuditAction.CREATION, "REFERENTIEL", "sites", site.getId(), null, siteMapper.toDto(site));
        return siteMapper.toDto(site);
    }

    @Transactional
    public SiteDto update(UUID id, SiteRequest request) {
        Site site = getOrThrow(id);
        String code = request.code().trim();
        if (siteRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new BusinessRuleException("Un site avec le code '" + code + "' existe deja");
        }
        SiteDto before = siteMapper.toDto(site);
        site.setCode(code);
        site.setName(request.name().trim());
        site.setAddress(request.address());
        site.setCity(request.city());
        auditRecorder.record(AuditAction.MODIFICATION, "REFERENTIEL", "sites", site.getId(), before, siteMapper.toDto(site));
        return siteMapper.toDto(site);
    }

    @Transactional
    public SiteDto setActive(UUID id, boolean active) {
        Site site = getOrThrow(id);
        if (site.isActive() == active) {
            return siteMapper.toDto(site);
        }
        SiteDto before = siteMapper.toDto(site);
        site.setActive(active);
        auditRecorder.record(AuditAction.CHANGEMENT_STATUT, "REFERENTIEL", "sites", site.getId(), before, siteMapper.toDto(site));
        return siteMapper.toDto(site);
    }

    @Transactional
    public void delete(UUID id) {
        Site site = getOrThrow(id);
        if (buildingRepository.existsBySiteId(id)) {
            throw new BusinessRuleException("Impossible de supprimer ce site : des batiments y sont rattaches. Desactivez-le plutot.");
        }
        if (userRepository.existsBySiteId(id)) {
            throw new BusinessRuleException("Impossible de supprimer ce site : des utilisateurs y sont rattaches. Desactivez-le plutot.");
        }
        if (assetRepository.existsBySiteId(id)) {
            throw new BusinessRuleException("Impossible de supprimer ce site : des immobilisations y sont rattachees. Desactivez-le plutot.");
        }
        SiteDto before = siteMapper.toDto(site);
        siteRepository.delete(site);
        auditRecorder.record(AuditAction.SUPPRESSION, "REFERENTIEL", "sites", id, before, null);
    }

    private Site getOrThrow(UUID id) {
        return siteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Site introuvable"));
    }
}
