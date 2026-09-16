package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.AssetDto;
import dz.vecopharm.vecoassets.entity.Asset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Resout les libelles des entites liees (categorie, hierarchie de
 * localisation, utilisateurs) en plus de leurs identifiants, pour que le
 * frontend n'ait jamais a faire de round-trip supplementaire pour afficher
 * une liste ou une fiche d'immobilisation - meme convention que
 * {@link UserMapper#toSummary} pour les champs derives nullable.
 */
@Mapper(componentModel = "spring")
public interface AssetMapper {

    @Mapping(target = "categoryId", expression = "java(asset.getCategory() != null ? asset.getCategory().getId() : null)")
    @Mapping(target = "categoryName", expression = "java(asset.getCategory() != null ? asset.getCategory().getName() : null)")
    @Mapping(target = "siteId", expression = "java(asset.getSite() != null ? asset.getSite().getId() : null)")
    @Mapping(target = "siteName", expression = "java(asset.getSite() != null ? asset.getSite().getName() : null)")
    @Mapping(target = "buildingId", expression = "java(asset.getBuilding() != null ? asset.getBuilding().getId() : null)")
    @Mapping(target = "buildingName", expression = "java(asset.getBuilding() != null ? asset.getBuilding().getName() : null)")
    @Mapping(target = "floorId", expression = "java(asset.getFloor() != null ? asset.getFloor().getId() : null)")
    @Mapping(target = "floorName", expression = "java(asset.getFloor() != null ? asset.getFloor().getName() : null)")
    @Mapping(target = "zoneId", expression = "java(asset.getZone() != null ? asset.getZone().getId() : null)")
    @Mapping(target = "zoneName", expression = "java(asset.getZone() != null ? asset.getZone().getName() : null)")
    @Mapping(target = "locationId", expression = "java(asset.getLocation() != null ? asset.getLocation().getId() : null)")
    @Mapping(target = "locationName", expression = "java(asset.getLocation() != null ? asset.getLocation().getName() : null)")
    @Mapping(target = "currentUserId", expression = "java(asset.getCurrentUser() != null ? asset.getCurrentUser().getId() : null)")
    @Mapping(target = "currentUserName", expression = "java(asset.getCurrentUser() != null ? asset.getCurrentUser().getFullName() : null)")
    @Mapping(target = "responsibleUserId", expression = "java(asset.getResponsibleUser() != null ? asset.getResponsibleUser().getId() : null)")
    @Mapping(target = "responsibleUserName", expression = "java(asset.getResponsibleUser() != null ? asset.getResponsibleUser().getFullName() : null)")
    AssetDto toDto(Asset asset);
}
