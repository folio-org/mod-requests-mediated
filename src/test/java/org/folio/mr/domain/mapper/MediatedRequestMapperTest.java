package org.folio.mr.domain.mapper;

import java.util.Date;
import java.util.UUID;

import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequestInstance;
import org.folio.mr.domain.dto.MediatedRequestInstanceIdentifiersInner;
import org.folio.mr.domain.dto.MediatedRequestItem;
import org.folio.mr.domain.dto.MediatedRequestProxy;
import org.folio.mr.domain.dto.MediatedRequestRequester;
import org.folio.mr.domain.dto.MediatedRequestSearchIndex;
import org.folio.mr.domain.dto.MediatedRequestSearchIndexCallNumberComponents;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MediatedRequestMapperTest {
  @Test
  void testDtoToEntityMapping() {
    MediatedRequestMapperImpl mapper = new MediatedRequestMapperImpl();
    var entity = mapper.mapDtoToEntity(buildMediatedRequest());

    Assertions.assertEquals("identifier-value",
      entity.getInstanceIdentifiers().stream().iterator().next().getValue());
    Assertions.assertEquals("12345", entity.getItemBarcode());
    Assertions.assertEquals("First", entity.getRequesterFirstName());
    Assertions.assertEquals("Last", entity.getRequesterLastName());
    Assertions.assertEquals("Middle", entity.getRequesterMiddleName());
    Assertions.assertEquals("123", entity.getRequesterBarcode());
    Assertions.assertEquals("ProxyFirst", entity.getProxyFirstName());
    Assertions.assertEquals("ProxyLast", entity.getProxyLastName());
    Assertions.assertEquals("ProxyMiddle", entity.getProxyMiddleName());
    Assertions.assertEquals("Proxy123", entity.getProxyBarcode());

    Assertions.assertEquals("F16.H37 A2 9001", entity.getCallNumber());
    Assertions.assertEquals("pre", entity.getCallNumberPrefix());
    Assertions.assertEquals("suf", entity.getCallNumberSuffix());
    Assertions.assertEquals("F 416 H37 A2 59001", entity.getShelvingOrder());
    Assertions.assertEquals("Circ Desk 1", entity.getPickupServicePointName());
  }

  @Test
  void testEntityToDtoMapping() {
    MediatedRequestMapperImpl mapper = new MediatedRequestMapperImpl();
    var entity = mapper.mapDtoToEntity(buildMediatedRequest());
    var dto = mapper.mapEntityToDto(entity);

    Assertions.assertEquals("identifier-value",
      dto.getInstance().getIdentifiers().get(0).getValue());
    Assertions.assertEquals("12345", dto.getItem().getBarcode());
    Assertions.assertEquals("First", dto.getRequester().getFirstName());
    Assertions.assertEquals("Last", dto.getRequester().getLastName());
    Assertions.assertEquals("Middle", dto.getRequester().getMiddleName());
    Assertions.assertEquals("123", dto.getRequester().getBarcode());
    Assertions.assertEquals("ProxyFirst", dto.getProxy().getFirstName());
    Assertions.assertEquals("ProxyLast", dto.getProxy().getLastName());
    Assertions.assertEquals("ProxyMiddle", dto.getProxy().getMiddleName());
    Assertions.assertEquals("Proxy123", dto.getProxy().getBarcode());

    Assertions.assertEquals("F16.H37 A2 9001", dto.getSearchIndex().getCallNumberComponents().getCallNumber());
    Assertions.assertEquals("pre", dto.getSearchIndex().getCallNumberComponents().getPrefix());
    Assertions.assertEquals("suf", dto.getSearchIndex().getCallNumberComponents().getSuffix());
    Assertions.assertEquals("F 416 H37 A2 59001", dto.getSearchIndex().getShelvingOrder());
    Assertions.assertEquals("Circ Desk 1", dto.getSearchIndex().getPickupServicePointName());
  }

  private MediatedRequest buildMediatedRequest() {
    return new MediatedRequest()
      .id(UUID.randomUUID().toString())
      .requestLevel(MediatedRequest.RequestLevelEnum.TITLE)
      .requestType(MediatedRequest.RequestTypeEnum.HOLD)
      .requestDate(new Date())
      .patronComments("")
      .requesterId(UUID.randomUUID().toString())
      .requester(new MediatedRequestRequester()
        .firstName("First")
        .lastName("Last")
        .middleName("Middle")
        .barcode("123"))
      .proxyUserId(UUID.randomUUID().toString())
      .proxy(new MediatedRequestProxy()
        .firstName("ProxyFirst")
        .lastName("ProxyLast")
        .middleName("ProxyMiddle")
        .barcode("Proxy123"))
      .instanceId(UUID.randomUUID().toString())
      .instance(new MediatedRequestInstance()
        .title("title")
        .addIdentifiersItem(new MediatedRequestInstanceIdentifiersInner()
          .identifierTypeId("a9b985ef-6833-4fb2-aaba-2b31b449fc7a")
          .value("identifier-value")))
      .holdingsRecordId(UUID.randomUUID().toString())
      .itemId(UUID.randomUUID().toString())
      .item(new MediatedRequestItem().barcode("12345"))
      .mediatedWorkflow("Private request")
      .mediatedRequestStatus(MediatedRequest.MediatedRequestStatusEnum.NEW)
      .status(MediatedRequest.StatusEnum.NEW_AWAITING_CONFIRMATION)
      .cancellationReasonId(UUID.randomUUID().toString())
      .cancelledByUserId(UUID.randomUUID().toString())
      .cancellationAdditionalInformation("info")
      .cancelledDate(new Date())
      .position(1)
      .fulfillmentPreference(MediatedRequest.FulfillmentPreferenceEnum.HOLD_SHELF)
      .deliveryAddressTypeId(null)
      .pickupServicePointId(UUID.randomUUID().toString())
      .searchIndex(new MediatedRequestSearchIndex()
        .callNumberComponents(new MediatedRequestSearchIndexCallNumberComponents()
          .callNumber("F16.H37 A2 9001")
          .prefix("pre")
          .suffix("suf"))
        .shelvingOrder("F 416 H37 A2 59001")
        .pickupServicePointName("Circ Desk 1"));
  }
}
