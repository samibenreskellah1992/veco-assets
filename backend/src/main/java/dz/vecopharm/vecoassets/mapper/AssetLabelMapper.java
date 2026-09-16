package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.AssetLabelDto;
import dz.vecopharm.vecoassets.entity.AssetLabel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AssetLabelMapper {

    @Mapping(target = "formatId", expression = "java(label.getFormat().getId())")
    @Mapping(target = "formatCode", expression = "java(label.getFormat().getCode())")
    @Mapping(target = "formatName", expression = "java(label.getFormat().getName())")
    @Mapping(target = "generatedByName", expression = "java(label.getGeneratedBy() != null ? label.getGeneratedBy().getFullName() : null)")
    AssetLabelDto toDto(AssetLabel label);
}
