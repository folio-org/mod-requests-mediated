package org.folio.mr.domain.dto;

import java.util.Date;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.folio.mr.domain.FulfillmentPreference;
import org.folio.mr.domain.RequestLevel;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@RequiredArgsConstructor
public class EcsRequestExternal {

  private String id;

  private final String instanceId;

  private final String requesterId;

  private final RequestLevel requestLevel;

  private final FulfillmentPreference fulfillmentPreference;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private final Date requestDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Date requestExpirationDate;

  private String patronComments;

  private String pickupServicePointId;

  private String itemId;

  private String holdingsRecordId;

  private String primaryRequestId;

  private String primaryRequestDcbTransactionId;

  private String primaryRequestTenantId;

  private String secondaryRequestId;

  private String secondaryRequestDcbTransactionId;

  private String secondaryRequestTenantId;

  public EcsRequestExternal withPatronComments(String patronComments) {
    this.patronComments = patronComments;
    return this;
  }

  public EcsRequestExternal withPickupServicePointId(String pickupServicePointId) {
    this.pickupServicePointId = pickupServicePointId;
    return this;
  }

  public EcsRequestExternal withItemId(String itemId) {
    this.itemId = itemId;
    return this;
  }

  public EcsRequestExternal withHoldingsRecordId(String holdingsRecordId) {
    this.holdingsRecordId = holdingsRecordId;
    return this;
  }

  public EcsRequestExternal withPrimaryRequestTenantId(String primaryRequestTenantId) {
    this.primaryRequestTenantId = primaryRequestTenantId;
    return this;
  }

  public EcsRequestExternal withRequestExpirationDate(Date requestExpirationDate) {
    this.requestExpirationDate = requestExpirationDate;
    return this;
  }

  public EcsRequestExternal withPrimaryRequestId(String primaryRequestId) {
    this.primaryRequestId = primaryRequestId;
    return this;
  }

  public EcsRequestExternal withPrimaryRequestDcbTransactionId(String primaryRequestDcbTransactionId) {
    this.primaryRequestDcbTransactionId = primaryRequestDcbTransactionId;
    return this;
  }

  public EcsRequestExternal withSecondaryRequestId(String secondaryRequestId) {
    this.secondaryRequestId = secondaryRequestId;
    return this;
  }

  public EcsRequestExternal withSecondaryRequestDcbTransactionId(String secondaryRequestDcbTransactionId) {
    this.secondaryRequestDcbTransactionId = secondaryRequestDcbTransactionId;
    return this;
  }

  public EcsRequestExternal withSecondaryRequestTenantId(String secondaryRequestTenantId) {
    this.secondaryRequestTenantId = secondaryRequestTenantId;
    return this;
  }
}
