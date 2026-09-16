package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.AssetLabelDto;
import dz.vecopharm.vecoassets.entity.Asset;
import dz.vecopharm.vecoassets.entity.AssetLabel;
import dz.vecopharm.vecoassets.entity.AssetLabelFormat;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.entity.User;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.mapper.AssetLabelMapper;
import dz.vecopharm.vecoassets.repository.AssetLabelFormatRepository;
import dz.vecopharm.vecoassets.repository.AssetLabelRepository;
import dz.vecopharm.vecoassets.repository.AssetRepository;
import dz.vecopharm.vecoassets.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Generation d'etiquettes (prompt maitre Phase 6). Produit un vrai PDF
 * ({@link LabelPdfBuilder}, QR/code-barres reels via {@link LabelImageGenerator})
 * et trace chaque generation : une ligne {@code asset_labels} par
 * immobilisation, l'immobilisation elle-meme marquee {@code labeled = true},
 * et une entree d'audit ({@code AuditAction.GENERATION_ETIQUETTE}) - jamais
 * une generation silencieuse.
 */
@Service
public class AssetLabelService {

    private final AssetRepository assetRepository;
    private final AssetLabelFormatRepository formatRepository;
    private final AssetLabelRepository assetLabelRepository;
    private final UserRepository userRepository;
    private final AssetLabelMapper assetLabelMapper;
    private final LabelPdfBuilder labelPdfBuilder;
    private final AuditRecorder auditRecorder;

    public AssetLabelService(
            AssetRepository assetRepository,
            AssetLabelFormatRepository formatRepository,
            AssetLabelRepository assetLabelRepository,
            UserRepository userRepository,
            AssetLabelMapper assetLabelMapper,
            LabelPdfBuilder labelPdfBuilder,
            AuditRecorder auditRecorder
    ) {
        this.assetRepository = assetRepository;
        this.formatRepository = formatRepository;
        this.assetLabelRepository = assetLabelRepository;
        this.userRepository = userRepository;
        this.assetLabelMapper = assetLabelMapper;
        this.labelPdfBuilder = labelPdfBuilder;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<AssetLabelDto> historyForAsset(UUID assetId) {
        if (!assetRepository.existsById(assetId)) {
            throw new ResourceNotFoundException("Immobilisation introuvable");
        }
        return assetLabelRepository.findByAssetIdOrderByGeneratedAtDesc(assetId).stream()
                .map(assetLabelMapper::toDto)
                .toList();
    }

    @Transactional
    public byte[] generate(List<UUID> assetIds, UUID formatId) {
        AssetLabelFormat format = formatRepository.findById(formatId)
                .orElseThrow(() -> new BusinessRuleException("Format d'etiquette introuvable"));
        if (!format.isActive()) {
            throw new BusinessRuleException("Ce format d'etiquette est desactive");
        }

        List<Asset> assets = assetRepository.findAllById(assetIds);
        if (assets.size() != assetIds.size()) {
            throw new BusinessRuleException("Une ou plusieurs immobilisations selectionnees sont introuvables");
        }
        Asset archived = assets.stream().filter(Asset::isDeleted).findFirst().orElse(null);
        if (archived != null) {
            throw new BusinessRuleException(
                    "Impossible de generer une etiquette pour une immobilisation archivee (" + archived.getAssetCode() + ")");
        }

        byte[] pdf = labelPdfBuilder.build(assets, format);

        User actor = currentUser();
        Instant now = Instant.now();
        for (Asset asset : assets) {
            AssetLabel label = new AssetLabel();
            label.setAsset(asset);
            label.setFormat(format);
            label.setGeneratedBy(actor);
            label.setGeneratedAt(now);
            label = assetLabelRepository.save(label);

            asset.setLabeled(true);

            auditRecorder.record(
                    AuditAction.GENERATION_ETIQUETTE,
                    "ETIQUETAGE",
                    "assets",
                    asset.getId(),
                    null,
                    assetLabelMapper.toDto(label));
        }

        return pdf;
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }
}
