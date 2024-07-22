package org.folio.mr.domain.mapper;

import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface MediatedRequestMapper {
  @Mapping(target = "requestType", qualifiedByName = "StringToRequestType")
  @Mapping(target = "requestLevel", qualifiedByName = "StringToRequestLevel")
  @Mapping(target = "fulfillmentPreference", qualifiedByName = "StringToFulfillmentPreference")
  @Mapping(target = "status", qualifiedByName = "StringToStatus")
  MediatedRequest mapEntityToDto(MediatedRequestEntity mediatedRequestEntity);

  @Mapping(target = "requestType", qualifiedByName = "RequestTypeToString")
  @Mapping(target = "requestLevel", qualifiedByName = "RequestLevelToString")
  @Mapping(target = "fulfillmentPreference", qualifiedByName = "FulfillmentPreferenceToString")
  @Mapping(target = "status", qualifiedByName = "StatusToString")
  MediatedRequestEntity mapDtoToEntity(MediatedRequest mediatedRequest);

  @Named("StringToRequestType")
  default MediatedRequest.RequestTypeEnum mapRequestType(String requestType) {
    return requestType != null ? MediatedRequest.RequestTypeEnum.fromValue(requestType) : null;
  }

  @Named("StringToRequestLevel")
  default MediatedRequest.RequestLevelEnum mapRequestLevel(String requestLevel) {
    return requestLevel != null ? MediatedRequest.RequestLevelEnum.fromValue(requestLevel) : null;
  }

  @Named("StringToFulfillmentPreference")
  default MediatedRequest.FulfillmentPreferenceEnum mapFulfillmentPreference(String fulfillmentPreference) {
    return fulfillmentPreference != null ? MediatedRequest.FulfillmentPreferenceEnum.fromValue(fulfillmentPreference) : null;
  }

  @Named("StringToStatus")
  default MediatedRequest.StatusEnum mapStringToStatus(String status) {
    return status != null ? MediatedRequest.StatusEnum.fromValue(status) : null;
  }

  @Named("RequestTypeToString")
  default String mapRequestTypeToString(MediatedRequest.RequestTypeEnum requestTypeEnum) {
    return requestTypeEnum != null ? requestTypeEnum.getValue() : null;
  }

  @Named("RequestLevelToString")
  default String mapRequestLevelToString(MediatedRequest.RequestLevelEnum requestLevelEnum) {
    return requestLevelEnum != null ? requestLevelEnum.getValue() : null;
  }

  @Named("FulfillmentPreferenceToString")
  default String mapFulfillmentPreferenceToString(MediatedRequest.FulfillmentPreferenceEnum fulfillmentPreferenceEnum) {
    return fulfillmentPreferenceEnum != null ? fulfillmentPreferenceEnum.getValue() : null;
  }

  @Named("StatusToString")
  default String mapStatusToString(MediatedRequest.StatusEnum status) {
    return status != null ? status.getValue() : null;
  }
}
