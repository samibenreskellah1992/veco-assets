package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.UserSummaryDto;
import dz.vecopharm.vecoassets.entity.Permission;
import dz.vecopharm.vecoassets.entity.Role;
import dz.vecopharm.vecoassets.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Maps {@link User} to {@link UserSummaryDto}. Never maps {@code passwordHash}
 * - the DTO simply has no such field, so there is nothing to accidentally
 * expose.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    @Mapping(target = "siteName", expression = "java(user.getSite() != null ? user.getSite().getName() : null)")
    @Mapping(target = "roles", expression = "java(roleCodesOf(user))")
    @Mapping(target = "permissions", expression = "java(permissionCodesOf(user))")
    UserSummaryDto toSummary(User user);

    default List<String> roleCodesOf(User user) {
        return user.getRoles().stream()
                .map(Role::getCode)
                .sorted()
                .toList();
    }

    default List<String> permissionCodesOf(User user) {
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .distinct()
                .sorted()
                .toList();
    }
}
