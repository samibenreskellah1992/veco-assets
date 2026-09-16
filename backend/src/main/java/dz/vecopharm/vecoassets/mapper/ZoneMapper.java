package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.ZoneDto;
import dz.vecopharm.vecoassets.entity.Zone;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ZoneMapper {

    @Mapping(target = "floorId", source = "floor.id")
    @Mapping(target = "floorName", source = "floor.name")
    ZoneDto toDto(Zone zone);
}
