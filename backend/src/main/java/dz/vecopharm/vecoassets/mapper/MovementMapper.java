package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.MovementDto;
import dz.vecopharm.vecoassets.entity.AssetMovement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MovementMapper {

    @Mapping(target = "assetId", expression = "java(movement.getAsset().getId())")
    @Mapping(target = "assetCode", expression = "java(movement.getAsset().getAssetCode())")
    @Mapping(target = "assetDesignation", expression = "java(movement.getAsset().getDesignation())")
    @Mapping(target = "fromSiteId", expression = "java(movement.getFromSite() != null ? movement.getFromSite().getId() : null)")
    @Mapping(target = "fromSiteName", expression = "java(movement.getFromSite() != null ? movement.getFromSite().getName() : null)")
    @Mapping(target = "toSiteId", expression = "java(movement.getToSite() != null ? movement.getToSite().getId() : null)")
    @Mapping(target = "toSiteName", expression = "java(movement.getToSite() != null ? movement.getToSite().getName() : null)")
    @Mapping(target = "fromLocationId", expression = "java(movement.getFromLocation() != null ? movement.getFromLocation().getId() : null)")
    @Mapping(target = "fromLocationName", expression = "java(movement.getFromLocation() != null ? movement.getFromLocation().getName() : null)")
    @Mapping(target = "toLocationId", expression = "java(movement.getToLocation() != null ? movement.getToLocation().getId() : null)")
    @Mapping(target = "toLocationName", expression = "java(movement.getToLocation() != null ? movement.getToLocation().getName() : null)")
    @Mapping(target = "fromUserId", expression = "java(movement.getFromUser() != null ? movement.getFromUser().getId() : null)")
    @Mapping(target = "fromUserName", expression = "java(movement.getFromUser() != null ? movement.getFromUser().getFullName() : null)")
    @Mapping(target = "toUserId", expression = "java(movement.getToUser() != null ? movement.getToUser().getId() : null)")
    @Mapping(target = "toUserName", expression = "java(movement.getToUser() != null ? movement.getToUser().getFullName() : null)")
    @Mapping(target = "requestedById", expression = "java(movement.getRequestedBy() != null ? movement.getRequestedBy().getId() : null)")
    @Mapping(target = "requestedByName", expression = "java(movement.getRequestedBy() != null ? movement.getRequestedBy().getFullName() : null)")
    @Mapping(target = "validatedById", expression = "java(movement.getValidatedBy() != null ? movement.getValidatedBy().getId() : null)")
    @Mapping(target = "validatedByName", expression = "java(movement.getValidatedBy() != null ? movement.getValidatedBy().getFullName() : null)")
    MovementDto toDto(AssetMovement movement);
}
