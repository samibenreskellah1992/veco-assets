package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.AssetCategoryDto;
import dz.vecopharm.vecoassets.dto.AssetCategoryRequest;
import dz.vecopharm.vecoassets.entity.AssetCategory;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.mapper.AssetCategoryMapper;
import dz.vecopharm.vecoassets.repository.AssetCategoryRepository;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Categories et sous-categories d'immobilisation (prompt maitre Phase 4),
 * auto-reference via {@code parent}. Le code reste unique globalement (pas
 * seulement par parent - contrainte {@code UNIQUE} de V4), contrairement a
 * la hierarchie de localisation.
 */
@Service
public class AssetCategoryService {

    private static final int MAX_DEPTH = 50;

    private final AssetCategoryRepository categoryRepository;
    private final AssetRepository assetRepository;
    private final AssetCategoryMapper categoryMapper;
    private final AuditRecorder auditRecorder;

    public AssetCategoryService(
            AssetCategoryRepository categoryRepository,
            AssetRepository assetRepository,
            AssetCategoryMapper categoryMapper,
            AuditRecorder auditRecorder
    ) {
        this.categoryRepository = categoryRepository;
        this.assetRepository = assetRepository;
        this.categoryMapper = categoryMapper;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public java.util.List<AssetCategoryDto> findAll(UUID parentId) {
        java.util.List<AssetCategory> categories = parentId != null
                ? categoryRepository.findByParentId(parentId)
                : categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        return categories.stream().map(categoryMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AssetCategoryDto findById(UUID id) {
        return categoryMapper.toDto(getOrThrow(id));
    }

    @Transactional
    public AssetCategoryDto create(AssetCategoryRequest request) {
        String code = request.code().trim();
        if (categoryRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessRuleException("Une categorie avec le code '" + code + "' existe deja");
        }
        AssetCategory category = new AssetCategory();
        category.setParent(resolveParent(request.parentId(), null));
        category.setCode(code);
        category.setName(request.name().trim());
        category.setActive(true);
        category = categoryRepository.save(category);
        auditRecorder.record(AuditAction.CREATION, "REFERENTIEL", "asset_categories", category.getId(), null, categoryMapper.toDto(category));
        return categoryMapper.toDto(category);
    }

    @Transactional
    public AssetCategoryDto update(UUID id, AssetCategoryRequest request) {
        AssetCategory category = getOrThrow(id);
        String code = request.code().trim();
        if (categoryRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new BusinessRuleException("Une categorie avec le code '" + code + "' existe deja");
        }
        AssetCategoryDto before = categoryMapper.toDto(category);
        category.setParent(resolveParent(request.parentId(), category));
        category.setCode(code);
        category.setName(request.name().trim());
        auditRecorder.record(AuditAction.MODIFICATION, "REFERENTIEL", "asset_categories", category.getId(), before, categoryMapper.toDto(category));
        return categoryMapper.toDto(category);
    }

    @Transactional
    public AssetCategoryDto setActive(UUID id, boolean active) {
        AssetCategory category = getOrThrow(id);
        if (category.isActive() == active) {
            return categoryMapper.toDto(category);
        }
        AssetCategoryDto before = categoryMapper.toDto(category);
        category.setActive(active);
        auditRecorder.record(AuditAction.CHANGEMENT_STATUT, "REFERENTIEL", "asset_categories", category.getId(), before, categoryMapper.toDto(category));
        return categoryMapper.toDto(category);
    }

    @Transactional
    public void delete(UUID id) {
        AssetCategory category = getOrThrow(id);
        if (categoryRepository.existsByParentId(id)) {
            throw new BusinessRuleException("Impossible de supprimer cette categorie : des sous-categories y sont rattachees. Desactivez-la plutot.");
        }
        if (assetRepository.existsByCategoryId(id)) {
            throw new BusinessRuleException("Impossible de supprimer cette categorie : des immobilisations y sont rattachees. Desactivez-la plutot.");
        }
        AssetCategoryDto before = categoryMapper.toDto(category);
        categoryRepository.delete(category);
        auditRecorder.record(AuditAction.SUPPRESSION, "REFERENTIEL", "asset_categories", id, before, null);
    }

    /**
     * Resout et valide le parent demande : doit exister, ne peut pas etre la
     * categorie elle-meme, et ne peut pas etre un de ses propres descendants
     * (ce qui creerait un cycle dans l'arbre).
     */
    private AssetCategory resolveParent(UUID parentId, AssetCategory self) {
        if (parentId == null) {
            return null;
        }
        if (self != null && parentId.equals(self.getId())) {
            throw new BusinessRuleException("Une categorie ne peut pas etre son propre parent");
        }
        AssetCategory parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Categorie parente introuvable"));

        if (self != null) {
            AssetCategory cursor = parent;
            int depth = 0;
            while (cursor != null) {
                if (cursor.getId().equals(self.getId())) {
                    throw new BusinessRuleException("Affectation parent invalide : creerait une boucle dans l'arbre des categories");
                }
                if (++depth > MAX_DEPTH) {
                    throw new BusinessRuleException("Hierarchie de categories trop profonde");
                }
                cursor = cursor.getParent();
            }
        }
        return parent;
    }

    private AssetCategory getOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categorie introuvable"));
    }
}
