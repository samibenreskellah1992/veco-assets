package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.audit.AuditRecorder;
import dz.vecopharm.vecoassets.dto.AttachmentDto;
import dz.vecopharm.vecoassets.dto.InventoryAnomalyDto;
import dz.vecopharm.vecoassets.entity.AnomalyStatus;
import dz.vecopharm.vecoassets.entity.Attachment;
import dz.vecopharm.vecoassets.entity.AttachmentCategory;
import dz.vecopharm.vecoassets.entity.AttachmentOwnerType;
import dz.vecopharm.vecoassets.entity.AuditAction;
import dz.vecopharm.vecoassets.entity.InventoryAnomaly;
import dz.vecopharm.vecoassets.entity.User;
import dz.vecopharm.vecoassets.exception.BusinessRuleException;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.mapper.AttachmentMapper;
import dz.vecopharm.vecoassets.mapper.InventoryAnomalyMapper;
import dz.vecopharm.vecoassets.repository.AttachmentRepository;
import dz.vecopharm.vecoassets.repository.InventoryAnomalyRepository;
import dz.vecopharm.vecoassets.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Gestion des anomalies d'inventaire (prompt maitre Phase 7 / section 16) :
 * consultation, changement de statut, et piece jointe photo (association
 * polymorphe {@code attachments}, deja en place depuis la Phase 2 mais
 * jamais utilisee jusqu'ici - voir {@link AttachmentStorageService}).
 */
@Service
public class InventoryAnomalyService {

    private static final Set<AnomalyStatus> TERMINAL_STATUSES = EnumSet.of(AnomalyStatus.RESOLUE, AnomalyStatus.REJETEE);

    /**
     * Types MIME reellement acceptes pour une photo d'anomalie (Phase 10,
     * revue securite). Un simple {@code startsWith("image/")} accepterait
     * aussi {@code image/svg+xml} : un SVG peut embarquer du {@code
     * <script>}, et {@code AttachmentController.download} le sert avec
     * {@code Content-Disposition: inline} - un vecteur XSS stocke classique.
     * Une photo prise depuis un telephone est toujours un raster (jpeg/png/
     * webp/gif/heic) : exclure le SVG ne retire aucun cas d'usage reel.
     */
    private static final Set<String> ALLOWED_PHOTO_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif", "image/heic", "image/heif");

    private final InventoryAnomalyRepository anomalyRepository;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentStorageService attachmentStorageService;
    private final UserRepository userRepository;
    private final InventoryAnomalyMapper anomalyMapper;
    private final AttachmentMapper attachmentMapper;
    private final AuditRecorder auditRecorder;

    public InventoryAnomalyService(
            InventoryAnomalyRepository anomalyRepository,
            AttachmentRepository attachmentRepository,
            AttachmentStorageService attachmentStorageService,
            UserRepository userRepository,
            InventoryAnomalyMapper anomalyMapper,
            AttachmentMapper attachmentMapper,
            AuditRecorder auditRecorder
    ) {
        this.anomalyRepository = anomalyRepository;
        this.attachmentRepository = attachmentRepository;
        this.attachmentStorageService = attachmentStorageService;
        this.userRepository = userRepository;
        this.anomalyMapper = anomalyMapper;
        this.attachmentMapper = attachmentMapper;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<InventoryAnomalyDto> listByCampaign(UUID campaignId) {
        return anomalyRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId).stream()
                .map(anomalyMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryAnomalyDto> listAll() {
        return anomalyRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(anomalyMapper::toDto)
                .toList();
    }

    @Transactional
    public InventoryAnomalyDto updateStatus(UUID id, AnomalyStatus target) {
        InventoryAnomaly anomaly = getOrThrow(id);
        if (TERMINAL_STATUSES.contains(anomaly.getStatus())) {
            throw new BusinessRuleException(
                    "Cette anomalie est deja " + anomaly.getStatus() + " (statut definitif), elle ne peut plus etre modifiee");
        }
        AnomalyStatus previous = anomaly.getStatus();
        anomaly.setStatus(target);
        anomaly = anomalyRepository.save(anomaly);
        auditRecorder.record(AuditAction.CHANGEMENT_STATUT, "INVENTAIRE", "inventory_anomalies", anomaly.getId(), previous, target);
        return anomalyMapper.toDto(anomaly);
    }

    @Transactional
    public AttachmentDto attachPhoto(UUID anomalyId, MultipartFile file) {
        InventoryAnomaly anomaly = getOrThrow(anomalyId);
        if (file == null || file.getContentType() == null || !ALLOWED_PHOTO_CONTENT_TYPES.contains(file.getContentType().toLowerCase())) {
            throw new BusinessRuleException("Seules les photos (JPEG, PNG, WEBP, GIF, HEIC) peuvent etre jointes a une anomalie");
        }
        AttachmentStorageService.StoredFile stored = attachmentStorageService.store(file);

        Attachment attachment = new Attachment();
        attachment.setOwnerType(AttachmentOwnerType.ANOMALY);
        attachment.setOwnerId(anomaly.getId());
        attachment.setCategory(AttachmentCategory.PHOTO);
        attachment.setFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo");
        attachment.setStoragePath(stored.storagePath());
        attachment.setContentType(file.getContentType());
        attachment.setSizeBytes(stored.sizeBytes());
        attachment.setUploadedBy(currentUser());
        attachment = attachmentRepository.save(attachment);

        auditRecorder.record(AuditAction.CREATION, "INVENTAIRE", "attachments", attachment.getId(), null, attachmentMapper.toDto(attachment));
        return attachmentMapper.toDto(attachment);
    }

    @Transactional(readOnly = true)
    public List<AttachmentDto> photos(UUID anomalyId) {
        if (!anomalyRepository.existsById(anomalyId)) {
            throw new ResourceNotFoundException("Anomalie introuvable");
        }
        return attachmentRepository.findByOwnerTypeAndOwnerId(AttachmentOwnerType.ANOMALY, anomalyId).stream()
                .map(attachmentMapper::toDto)
                .toList();
    }

    private InventoryAnomaly getOrThrow(UUID id) {
        return anomalyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anomalie introuvable"));
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }
}
