package org.folio.mr.domain.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

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
import org.folio.mr.domain.dto.MediatedRequestSearchIndex;
import org.folio.mr.domain.dto.MediatedRequestSearchIndexCallNumberComponents;
import org.folio.mr.domain.dto.Metadata;
import org.junit.jupiter.api.Test;

class MediatedRequestMapperTest {
  private static final Date CURRENT_DATE = new Date();
  private static final Date CANCELLED_DATE = new Date();
  private static final String REQUESTER_ID = UUID.randomUUID().toString();
  private static final String PROXY_ID = UUID.randomUUID().toString();
  private static final String INSTANCE_ID = UUID.randomUUID().toString();
  private static final String HOLDINGS_RECORD_ID = UUID.randomUUID().toString();
  private static final String ITEM_ID = UUID.randomUUID().toString();
  private static final String CANCELLATION_REASON_ID = UUID.randomUUID().toString();
  private static final String CANCELLED_BY_USER_ID = UUID.randomUUID().toString();
  private static final String DELIVERY_ADDRESS_TYPE_ID = UUID.randomUUID().toString();
  private static final String PICKUP_SERVICE_POINT_ID = UUID.randomUUID().toString();
  private static final String CONFIRMED_REQUEST_ID = UUID.randomUUID().toString();

  // Metadata
  private static final Date CREATED_DATE = new Date();
  private static final String CREATED_BY_USER_ID = UUID.randomUUID().toString();
  private static final Date UPDATED_DATE = new Date();
  private static final String UPDATED_BY_USER_ID = UUID.randomUUID().toString();

  @Test
  void testDtoToEntityMapping() {
    MediatedRequestMapperImpl mapper = new MediatedRequestMapperImpl();
    var entity = mapper.mapDtoToEntity(buildMediatedRequest());

    assertEquals(RequestLevel.TITLE, entity.getRequestLevel());
    assertEquals(RequestType.HOLD, entity.getRequestType());
    assertEquals(CURRENT_DATE, entity.getRequestDate());
    assertEquals("comment", entity.getPatronComments());
    assertEquals(REQUESTER_ID, entity.getRequesterId().toString());
    assertEquals("First", entity.getRequesterFirstName());
    assertEquals("Last", entity.getRequesterLastName());
    assertEquals("Middle", entity.getRequesterMiddleName());
    assertEquals("123", entity.getRequesterBarcode());
    assertEquals(PROXY_ID, entity.getProxyUserId().toString());
    assertEquals("ProxyFirst", entity.getProxyFirstName());
    assertEquals("ProxyLast", entity.getProxyLastName());
    assertEquals("ProxyMiddle", entity.getProxyMiddleName());
    assertEquals("Proxy123", entity.getProxyBarcode());
    assertEquals(INSTANCE_ID, entity.getInstanceId().toString());
    assertEquals("title", entity.getInstanceTitle());
    assertEquals("identifier-value",
      entity.getInstanceIdentifiers().stream().iterator().next().getValue());
    assertEquals(HOLDINGS_RECORD_ID, entity.getHoldingsRecordId().toString());
    assertEquals(ITEM_ID, entity.getItemId().toString());
    assertEquals("12345", entity.getItemBarcode());
    assertEquals("Private request", entity.getMediatedWorkflow());
    assertEquals(MediatedRequestStatus.NEW, entity.getMediatedRequestStatus());
    assertEquals("Awaiting confirmation", entity.getMediatedRequestStep());
    assertEquals("New - Awaiting confirmation", entity.getStatus());
    assertEquals(CANCELLATION_REASON_ID, entity.getCancellationReasonId().toString());
    assertEquals(CANCELLED_BY_USER_ID, entity.getCancelledByUserId().toString());
    assertEquals("info", entity.getCancellationAdditionalInformation());
    assertEquals(CANCELLED_DATE, entity.getCancelledDate());
    assertEquals(1, entity.getPosition());
    assertEquals(FulfillmentPreference.HOLD_SHELF, entity.getFulfillmentPreference());
    assertEquals(DELIVERY_ADDRESS_TYPE_ID, entity.getDeliveryAddressTypeId().toString());
    assertEquals(PICKUP_SERVICE_POINT_ID, entity.getPickupServicePointId().toString());
    assertEquals(CONFIRMED_REQUEST_ID, entity.getConfirmedRequestId().toString());
    // Search index
    assertEquals("F16.H37 A2 9001", entity.getCallNumber());
    assertEquals("pre", entity.getCallNumberPrefix());
    assertEquals("suf", entity.getCallNumberSuffix());
    assertEquals("F 416 H37 A2 59001", entity.getShelvingOrder());
    assertEquals("Circ Desk 1", entity.getPickupServicePointName());
  }

  @Test
  void testEntityToDtoMapping() {
    MediatedRequestMapperImpl mapper = new MediatedRequestMapperImpl();
    var entity = mapper.mapDtoToEntity(buildMediatedRequest());

    // Since metadata fields are now ignored in DTO-to-Entity mapping (managed by JPA auditing),
    // we need to manually set them on the entity to test Entity-to-DTO mapping
    entity.setCreatedDate(new Timestamp(CREATED_DATE.getTime()));
    entity.setCreatedByUserId(UUID.fromString(CREATED_BY_USER_ID));
    entity.setCreatedByUsername("created-by");
    entity.setUpdatedDate(new Timestamp(UPDATED_DATE.getTime()));
    entity.setUpdatedByUserId(UUID.fromString(UPDATED_BY_USER_ID));
    entity.setUpdatedByUsername("updated-by");

    var dto = mapper.mapEntityToDto(entity);

    assertEquals(MediatedRequest.RequestLevelEnum.TITLE, dto.getRequestLevel());
    assertEquals(MediatedRequest.RequestTypeEnum.HOLD, dto.getRequestType());
    assertEquals(CURRENT_DATE, dto.getRequestDate());
    assertEquals(REQUESTER_ID, dto.getRequesterId());
    assertEquals("12345", dto.getItem().getBarcode());
    assertEquals("First", dto.getRequester().getFirstName());
    assertEquals("Last", dto.getRequester().getLastName());
    assertEquals("Middle", dto.getRequester().getMiddleName());
    assertEquals(PROXY_ID, dto.getProxyUserId());
    assertEquals("ProxyFirst", dto.getProxy().getFirstName());
    assertEquals("ProxyLast", dto.getProxy().getLastName());
    assertEquals("ProxyMiddle", dto.getProxy().getMiddleName());
    assertEquals("Proxy123", dto.getProxy().getBarcode());
    assertEquals(INSTANCE_ID, dto.getInstanceId());
    assertEquals("title", dto.getInstance().getTitle());
    assertEquals("identifier-value",
      dto.getInstance().getIdentifiers().get(0).getValue());
    assertEquals(HOLDINGS_RECORD_ID, dto.getHoldingsRecordId());
    assertEquals(ITEM_ID, dto.getItemId());
    assertEquals("12345", dto.getItem().getBarcode());
    assertEquals("Private request", dto.getMediatedWorkflow());
    assertEquals("Awaiting confirmation", dto.getMediatedRequestStep());
    assertEquals(MediatedRequest.StatusEnum.NEW_AWAITING_CONFIRMATION, dto.getStatus());
    assertEquals(CANCELLATION_REASON_ID, dto.getCancellationReasonId());
    assertEquals(CANCELLED_BY_USER_ID, dto.getCancelledByUserId());
    assertEquals("info", dto.getCancellationAdditionalInformation());
    assertEquals(CANCELLED_DATE, dto.getCancelledDate());
    assertEquals(1, dto.getPosition());
    assertEquals(MediatedRequest.FulfillmentPreferenceEnum.HOLD_SHELF, dto.getFulfillmentPreference());
    assertEquals(DELIVERY_ADDRESS_TYPE_ID, dto.getDeliveryAddressTypeId());
    assertEquals(PICKUP_SERVICE_POINT_ID, dto.getPickupServicePointId());
    // Search index
    assertEquals("F16.H37 A2 9001", dto.getSearchIndex().getCallNumberComponents().getCallNumber());
    assertEquals("pre", dto.getSearchIndex().getCallNumberComponents().getPrefix());
    assertEquals("suf", dto.getSearchIndex().getCallNumberComponents().getSuffix());
    assertEquals("F 416 H37 A2 59001", dto.getSearchIndex().getShelvingOrder());
    assertEquals("Circ Desk 1", dto.getSearchIndex().getPickupServicePointName());
  }

  @Test
  void testEntityToDtoMappingWithNullInstance() {
    MediatedRequestMapperImpl mapper = new MediatedRequestMapperImpl();

    var mediatedRequest = buildMediatedRequest();
    mediatedRequest.setInstance(null);

    var entity = mapper.mapDtoToEntity(mediatedRequest);
    var dto = mapper.mapEntityToDto(entity);

    assertEquals(0, dto.getInstance().getIdentifiers().size());
  }

  private MediatedRequest buildMediatedRequest() {
    return new MediatedRequest()
      .id(UUID.randomUUID().toString())
      .requestLevel(MediatedRequest.RequestLevelEnum.TITLE)
      .requestType(MediatedRequest.RequestTypeEnum.HOLD)
      .requestDate(CURRENT_DATE)
      .patronComments("comment")
      .requesterId(REQUESTER_ID)
      .requester(new MediatedRequestRequester()
        .firstName("First")
        .lastName("Last")
        .middleName("Middle")
        .barcode("123"))
      .proxyUserId(PROXY_ID)
      .proxy(new MediatedRequestProxy()
        .firstName("ProxyFirst")
        .lastName("ProxyLast")
        .middleName("ProxyMiddle")
        .barcode("Proxy123"))
      .instanceId(INSTANCE_ID)
      .instance(new MediatedRequestInstance()
        .title("title")
        .addIdentifiersItem(new MediatedRequestInstanceIdentifiersInner()
          .identifierTypeId("a9b985ef-6833-4fb2-aaba-2b31b449fc7a")
          .value("identifier-value")))
      .holdingsRecordId(HOLDINGS_RECORD_ID)
      .itemId(ITEM_ID)
      .item(new MediatedRequestItem().barcode("12345"))
      .mediatedWorkflow("Private request")
      .mediatedRequestStatus(MediatedRequest.MediatedRequestStatusEnum.NEW)
      .mediatedRequestStep("Awaiting confirmation")
      .status(MediatedRequest.StatusEnum.NEW_AWAITING_CONFIRMATION)
      .cancellationReasonId(CANCELLATION_REASON_ID)
      .cancelledByUserId(CANCELLED_BY_USER_ID)
      .cancellationAdditionalInformation("info")
      .cancelledDate(CANCELLED_DATE)
      .position(1)
      .fulfillmentPreference(MediatedRequest.FulfillmentPreferenceEnum.HOLD_SHELF)
      .deliveryAddressTypeId(DELIVERY_ADDRESS_TYPE_ID)
      .pickupServicePointId(PICKUP_SERVICE_POINT_ID)
      .confirmedRequestId(CONFIRMED_REQUEST_ID)
      .searchIndex(new MediatedRequestSearchIndex()
        .callNumberComponents(new MediatedRequestSearchIndexCallNumberComponents()
          .callNumber("F16.H37 A2 9001")
          .prefix("pre")
          .suffix("suf"))
        .shelvingOrder("F 416 H37 A2 59001")
        .pickupServicePointName("Circ Desk 1"))
      .metadata(new Metadata()
        .createdDate(CREATED_DATE)
        .createdByUserId(CREATED_BY_USER_ID)
        .createdByUsername("created-by")
        .updatedDate(UPDATED_DATE)
        .updatedByUserId(UPDATED_BY_USER_ID)
        .updatedByUsername("updated-by"));
  }
}
