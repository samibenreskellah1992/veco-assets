package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.AssetLabelFormatDto;
import dz.vecopharm.vecoassets.entity.AssetLabelFormat;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AssetLabelFormatMapper {
    AssetLabelFormatDto toDto(AssetLabelFormat format);
}
