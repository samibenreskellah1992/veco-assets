package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.InventoryScanDto;
import dz.vecopharm.vecoassets.entity.InventoryScan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryScanMapper {

    @Mapping(target = "campaignId", expression = "java(scan.getCampaign() != null ? scan.getCampaign().getId() : null)")
    @Mapping(target = "campaignName", expression = "java(scan.getCampaign() != null ? scan.getCampaign().getName() : null)")
    @Mapping(target = "locationSessionId", expression = "java(scan.getLocationSession() != null ? scan.getLocationSession().getId() : null)")
    @Mapping(target = "assetId", expression = "java(scan.getAsset() != null ? scan.getAsset().getId() : null)")
    @Mapping(target = "assetCode", expression = "java(scan.getAsset() != null ? scan.getAsset().getAssetCode() : null)")
    @Mapping(target = "assetDesignation", expression = "java(scan.getAsset() != null ? scan.getAsset().getDesignation() : null)")
    @Mapping(target = "scannedById", expression = "java(scan.getScannedBy() != null ? scan.getScannedBy().getId() : null)")
    @Mapping(target = "scannedByName", expression = "java(scan.getScannedBy() != null ? scan.getScannedBy().getFullName() : null)")
    InventoryScanDto toDto(InventoryScan scan);
}
