package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.LocationLabelDto;
import dz.vecopharm.vecoassets.entity.LocationLabel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Checkpoint 2 "locaux scannables" (2026-09) - mirroir de {@link AssetLabelMapper}. */
@Mapper(componentModel = "spring")
public interface LocationLabelMapper {

    @Mapping(target = "formatId", expression = "java(label.getFormat().getId())")
    @Mapping(target = "formatCode", expression = "java(label.getFormat().getCode())")
    @Mapping(target = "formatName", expression = "java(label.getFormat().getName())")
    @Mapping(target = "generatedByName", expression = "java(label.getGeneratedBy() != null ? label.getGeneratedBy().getFullName() : null)")
    LocationLabelDto toDto(LocationLabel label);
}
