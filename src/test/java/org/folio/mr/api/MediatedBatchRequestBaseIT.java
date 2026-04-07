package org.folio.mr.api;

import static java.util.Collections.emptySet;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.awaitility.core.ThrowingRunnable;

import org.folio.mr.client.CirculationClient;
import org.folio.mr.client.SettingsClient;
import org.folio.mr.controller.delegate.BatchRequestsServiceDelegate;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.dto.SearchInstancesResponse;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserTenant;
import org.folio.mr.domain.dto.UserTenantsResponse;

abstract class MediatedBatchRequestBaseIT extends BaseIT {

  protected static final Duration MAX_AWAIT_TIMEOUT = Durations.FIVE_SECONDS;
  protected static final Duration AWAIT_POLL_INTERVAL = Durations.ONE_HUNDRED_MILLISECONDS;
  protected static final String REQUESTER_ID = "9812e24b-0a66-457a-832c-c5e789797e35";
  protected static final String SERVICE_POINT_ID = "a0ab0704-2d16-4326-a6cd-9b6c15983bac";
  protected static final String HOLDING_RECORD_ID = "7212fa7b-a06a-41ee-b908-50c8f1b37ebb";

  protected static void setupWiremockForSingleTenantRequestCreation(String tenant) {
    mockHelper.mockGetSettingEntries(tenant, settingsQuery(), emptySettingsResponse());
    mockHelper.mockGetUserTenants(tenant, emptyUserTenants());
    mockHelper.mockGetAllowedServicePoints(tenant, allowedServicePointsResponse());
    mockHelper.mockGetInventoryHoldingRecord(tenant, HOLDING_RECORD_ID);
    mockHelper.mockGetInventoryItemAny(tenant, HOLDING_RECORD_ID);
    mockHelper.mockPostCirculationRequestAny(tenant);
  }

  protected static void setupWiremockForEcsRequestCreation() {
    mockHelper.mockGetSettingEntries(TENANT_ID_CENTRAL, settingsQuery(), emptySettingsResponse());
    mockHelper.mockGetUserTenants(TENANT_ID_CENTRAL, userTenants(TENANT_ID_CENTRAL));
    mockHelper.mockGetConsortiumItemAny(TENANT_ID_CENTRAL, consortiumItem());
    mockHelper.mockPostEcsExternalRequestAny(TENANT_ID_CENTRAL);
    mockHelper.mockGetCirculationRequestByIdAny(TENANT_ID_COLLEGE, circulationRequest());
    mockHelper.mockGetCirculationRequestByIdAny(TENANT_ID_CENTRAL, circulationRequest());
  }

  protected static void setupWiremockForSecureTenantRequestCreation() {
    mockHelper.mockGetSettingEntries(TENANT_ID_SECURE, settingsQuery(), emptySettingsResponse());
    mockHelper.mockGetUserTenants(TENANT_ID_SECURE, userTenants(TENANT_ID_SECURE));
    mockHelper.mockGetConsortiumItemAny(TENANT_ID_CENTRAL, consortiumItem());
    mockHelper.mockGetUserById(TENANT_ID_SECURE, activeUser());
    mockHelper.mockGetSearchInstancesEmpty(TENANT_ID_SECURE, emptySearchInstances());
  }

  protected static void awaitUntilAsserted(ThrowingRunnable throwingRunnable) {
    Awaitility.await()
      .pollInterval(AWAIT_POLL_INTERVAL)
      .atMost(MAX_AWAIT_TIMEOUT)
      .untilAsserted(throwingRunnable);
  }

  protected static UserTenantsResponse userTenants(String tenant) {
    return new UserTenantsResponse()
      .totalRecords(1)
      .userTenants(List.of(
        new UserTenant()
          .id(UUID.randomUUID())
          .userId(UUID.fromString(USER_ID))
          .username("consortia-system-user")
          .tenantId(tenant)
          .centralTenantId(TENANT_ID_CENTRAL)
          .consortiumId(UUID.randomUUID())
      ));
  }

  protected static UserTenantsResponse emptyUserTenants() {
    return new UserTenantsResponse().totalRecords(0);
  }

  protected static CirculationClient.AllowedServicePoints allowedServicePointsResponse() {
    return new CirculationClient.AllowedServicePoints(
      emptySet(), Set.of(new ServicePoint().id(SERVICE_POINT_ID)), emptySet());
  }

  protected static String settingsQuery() {
    return BatchRequestsServiceDelegate.BATCH_REQUEST_ITEMS_VALIDATION_SETTING_FETCH_QUERY;
  }

  private static SearchInstancesResponse emptySearchInstances() {
    return new SearchInstancesResponse().totalRecords(0);
  }

  private static SettingsClient.SettingsEntries emptySettingsResponse() {
    return new SettingsClient.SettingsEntries(
      Collections.emptyList(),
      new SettingsClient.ResultInfo(0));
  }

  protected static User activeUser() {
    return new User().id(USER_ID).active(true);
  }

  protected static ConsortiumItem consortiumItem() {
    return new ConsortiumItem()
      .id("{{request.path.id}}")
      .tenantId(TENANT_ID_COLLEGE)
      .instanceId(UUID.randomUUID().toString())
      .id(HOLDING_RECORD_ID);
  }

  protected static Request circulationRequest() {
    return new Request()
      .id("{{request.path.id}}")
      .itemId(UUID.randomUUID().toString())
      .instanceId(UUID.randomUUID().toString())
      .holdingsRecordId(HOLDING_RECORD_ID)
      .requesterId(USER_ID)
      .pickupServicePointId(SERVICE_POINT_ID);
  }
}
