package org.folio.mr.domain.mapper;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.folio.mr.domain.FulfillmentPreference;
import org.folio.mr.domain.MediatedRequestStatus;
import org.folio.mr.domain.RequestLevel;
import org.folio.mr.domain.RequestType;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequestInstance;
import org.folio.mr.domain.dto.MediatedRequestInstanceIdentifiersInner;
import org.folio.mr.domain.dto.MediatedRequestItem;
import org.folio.mr.domain.dto.MediatedRequestProxy;
import org.folio.mr.domain.dto.MediatedRequestRequester;
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
  @Mapping(target = "id", expression = "java(uuidToStringSafe(mediatedRequestEntity.getId()))")
  @Mapping(target = "requestType", expression = "java(mediatedRequestEntity.getRequestType() != null ? "
    + "MediatedRequest.RequestTypeEnum.fromValue(mediatedRequestEntity.getRequestType().getValue()) : null)")
  @Mapping(target = "requestLevel", expression = "java(mediatedRequestEntity.getRequestLevel() != null ? "
    + "MediatedRequest.RequestLevelEnum.fromValue(mediatedRequestEntity.getRequestLevel().getValue()) : null)")
  @Mapping(target = "fulfillmentPreference", expression = "java(mediatedRequestEntity.getFulfillmentPreference() != null ? "
    + "MediatedRequest.FulfillmentPreferenceEnum.fromValue(mediatedRequestEntity.getFulfillmentPreference().getValue()) : null)")
  @Mapping(target = "status", qualifiedByName = "StringToStatus")
  @Mapping(target = "mediatedRequestStatus", expression = "java(mediatedRequestEntity.getMediatedRequestStatus() != null ? "
    + "MediatedRequest.MediatedRequestStatusEnum.fromValue(mediatedRequestEntity.getMediatedRequestStatus().getValue()) : null)")
  @Mapping(target = "instance", expression = "java(mapDtoInstance(mediatedRequestEntity))")
  @Mapping(target = "item", expression = "java(mapDtoItem(mediatedRequestEntity))")
  @Mapping(target = "requester", expression = "java(mapDtoRequester(mediatedRequestEntity))")
  @Mapping(target = "proxy", expression = "java(mapDtoProxy(mediatedRequestEntity))")
  // Search index
  @Mapping(target = "searchIndex.callNumberComponents.callNumber", source="callNumber")
  @Mapping(target = "searchIndex.callNumberComponents.prefix", source="callNumberPrefix")
  @Mapping(target = "searchIndex.callNumberComponents.suffix", source="callNumberSuffix")
  @Mapping(target = "searchIndex.shelvingOrder", source="shelvingOrder")
  @Mapping(target = "searchIndex.pickupServicePointName", source="pickupServicePointName")
  // Metadata
  @Mapping(target = "metadata.createdDate", source = "createdDate")
  @Mapping(target = "metadata.updatedDate", source = "updatedDate")
  @Mapping(target = "metadata.createdByUserId",
    expression = "java(mediatedRequestEntity.getCreatedByUserId() == null ? "
      + "null : String.valueOf(mediatedRequestEntity.getCreatedByUserId()))")
  @Mapping(target = "metadata.createdByUsername", source = "createdByUsername")
  @Mapping(target = "metadata.updatedByUserId",
    expression = "java(mediatedRequestEntity.getUpdatedByUserId() == null ? "
      + "null : String.valueOf(mediatedRequestEntity.getUpdatedByUserId()))")
  @Mapping(target = "metadata.updatedByUsername", source = "updatedByUsername")
  MediatedRequest mapEntityToDto(MediatedRequestEntity mediatedRequestEntity);

  @Mapping(target = "id", expression = "java(stringToUuidSafe(mediatedRequest.getId()))")
  @Mapping(target = "requestType", expression = "java(mediatedRequest.getRequestType() != null ? "
    + "RequestType.fromValue(mediatedRequest.getRequestType().getValue()) : null)")
  @Mapping(target = "requestLevel", expression = "java(mediatedRequest.getRequestLevel() != null ? "
    + "RequestLevel.fromValue(mediatedRequest.getRequestLevel().getValue()) : null)")
  @Mapping(target = "fulfillmentPreference", expression = "java(mediatedRequest.getFulfillmentPreference() != null ? "
    + "FulfillmentPreference.fromValue(mediatedRequest.getFulfillmentPreference().getValue()) : null)")
  @Mapping(target = "status", qualifiedByName = "StatusToString")
  @Mapping(target = "mediatedRequestStatus", expression = "java(mediatedRequest.getMediatedRequestStatus() != null ? "
    + "MediatedRequestStatus.fromValue(mediatedRequest.getMediatedRequestStatus().getValue()) : null)")
  @Mapping(target = "instanceTitle", source = "instance", qualifiedByName = "InstanceDtoToTitle")
  @Mapping(target = "instanceIdentifiers", source = "instance", qualifiedByName = "InstanceDtoToIdentifiersSet")
  @Mapping(target = "itemBarcode", source = "item", qualifiedByName = "ItemDtoToItemBarcode")
  @Mapping(target = "requesterFirstName", source = "requester", qualifiedByName = "RequesterDtoToRequesterFirstName")
  @Mapping(target = "requesterLastName", source = "requester", qualifiedByName = "RequesterDtoToRequesterLastName")
  @Mapping(target = "requesterMiddleName", source = "requester", qualifiedByName = "RequesterDtoToRequesterMiddleName")
  @Mapping(target = "requesterBarcode", source = "requester", qualifiedByName = "RequesterDtoToRequesterBarcode")
  @Mapping(target = "proxyFirstName", source = "proxy", qualifiedByName = "ProxyDtoToRequesterFirstName")
  @Mapping(target = "proxyLastName", source = "proxy", qualifiedByName = "ProxyDtoToRequesterLastName")
  @Mapping(target = "proxyMiddleName", source = "proxy", qualifiedByName = "ProxyDtoToRequesterMiddleName")
  @Mapping(target = "proxyBarcode", source = "proxy", qualifiedByName = "ProxyDtoToRequesterBarcode")
  // Search index
  @Mapping(target = "callNumber", source = "searchIndex.callNumberComponents.callNumber")
  @Mapping(target = "callNumberPrefix", source = "searchIndex.callNumberComponents.prefix")
  @Mapping(target = "callNumberSuffix", source = "searchIndex.callNumberComponents.suffix")
  @Mapping(target = "shelvingOrder", source = "searchIndex.shelvingOrder")
  @Mapping(target = "pickupServicePointName", source = "searchIndex.pickupServicePointName")
  // Metadata
  @Mapping(target = "createdByUserId", expression = "java(mediatedRequest.getMetadata() == null ? "
    + "null : stringToUuidSafe(mediatedRequest.getMetadata().getCreatedByUserId()))")
  @Mapping(target = "updatedByUserId", expression = "java(mediatedRequest.getMetadata() == null ? "
    + "null : stringToUuidSafe(mediatedRequest.getMetadata().getUpdatedByUserId()))")
  MediatedRequestEntity mapDtoToEntity(MediatedRequest mediatedRequest);

  @Named("StringToStatus")
  default MediatedRequest.StatusEnum mapStringToStatus(String status) {
    return status != null ? MediatedRequest.StatusEnum.fromValue(status) : null;
  }

  @Named("StatusToString")
  default String mapStatusToString(MediatedRequest.StatusEnum status) {
    return status != null ? status.getValue() : null;
  }

  default UUID stringToUuidSafe(String uuid) {
    return (StringUtils.isBlank(uuid)) ? null : java.util.UUID.fromString(uuid);
  }

  default String uuidToStringSafe(UUID uuid) {
    return uuid != null ? uuid.toString() : null;
  }

  @Named("InstanceDtoToTitle")
  default String mapInstanceDtoToTitle(MediatedRequestInstance instance) {
    return instance.getTitle();
  }

  @Named("InstanceDtoToIdentifiersSet")
  default Set<MediatedRequestInstanceIdentifier> mapInstanceDtoToIdentifiersSet(
    MediatedRequestInstance instance) {

    return instance.getIdentifiers().stream()
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

  default MediatedRequestInstance mapDtoInstance(MediatedRequestEntity mediatedRequestEntity) {
    return new MediatedRequestInstance()
      .title(mediatedRequestEntity.getInstanceTitle())
      .identifiers(mediatedRequestEntity.getInstanceIdentifiers() == null ? null :
        mediatedRequestEntity.getInstanceIdentifiers().stream()
          .map(this::entityIdentifierToDtoIdentifierInner)
          .toList());
  }

  default MediatedRequestInstanceIdentifiersInner entityIdentifierToDtoIdentifierInner(
    MediatedRequestInstanceIdentifier identifier) {

    return identifier == null ? null : new MediatedRequestInstanceIdentifiersInner()
      .identifierTypeId(identifier.getIdentifierTypeId().toString())
      .value(identifier.getValue());
  }

  default MediatedRequestItem mapDtoItem(MediatedRequestEntity mediatedRequestEntity) {
    return new MediatedRequestItem().barcode(mediatedRequestEntity.getItemBarcode());
  }

  @Named("ItemDtoToItemBarcode")
  default String mapItemDtoToItemBarcode(MediatedRequestItem item) {
    return item == null ? null : item.getBarcode();
  }

  @Named("RequesterDtoToRequesterFirstName")
  default String mapRequesterDtoToRequesterFirstName(MediatedRequestRequester requester) {
    return requester == null ? null : requester.getFirstName();
  }

  @Named("RequesterDtoToRequesterLastName")
  default String mapRequesterDtoToRequesterLastName(MediatedRequestRequester requester) {
    return requester == null ? null : requester.getLastName();
  }

  @Named("RequesterDtoToRequesterMiddleName")
  default String mapRequesterDtoToRequesterMiddleName(MediatedRequestRequester requester) {
    return requester == null ? null : requester.getMiddleName();
  }

  @Named("RequesterDtoToRequesterBarcode")
  default String mapRequesterDtoToRequesterBarcode(MediatedRequestRequester requester) {
    return requester == null ? null : requester.getBarcode();
  }

  default MediatedRequestRequester mapDtoRequester(MediatedRequestEntity mediatedRequestEntity) {
    return new MediatedRequestRequester()
      .firstName(mediatedRequestEntity.getRequesterFirstName())
      .lastName(mediatedRequestEntity.getRequesterLastName())
      .middleName(mediatedRequestEntity.getRequesterMiddleName())
      .barcode(mediatedRequestEntity.getRequesterBarcode());
  }

  @Named("ProxyDtoToRequesterFirstName")
  default String mapRequesterDtoToProxyFirstName(MediatedRequestProxy proxy) {
    return proxy == null ? null : proxy.getFirstName();
  }

  @Named("ProxyDtoToRequesterLastName")
  default String mapRequesterDtoToProxyLastName(MediatedRequestProxy proxy) {
    return proxy == null ? null : proxy.getLastName();
  }

  @Named("ProxyDtoToRequesterMiddleName")
  default String mapRequesterDtoToProxyMiddleName(MediatedRequestProxy proxy) {
    return proxy == null ? null : proxy.getMiddleName();
  }

  @Named("ProxyDtoToRequesterBarcode")
  default String mapRequesterDtoToProxyBarcode(MediatedRequestProxy proxy) {
    return proxy == null ? null : proxy.getBarcode();
  }

  default MediatedRequestProxy mapDtoProxy(MediatedRequestEntity mediatedRequestEntity) {
    return new MediatedRequestProxy()
      .firstName(mediatedRequestEntity.getProxyFirstName())
      .lastName(mediatedRequestEntity.getProxyLastName())
      .middleName(mediatedRequestEntity.getProxyMiddleName())
      .barcode(mediatedRequestEntity.getProxyBarcode());
  }

  @AfterMapping
  default void setIdentifierParentEntity(@MappingTarget MediatedRequestEntity mediatedRequest) {
    mediatedRequest.getInstanceIdentifiers()
      .forEach(identifier -> identifier.setMediatedRequest(mediatedRequest));
  }
}
