package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.AttachmentDto;
import dz.vecopharm.vecoassets.entity.Attachment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttachmentMapper {

    @Mapping(target = "ownerType", expression = "java(attachment.getOwnerType() != null ? attachment.getOwnerType().name() : null)")
    @Mapping(target = "category", expression = "java(attachment.getCategory() != null ? attachment.getCategory().name() : null)")
    @Mapping(target = "uploadedById", expression = "java(attachment.getUploadedBy() != null ? attachment.getUploadedBy().getId() : null)")
    @Mapping(target = "uploadedByName", expression = "java(attachment.getUploadedBy() != null ? attachment.getUploadedBy().getFullName() : null)")
    AttachmentDto toDto(Attachment attachment);
}
