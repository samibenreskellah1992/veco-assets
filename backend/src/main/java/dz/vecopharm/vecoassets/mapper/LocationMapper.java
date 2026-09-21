package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.LocationDto;
import dz.vecopharm.vecoassets.entity.Location;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * {@code Location} n'a qu'une reference directe vers {@code zone}
 * (contrairement a {@code Asset}, qui denormalise site/batiment/etage/zone
 * pour l'affectation courante - voir {@link AssetMapper}) : la chaine
 * hierarchique (site/batiment/etage) est donc resolue ici en traversant
 * {@code zone.getFloor().getBuilding().getSite()}.
 *
 * <p>{@code assetCount} n'est pas mappe : un comptage par local necessite
 * une requete d'agregation batchee sur plusieurs locaux a la fois (voir
 * {@code LocationRepository#countAssetsByLocationIds}), ce que MapStruct ne
 * peut pas exprimer proprement instance par instance - {@code
 * LocationService} le renseigne apres coup via le constructeur canonique du
 * record.</p>
 */
@Mapper(componentModel = "spring")
public interface LocationMapper {

    @Mapping(target = "zoneId", source = "zone.id")
    @Mapping(target = "zoneName", source = "zone.name")
    @Mapping(target = "responsibleUserId", expression = "java(location.getResponsibleUser() != null ? location.getResponsibleUser().getId() : null)")
    @Mapping(target = "responsibleUserName", expression = "java(location.getResponsibleUser() != null ? location.getResponsibleUser().getFullName() : null)")
    @Mapping(target = "floorId", expression = "java(location.getZone() != null ? location.getZone().getFloor().getId() : null)")
    @Mapping(target = "floorName", expression = "java(location.getZone() != null ? location.getZone().getFloor().getName() : null)")
    @Mapping(target = "buildingId", expression = "java(location.getZone() != null ? location.getZone().getFloor().getBuilding().getId() : null)")
    @Mapping(target = "buildingName", expression = "java(location.getZone() != null ? location.getZone().getFloor().getBuilding().getName() : null)")
    @Mapping(target = "siteId", expression = "java(location.getZone() != null ? location.getZone().getFloor().getBuilding().getSite().getId() : null)")
    @Mapping(target = "siteName", expression = "java(location.getZone() != null ? location.getZone().getFloor().getBuilding().getSite().getName() : null)")
    @Mapping(target = "assetCount", ignore = true)
    LocationDto toDto(Location location);
}
