package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.RoleDto;
import dz.vecopharm.vecoassets.entity.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleDto toDto(Role role);
}
