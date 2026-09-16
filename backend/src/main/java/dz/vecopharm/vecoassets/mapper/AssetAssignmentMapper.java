package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.AssetAssignmentDto;
import dz.vecopharm.vecoassets.entity.AssetAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AssetAssignmentMapper {

    @Mapping(target = "userId", expression = "java(assignment.getUser() != null ? assignment.getUser().getId() : null)")
    @Mapping(target = "userName", expression = "java(assignment.getUser() != null ? assignment.getUser().getFullName() : null)")
    @Mapping(target = "assignedByName", expression = "java(assignment.getAssignedBy() != null ? assignment.getAssignedBy().getFullName() : null)")
    AssetAssignmentDto toDto(AssetAssignment assignment);
}
