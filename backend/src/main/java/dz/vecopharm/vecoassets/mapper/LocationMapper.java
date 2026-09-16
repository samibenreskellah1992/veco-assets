package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.LocationDto;
import dz.vecopharm.vecoassets.entity.Location;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    @Mapping(target = "zoneId", source = "zone.id")
    @Mapping(target = "zoneName", source = "zone.name")
    LocationDto toDto(Location location);
}
