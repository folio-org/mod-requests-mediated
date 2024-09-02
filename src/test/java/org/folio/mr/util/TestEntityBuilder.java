package org.folio.mr.util;

import java.util.Date;
import java.util.UUID;

import org.folio.mr.domain.FulfillmentPreference;
import org.folio.mr.domain.MediatedRequestStatus;
import org.folio.mr.domain.RequestLevel;
import org.folio.mr.domain.RequestType;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequest.MediatedRequestStatusEnum;
import org.folio.mr.domain.dto.MediatedRequestInstance;
import org.folio.mr.domain.dto.MediatedRequestItem;
import org.folio.mr.domain.dto.MediatedRequestRequester;
import org.folio.mr.domain.dto.MediatedRequestSearchIndex;
import org.folio.mr.domain.dto.MediatedRequestSearchIndexCallNumberComponents;
import org.folio.mr.domain.entity.MediatedRequestEntity;

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
      .withItemId(UUID.fromString("9428231b-dd31-4f70-8406-fe22fbdeabc2"))
      .withItemBarcode("A14837334314")
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
        .suffix("SFX")));
  }
}
