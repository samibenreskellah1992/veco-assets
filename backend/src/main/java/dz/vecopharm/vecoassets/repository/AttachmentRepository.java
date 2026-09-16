package dz.vecopharm.vecoassets.repository;

import dz.vecopharm.vecoassets.entity.Attachment;
import dz.vecopharm.vecoassets.entity.AttachmentOwnerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    List<Attachment> findByOwnerTypeAndOwnerId(AttachmentOwnerType ownerType, UUID ownerId);
}
