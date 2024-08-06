package org.folio.mr.domain.mapper;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
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

@Mapper(componentModel = "spring",
  nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
  imports = {RequestLevel.class, RequestType.class, MediatedRequestStatus.class,
    FulfillmentPreference.class})
public interface MediatedRequestMapper {
  @Mapping(target = "id", qualifiedByName = "UuidToStringSafe")
  @Mapping(target = "requestLevel", expression =
    "java(mapEnum(mediatedRequestEntity.getRequestLevel(), RequestLevel::getValue, MediatedRequest.RequestLevelEnum::fromValue))")
  @Mapping(target = "requestType", expression =
    "java(mapEnum(mediatedRequestEntity.getRequestType(), RequestType::getValue, MediatedRequest.RequestTypeEnum::fromValue))")
  @Mapping(target = "fulfillmentPreference", expression =
    "java(mapEnum(mediatedRequestEntity.getFulfillmentPreference(), FulfillmentPreference::getValue, MediatedRequest.FulfillmentPreferenceEnum::fromValue))")
  @Mapping(target = "mediatedRequestStatus", expression =
    "java(mapEnum(mediatedRequestEntity.getMediatedRequestStatus(), MediatedRequestStatus::getValue, MediatedRequest.MediatedRequestStatusEnum::fromValue))")
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
  @Mapping(target = "requestLevel", expression =
    "java(mapEnum(mediatedRequest.getRequestLevel(), MediatedRequest.RequestLevelEnum::getValue, RequestLevel::fromValue))")
  @Mapping(target = "requestType", expression =
    "java(mapEnum(mediatedRequest.getRequestType(), MediatedRequest.RequestTypeEnum::getValue, RequestType::fromValue))")
  @Mapping(target = "fulfillmentPreference", expression =
    "java(mapEnum(mediatedRequest.getFulfillmentPreference(), MediatedRequest.FulfillmentPreferenceEnum::getValue, FulfillmentPreference::fromValue))")
  @Mapping(target = "mediatedRequestStatus", expression =
    "java(mapEnum(mediatedRequest.getMediatedRequestStatus(), MediatedRequest.MediatedRequestStatusEnum::getValue, MediatedRequestStatus::fromValue))")
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

  default <T, V> T mapEnum(V v, Function<V, String> valueGetter, Function <String, T> mapper) {
    return v != null
      ? mapper.apply(valueGetter.apply(v))
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
