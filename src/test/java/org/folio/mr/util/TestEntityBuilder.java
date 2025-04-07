package org.folio.mr.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.folio.mr.domain.FulfillmentPreference;
import org.folio.mr.domain.MediatedRequestStatus;
import org.folio.mr.domain.RequestLevel;
import org.folio.mr.domain.RequestType;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequest.MediatedRequestStatusEnum;
import org.folio.mr.domain.dto.MediatedRequestDeliveryAddress;
import org.folio.mr.domain.dto.MediatedRequestInstance;
import org.folio.mr.domain.dto.MediatedRequestItem;
import org.folio.mr.domain.dto.MediatedRequestPickupServicePoint;
import org.folio.mr.domain.dto.MediatedRequestRequester;
import org.folio.mr.domain.dto.MediatedRequestSearchIndex;
import org.folio.mr.domain.dto.MediatedRequestSearchIndexCallNumberComponents;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.domain.entity.MediatedRequestWorkflowLog;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestEntityBuilder {

  public static MediatedRequestEntity buildMediatedRequestEntity(MediatedRequest.StatusEnum status) {
    String[] statusAndStep = status.getValue().split(" - ");

    return new MediatedRequestEntity()
      .withRequestDate(new Date())
      .withRequestLevel(RequestLevel.TITLE)
      .withRequestType(RequestType.HOLD)
      .withFulfillmentPreference(FulfillmentPreference.DELIVERY)
      .withInstanceId(UUID.fromString("69640328-788e-43fc-9c3c-af39e243f3b7"))
      .withInstanceTitle("ABA Journal")
      .withHoldingsRecordId(UUID.fromString("0c45bb50-7c9b-48b0-86eb-178a494e25fe"))
      .withItemId(UUID.fromString("9428231b-dd31-4f70-8406-fe22fbdeabc2"))
      .withItemBarcode("A14837334314")
      .withConfirmedRequestId(UUID.randomUUID())
      .withMediatedRequestStatus(MediatedRequestStatus.fromValue(statusAndStep[0]))
      .withMediatedWorkflow("Private request")
      .withMediatedRequestStep(statusAndStep[1])
      .withStatus(status.getValue())
      .withRequesterId(UUID.fromString("9812e24b-0a66-457a-832c-c5e789797e35"))
      .withRequesterBarcode("111")
      .withRequesterFirstName("Requester")
      .withRequesterMiddleName("X")
      .withRequesterLastName("Mediated")
      .withCallNumberPrefix("PFX")
      .withCallNumber("CN")
      .withCallNumberSuffix("SFX");
  }

  public static MediatedRequest buildMediatedRequest(MediatedRequest.StatusEnum status) {
    String[] statusAndStep = status.getValue().split(" - ");

    return new MediatedRequest()
      .id(UUID.randomUUID().toString())
      .requestDate(new Date())
      .requestLevel(MediatedRequest.RequestLevelEnum.TITLE)
      .requestType(MediatedRequest.RequestTypeEnum.HOLD)
      .fulfillmentPreference(MediatedRequest.FulfillmentPreferenceEnum.DELIVERY)
      .instanceId("69640328-788e-43fc-9c3c-af39e243f3b7")
      .instance(new MediatedRequestInstance().title("ABA Journal"))
      .itemId("9428231b-dd31-4f70-8406-fe22fbdeabc2")
      .mediatedRequestStatus(MediatedRequestStatusEnum.fromValue(statusAndStep[0]))
      .confirmedRequestId("b41675a0-86f4-471b-9640-225b51f49e48")
      .pickupServicePoint(new MediatedRequestPickupServicePoint()
        .name("test name")
        .code("test code")
        .discoveryDisplayName("test discoveryDisplayName")
        .pickupLocation(true))
      .mediatedWorkflow("Private request")
      .mediatedRequestStep(statusAndStep[1])
      .status(status)
      .requesterId("9812e24b-0a66-457a-832c-c5e789797e35")
      .item(new MediatedRequestItem()
        .barcode("A14837334314")
        .callNumber("CN"))
      .requester(new MediatedRequestRequester()
        .barcode("111")
        .firstName("Requester")
        .middleName("X")
        .lastName("Mediated"))
      .searchIndex(new MediatedRequestSearchIndex()
        .callNumberComponents(new MediatedRequestSearchIndexCallNumberComponents()
        .prefix("PFX")
        .callNumber("CN")
        .suffix("SFX")))
      .deliveryAddress(new MediatedRequestDeliveryAddress()
        .region("test reqion")
        .countryId("test countryId")
        .city("test city")
        .addressLine1("address line 1")
        .addressLine2("address line 2")
        .postalCode("test postal code"));
  }

  @SneakyThrows
  public static MediatedRequestWorkflowLog buildMediatedRequestWorkflowLog(String datePattern,
    String date) {

    SimpleDateFormat formatter = new SimpleDateFormat(datePattern);
    Date actionDate = formatter.parse(date);
    MediatedRequestWorkflowLog mediatedRequestWorkflowLog = new MediatedRequestWorkflowLog();
    mediatedRequestWorkflowLog.setActionDate(actionDate);
    return mediatedRequestWorkflowLog;
  }
}
