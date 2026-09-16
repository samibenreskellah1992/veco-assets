package dz.vecopharm.vecoassets.service;

import dz.vecopharm.vecoassets.entity.Attachment;
import dz.vecopharm.vecoassets.exception.ResourceNotFoundException;
import dz.vecopharm.vecoassets.repository.AttachmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Lecture generique d'une piece jointe par id (prompt maitre section 47).
 * Pour l'instant seules les photos d'anomalie (Phase 7) en produisent, mais
 * cette lecture reste volontairement generique : elle servira telle quelle
 * aux photos d'immobilisation et aux PV de mouvement dans les phases
 * suivantes, sans reecriture.
 */
@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final AttachmentStorageService storageService;

    public AttachmentService(AttachmentRepository attachmentRepository, AttachmentStorageService storageService) {
        this.attachmentRepository = attachmentRepository;
        this.storageService = storageService;
    }

    public record DownloadableFile(byte[] content, String fileName, String contentType) {
    }

    @Transactional(readOnly = true)
    public DownloadableFile download(UUID id) {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Piece jointe introuvable"));
        byte[] content = storageService.read(attachment.getStoragePath());
        return new DownloadableFile(content, attachment.getFileName(), attachment.getContentType());
    }
}
