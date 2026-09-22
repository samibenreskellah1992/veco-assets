package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.InventoryAnomalyDto;
import dz.vecopharm.vecoassets.entity.InventoryAnomaly;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryAnomalyMapper {

    @Mapping(target = "campaignId", expression = "java(anomaly.getCampaign() != null ? anomaly.getCampaign().getId() : null)")
    @Mapping(target = "campaignName", expression = "java(anomaly.getCampaign() != null ? anomaly.getCampaign().getName() : null)")
    @Mapping(target = "locationSessionId", expression = "java(anomaly.getLocationSession() != null ? anomaly.getLocationSession().getId() : null)")
    @Mapping(target = "assetId", expression = "java(anomaly.getAsset() != null ? anomaly.getAsset().getId() : null)")
    @Mapping(target = "assetCode", expression = "java(anomaly.getAsset() != null ? anomaly.getAsset().getAssetCode() : null)")
    @Mapping(target = "assetDesignation", expression = "java(anomaly.getAsset() != null ? anomaly.getAsset().getDesignation() : null)")
    @Mapping(target = "scanId", expression = "java(anomaly.getScan() != null ? anomaly.getScan().getId() : null)")
    @Mapping(target = "reportedById", expression = "java(anomaly.getReportedBy() != null ? anomaly.getReportedBy().getId() : null)")
    @Mapping(target = "reportedByName", expression = "java(anomaly.getReportedBy() != null ? anomaly.getReportedBy().getFullName() : null)")
    InventoryAnomalyDto toDto(InventoryAnomaly anomaly);
}
