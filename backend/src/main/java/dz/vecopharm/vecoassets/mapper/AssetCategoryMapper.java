package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.AssetCategoryDto;
import dz.vecopharm.vecoassets.entity.AssetCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AssetCategoryMapper {

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    AssetCategoryDto toDto(AssetCategory category);
}
