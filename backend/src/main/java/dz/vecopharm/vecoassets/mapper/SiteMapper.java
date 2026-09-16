package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.SiteDto;
import dz.vecopharm.vecoassets.entity.Site;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SiteMapper {
    SiteDto toDto(Site site);
}
