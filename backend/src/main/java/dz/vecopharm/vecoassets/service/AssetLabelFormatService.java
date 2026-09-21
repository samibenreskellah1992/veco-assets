package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.AssetLabelFormatDto;
import dz.vecopharm.vecoassets.dto.AssetLabelFormatRequest;
import dz.vecopharm.vecoassets.entity.AssetLabelFormat;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.mapper.AssetLabelFormatMapper;
import dz.vecopharm.vecoassets.repository.AssetLabelFormatRepository;
import dz.vecopharm.vecoassets.repository.AssetLabelRepository;
import dz.vecopharm.vecoassets.repository.LocationLabelRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Formats d'etiquette (prompt maitre section 13 - Phase 6) : dimensions et
 * contenu affiche administrables, jamais codes en dur cote frontend.
 * Suppression physique autorisee uniquement quand le format n'a encore
 * jamais ete utilise pour generer une etiquette (meme politique de
 * suppression que le referentiel en Phase 4) - sinon on desactive
 * ({@link #setActive}) pour ne jamais casser l'historique
 * {@code asset_labels} deja genere avec ce format.
 *
 * <p>Checkpoint 2 "locaux scannables" (2026-09) : ce meme format est
 * desormais aussi utilise pour les etiquettes de local ({@code
 * location_labels}, voir {@link dz.vecopharm.vecoassets.service.LocationLabelService})
 * - la suppression est donc bloquee si l'une OU l'autre table y fait
 * reference.</p>
 */
@Service
public class AssetLabelFormatService {

    private final AssetLabelFormatRepository formatRepository;
    private final AssetLabelRepository assetLabelRepository;
    private final LocationLabelRepository locationLabelRepository;
    private final AssetLabelFormatMapper formatMapper;
    private final AuditRecorder auditRecorder;

    public AssetLabelFormatService(
            AssetLabelFormatRepository formatRepository,
            AssetLabelRepository assetLabelRepository,
            LocationLabelRepository locationLabelRepository,
            AssetLabelFormatMapper formatMapper,
            AuditRecorder auditRecorder
    ) {
        this.formatRepository = formatRepository;
        this.assetLabelRepository = assetLabelRepository;
        this.locationLabelRepository = locationLabelRepository;
        this.formatMapper = formatMapper;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<AssetLabelFormatDto> findAll() {
        return formatRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(formatMapper::toDto)
                .toList();
    }

    @Transactional
    public AssetLabelFormatDto create(AssetLabelFormatRequest request) {
        String code = request.code().trim();
        if (formatRepository.findByCode(code).isPresent()) {
            throw new BusinessRuleException("Un format d'etiquette avec le code '" + code + "' existe deja");
        }
        AssetLabelFormat format = new AssetLabelFormat();
        format.setCode(code);
        applyRequest(format, request);
        format.setActive(true);
        format = formatRepository.save(format);
        auditRecorder.record(AuditAction.CREATION, "ETIQUETAGE", "asset_label_formats", format.getId(), null, formatMapper.toDto(format));
        return formatMapper.toDto(format);
    }

    @Transactional
    public AssetLabelFormatDto update(UUID id, AssetLabelFormatRequest request) {
        AssetLabelFormat format = getOrThrow(id);
        String code = request.code().trim();
        formatRepository.findByCode(code).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessRuleException("Un format d'etiquette avec le code '" + code + "' existe deja");
            }
        });
        AssetLabelFormatDto before = formatMapper.toDto(format);
        format.setCode(code);
        applyRequest(format, request);
        auditRecorder.record(AuditAction.MODIFICATION, "ETIQUETAGE", "asset_label_formats", format.getId(), before, formatMapper.toDto(format));
        return formatMapper.toDto(format);
    }

    @Transactional
    public AssetLabelFormatDto setActive(UUID id, boolean active) {
        AssetLabelFormat format = getOrThrow(id);
        if (format.isActive() == active) {
            return formatMapper.toDto(format);
        }
        AssetLabelFormatDto before = formatMapper.toDto(format);
        format.setActive(active);
        auditRecorder.record(AuditAction.CHANGEMENT_STATUT, "ETIQUETAGE", "asset_label_formats", format.getId(), before, formatMapper.toDto(format));
        return formatMapper.toDto(format);
    }

    @Transactional
    public void delete(UUID id) {
        AssetLabelFormat format = getOrThrow(id);
        if (assetLabelRepository.existsByFormatId(id) || locationLabelRepository.existsByFormatId(id)) {
            throw new BusinessRuleException(
                    "Impossible de supprimer ce format : des etiquettes ont deja ete generees avec. Desactivez-le plutot.");
        }
        AssetLabelFormatDto before = formatMapper.toDto(format);
        formatRepository.delete(format);
        auditRecorder.record(AuditAction.SUPPRESSION, "ETIQUETAGE", "asset_label_formats", id, before, null);
    }

    private void applyRequest(AssetLabelFormat format, AssetLabelFormatRequest request) {
        format.setName(request.name().trim());
        format.setWidthMm(request.widthMm());
        format.setHeightMm(request.heightMm());
        format.setShowLogo(request.showLogo());
        format.setShowShortDesignation(request.showShortDesignation());
        format.setShowQrCode(request.showQrCode());
        format.setShowBarcode(request.showBarcode());
    }

    private AssetLabelFormat getOrThrow(UUID id) {
        return formatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Format d'etiquette introuvable"));
    }
}
