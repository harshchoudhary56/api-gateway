package com.apple.inc.gateway.util.mapper;

import com.apple.inc.gateway.dto.ApiRecordData;
import com.apple.inc.gateway.entities.ApiRecordEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApiRecordMapper {

    ApiRecordEntity toEntity(ApiRecordData data);

    ApiRecordData toDto(ApiRecordEntity apiRecordEntity);
}
