package org.folio.mr.util;

import static java.util.UUID.randomUUID;

import java.util.Date;

import org.folio.mr.domain.FulfillmentPreference;
import org.folio.mr.domain.MediatedRequestStatus;
import org.folio.mr.domain.RequestLevel;
import org.folio.mr.domain.RequestType;
import org.folio.mr.domain.entity.MediatedRequestEntity;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TestEntityBuilder {

  public static MediatedRequestEntity buildMediatedRequestForItemArrivalConfirmation() {
    return new MediatedRequestEntity()
      .withRequestDate(new Date())
      .withRequestLevel(RequestLevel.TITLE)
      .withRequestType(RequestType.HOLD)
      .withFulfillmentPreference(FulfillmentPreference.DELIVERY)
      .withInstanceId(randomUUID())
      .withInstanceTitle("test-title")
      .withItemId(randomUUID())
      .withItemBarcode("test-item-barcode")
      .withShelvingOrder("test-shelving-order")
      .withMediatedRequestStatus(MediatedRequestStatus.OPEN)
      .withMediatedWorkflow("Private request")
      .withMediatedRequestStep("In transit for approval")
      .withStatus("Open - In transit for approval")
      .withRequesterId(randomUUID())
      .withRequesterBarcode("test-requester-barcode")
      .withRequesterFirstName("First")
      .withRequesterLastName("Last");
  }
}
