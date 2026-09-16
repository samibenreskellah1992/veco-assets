package dz.vecopharm.vecoassets.mapper;

import dz.vecopharm.vecoassets.dto.AssetStatusHistoryDto;
import dz.vecopharm.vecoassets.entity.AssetStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AssetStatusHistoryMapper {

    @Mapping(target = "changedByName", expression = "java(history.getChangedBy() != null ? history.getChangedBy().getFullName() : null)")
    AssetStatusHistoryDto toDto(AssetStatusHistory history);
}
