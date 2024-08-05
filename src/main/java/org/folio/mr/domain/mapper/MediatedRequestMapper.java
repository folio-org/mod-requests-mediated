package org.folio.mr.domain.mapper;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.folio.mr.domain.FulfillmentPreference;
import org.folio.mr.domain.MediatedRequestStatus;
import org.folio.mr.domain.RequestLevel;
import org.folio.mr.domain.RequestType;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequestInstanceIdentifiersInner;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.domain.entity.MediatedRequestInstanceIdentifier;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
  nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
  imports = {RequestLevel.class, RequestType.class, MediatedRequestStatus.class,
    FulfillmentPreference.class})
public interface MediatedRequestMapper {
  @Mapping(target = "id", qualifiedByName = "UuidToStringSafe")
  @Mapping(target = "requestLevel", qualifiedByName = "EntityRequestLevelToDtoRequestLevel")
  @Mapping(target = "requestType", qualifiedByName = "EntityRequestTypeToDtoRequestType")
  @Mapping(target = "fulfillmentPreference", qualifiedByName = "EntityFulfillmentPreferenceToDtoFulfillmentPreference")
  @Mapping(target = "mediatedRequestStatus", qualifiedByName = "EntityMediatedRequestStatusToDtoMediatedRequestStatus")
  @Mapping(target = "status", qualifiedByName = "StringToStatus")
  @Mapping(target = "instance.title", source = "instanceTitle")
  @Mapping(target = "instance.identifiers", source="instanceIdentifiers", qualifiedByName = "EntityIdentifiersToDtoIdentifiers")
  @Mapping(target = "item.barcode", source="itemBarcode")
  @Mapping(target = "requester.firstName", source="requesterFirstName")
  @Mapping(target = "requester.lastName", source="requesterLastName")
  @Mapping(target = "requester.middleName", source="requesterMiddleName")
  @Mapping(target = "requester.barcode", source="requesterBarcode")
  @Mapping(target = "proxy.firstName", source="proxyFirstName")
  @Mapping(target = "proxy.lastName", source="proxyLastName")
  @Mapping(target = "proxy.middleName", source="proxyMiddleName")
  @Mapping(target = "proxy.barcode", source="proxyBarcode")
  // Search index
  @Mapping(target = "searchIndex.callNumberComponents.callNumber", source="callNumber")
  @Mapping(target = "searchIndex.callNumberComponents.prefix", source="callNumberPrefix")
  @Mapping(target = "searchIndex.callNumberComponents.suffix", source="callNumberSuffix")
  @Mapping(target = "searchIndex.shelvingOrder", source="shelvingOrder")
  @Mapping(target = "searchIndex.pickupServicePointName", source="pickupServicePointName")
  // Metadata
  @Mapping(target = "metadata.createdDate", source = "createdDate")
  @Mapping(target = "metadata.createdByUserId", source="createdByUserId", qualifiedByName = "UuidToStringSafe")
  @Mapping(target = "metadata.createdByUsername", source = "createdByUsername")
  @Mapping(target = "metadata.updatedDate", source = "updatedDate")
  @Mapping(target = "metadata.updatedByUserId", source="updatedByUserId", qualifiedByName = "UuidToStringSafe")
  @Mapping(target = "metadata.updatedByUsername", source = "updatedByUsername")
  MediatedRequest mapEntityToDto(MediatedRequestEntity mediatedRequestEntity);

  @Mapping(target = "id", qualifiedByName = "StringToUuidSafe")
  @Mapping(target = "requestLevel", qualifiedByName = "DtoRequestLevelToEntityRequestLevel")
  @Mapping(target = "requestType", qualifiedByName = "DtoRequestTypeToEntityRequestType")
  @Mapping(target = "fulfillmentPreference", qualifiedByName = "DtoFulfillmentPreferenceToEntityFulfillmentPreference")
  @Mapping(target = "mediatedRequestStatus", qualifiedByName = "DtoMediatedRequestStatusToEntityMediatedRequestStatus")
  @Mapping(target = "status", qualifiedByName = "StatusToString")
  @Mapping(target = "instanceTitle", source = "instance.title")
  @Mapping(target = "instanceIdentifiers", source = "instance.identifiers", qualifiedByName = "InstanceIdentifiersToIdentifiersSet")
  @Mapping(target = "itemBarcode", source = "item.barcode")
  @Mapping(target = "requesterFirstName", source = "requester.firstName")
  @Mapping(target = "requesterLastName", source = "requester.lastName")
  @Mapping(target = "requesterMiddleName", source = "requester.middleName")
  @Mapping(target = "requesterBarcode", source = "requester.barcode")
  @Mapping(target = "proxyFirstName", source = "proxy.firstName")
  @Mapping(target = "proxyLastName", source = "proxy.lastName")
  @Mapping(target = "proxyMiddleName", source = "proxy.middleName")
  @Mapping(target = "proxyBarcode", source = "proxy.barcode")
  // Search index
  @Mapping(target = "callNumber", source = "searchIndex.callNumberComponents.callNumber")
  @Mapping(target = "callNumberPrefix", source = "searchIndex.callNumberComponents.prefix")
  @Mapping(target = "callNumberSuffix", source = "searchIndex.callNumberComponents.suffix")
  @Mapping(target = "shelvingOrder", source = "searchIndex.shelvingOrder")
  @Mapping(target = "pickupServicePointName", source = "searchIndex.pickupServicePointName")
  // Metadata
  @Mapping(target = "createdDate", source = "metadata.createdDate")
  @Mapping(target = "createdByUserId", source = "metadata.createdByUserId", qualifiedByName = "StringToUuidSafe")
  @Mapping(target = "createdByUsername", source = "metadata.createdByUsername")
  @Mapping(target = "updatedDate", source = "metadata.updatedDate")
  @Mapping(target = "updatedByUserId", source = "metadata.updatedByUserId", qualifiedByName = "StringToUuidSafe")
  @Mapping(target = "updatedByUsername", source = "metadata.updatedByUsername")
  MediatedRequestEntity mapDtoToEntity(MediatedRequest mediatedRequest);


  @Named("EntityRequestLevelToDtoRequestLevel")
  default MediatedRequest.RequestLevelEnum mapEntityRequestLevelToDtoRequestLevel(
    RequestLevel requestLevel) {

    return requestLevel != null
      ? MediatedRequest.RequestLevelEnum.fromValue(requestLevel.getValue())
      : null;
  }

  @Named("DtoRequestLevelToEntityRequestLevel")
  default RequestLevel mapDtoRequestLevelToEntityRequestLevel(
    MediatedRequest.RequestLevelEnum requestLevel) {

    return requestLevel != null
      ? RequestLevel.fromValue(requestLevel.getValue())
      : null;
  }

  @Named("EntityRequestTypeToDtoRequestType")
  default MediatedRequest.RequestTypeEnum mapEntityRequestTypeToDtoRequestType(
    RequestType requestType) {

    return requestType != null
      ? MediatedRequest.RequestTypeEnum.fromValue(requestType.getValue())
      : null;
  }

  @Named("DtoRequestTypeToEntityRequestType")
  default RequestType mapDtoRequestTypeToEntityRequestType(
    MediatedRequest.RequestTypeEnum requestType) {

    return requestType != null
      ? RequestType.fromValue(requestType.getValue())
      : null;
  }

  @Named("EntityFulfillmentPreferenceToDtoFulfillmentPreference")
  default MediatedRequest.FulfillmentPreferenceEnum mapEntityFulfillmentPreferenceToDtoFulfillmentPreference(
    FulfillmentPreference fulfillmentPreference) {

    return fulfillmentPreference != null
      ? MediatedRequest.FulfillmentPreferenceEnum.fromValue(fulfillmentPreference.getValue())
      : null;
  }

  @Named("DtoFulfillmentPreferenceToEntityFulfillmentPreference")
  default FulfillmentPreference mapDtoFulfillmentPreferenceToEntityFulfillmentPreference(
    MediatedRequest.FulfillmentPreferenceEnum fulfillmentPreference) {

    return fulfillmentPreference != null
      ? FulfillmentPreference.fromValue(fulfillmentPreference.getValue())
      : null;
  }

  @Named("EntityMediatedRequestStatusToDtoMediatedRequestStatus")
  default MediatedRequest.MediatedRequestStatusEnum mapEntityMediatedRequestStatusToDtoMediatedRequestStatus(
    MediatedRequestStatus mediatedRequestStatus) {

    return mediatedRequestStatus != null
      ? MediatedRequest.MediatedRequestStatusEnum.fromValue(mediatedRequestStatus.getValue())
      : null;
  }

  @Named("DtoMediatedRequestStatusToEntityMediatedRequestStatus")
  default MediatedRequestStatus mapDtoMediatedRequestStatusToEntityMediatedRequestStatus(
    MediatedRequest.MediatedRequestStatusEnum mediatedRequestStatus) {

    return mediatedRequestStatus != null
      ? MediatedRequestStatus.fromValue(mediatedRequestStatus.getValue())
      : null;
  }

  @Named("StringToStatus")
  default MediatedRequest.StatusEnum mapStringToStatus(String status) {
    return status != null ? MediatedRequest.StatusEnum.fromValue(status) : null;
  }

  @Named("StatusToString")
  default String mapStatusToString(MediatedRequest.StatusEnum status) {
    return status != null ? status.getValue() : null;
  }

  @Named("StringToUuidSafe")
  default UUID stringToUuidSafe(String uuid) {
    return (StringUtils.isBlank(uuid)) ? null : java.util.UUID.fromString(uuid);
  }

  @Named("UuidToStringSafe")
  default String uuidToStringSafe(UUID uuid) {
    return uuid != null ? uuid.toString() : null;
  }

  @Named("InstanceIdentifiersToIdentifiersSet")
  default Set<MediatedRequestInstanceIdentifier> mapInstanceDtoToIdentifiersSet(
    List<MediatedRequestInstanceIdentifiersInner> identifiers) {

    return identifiers == null ? null : identifiers.stream()
      .map(this::dtoIdentifierInnerToEntityIdentifier)
      .collect(Collectors.toSet());
  }

  default MediatedRequestInstanceIdentifier dtoIdentifierInnerToEntityIdentifier(
    MediatedRequestInstanceIdentifiersInner identifierInner) {

    var identifier = new MediatedRequestInstanceIdentifier();

    identifier.setIdentifierTypeId(stringToUuidSafe(identifierInner.getIdentifierTypeId()));
    identifier.setValue(identifierInner.getValue());
    return identifier;
  }

  @Named("EntityIdentifiersToDtoIdentifiers")
  default List<MediatedRequestInstanceIdentifiersInner> mapEntityIdentifiersToDtoIdentifiers(
    Set<MediatedRequestInstanceIdentifier> identifiers) {

    return identifiers == null ? null : identifiers.stream()
          .map(this::entityIdentifierToDtoIdentifierInner)
          .toList();
  }

  default MediatedRequestInstanceIdentifiersInner entityIdentifierToDtoIdentifierInner(
    MediatedRequestInstanceIdentifier identifier) {

    return identifier == null ? null : new MediatedRequestInstanceIdentifiersInner()
      .identifierTypeId(identifier.getIdentifierTypeId().toString())
      .value(identifier.getValue());
  }

  @AfterMapping
  default void setIdentifierParentEntity(@MappingTarget MediatedRequestEntity mediatedRequest) {
    if (mediatedRequest.getInstanceIdentifiers() == null) {
      return;
    }

    mediatedRequest.getInstanceIdentifiers().stream()
      .filter(Objects::nonNull)
      .forEach(identifier -> identifier.setMediatedRequest(mediatedRequest));
  }
}
