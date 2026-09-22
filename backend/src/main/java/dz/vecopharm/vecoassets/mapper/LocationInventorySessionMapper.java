package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.LocationInventorySessionDto;
import dz.vecopharm.vecoassets.entity.LocationInventorySession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LocationInventorySessionMapper {

    @Mapping(target = "locationId", expression = "java(session.getLocation() != null ? session.getLocation().getId() : null)")
    @Mapping(target = "locationCode", expression = "java(session.getLocation() != null ? session.getLocation().getCode() : null)")
    @Mapping(target = "locationName", expression = "java(session.getLocation() != null ? session.getLocation().getName() : null)")
    @Mapping(target = "openedById", expression = "java(session.getOpenedBy() != null ? session.getOpenedBy().getId() : null)")
    @Mapping(target = "openedByName", expression = "java(session.getOpenedBy() != null ? session.getOpenedBy().getFullName() : null)")
    @Mapping(target = "validatedById", expression = "java(session.getValidatedBy() != null ? session.getValidatedBy().getId() : null)")
    @Mapping(target = "validatedByName", expression = "java(session.getValidatedBy() != null ? session.getValidatedBy().getFullName() : null)")
    LocationInventorySessionDto toDto(LocationInventorySession session);
}
