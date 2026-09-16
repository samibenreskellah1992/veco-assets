package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.FloorDto;
import dz.vecopharm.vecoassets.entity.Floor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FloorMapper {

    @Mapping(target = "buildingId", source = "building.id")
    @Mapping(target = "buildingName", source = "building.name")
    FloorDto toDto(Floor floor);
}
