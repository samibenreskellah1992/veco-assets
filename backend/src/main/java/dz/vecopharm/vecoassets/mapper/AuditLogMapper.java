package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.AuditLogDto;
import dz.vecopharm.vecoassets.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "userFullName", expression = "java(log.getUser() != null ? log.getUser().getFullName() : null)")
    @Mapping(target = "action", expression = "java(log.getAction().name())")
    AuditLogDto toDto(AuditLog log);
}
