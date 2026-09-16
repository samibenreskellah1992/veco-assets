package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.BuildingDto;
import dz.vecopharm.vecoassets.entity.Building;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BuildingMapper {

    @Mapping(target = "siteId", source = "site.id")
    @Mapping(target = "siteName", source = "site.name")
    BuildingDto toDto(Building building);
}
