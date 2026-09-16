package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.InventoryCampaignDto;
import dz.vecopharm.vecoassets.entity.InventoryCampaign;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryCampaignMapper {

    @Mapping(target = "siteId", expression = "java(campaign.getSite() != null ? campaign.getSite().getId() : null)")
    @Mapping(target = "siteName", expression = "java(campaign.getSite() != null ? campaign.getSite().getName() : null)")
    @Mapping(target = "zoneId", expression = "java(campaign.getZone() != null ? campaign.getZone().getId() : null)")
    @Mapping(target = "zoneName", expression = "java(campaign.getZone() != null ? campaign.getZone().getName() : null)")
    @Mapping(target = "responsibleUserId", expression = "java(campaign.getResponsibleUser() != null ? campaign.getResponsibleUser().getId() : null)")
    @Mapping(target = "responsibleUserName", expression = "java(campaign.getResponsibleUser() != null ? campaign.getResponsibleUser().getFullName() : null)")
    InventoryCampaignDto toDto(InventoryCampaign campaign);
}
