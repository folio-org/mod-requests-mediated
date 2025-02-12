package org.folio.mr.service;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.CLOSED_CANCELLED;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.CLOSED_FILLED;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.NEW_AWAITING_CONFIRMATION;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_AWAITING_PICKUP;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_FOR_APPROVAL;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_ITEM_ARRIVED;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequest;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequestEntity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.folio.mr.client.SearchClient;
import org.folio.mr.domain.MediatedRequestStatus;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.EcsTlr;
import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.domain.entity.MediatedRequestStep;
import org.folio.mr.domain.entity.MediatedRequestWorkflow;
import org.folio.mr.domain.mapper.MediatedRequestMapper;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.impl.MediatedRequestActionsServiceImpl;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class MediatedRequestActionsServiceTest {
  private static final String INTERIM_SERVICE_POINT_ID = "32c6f0c7-26e4-4350-8c29-1e11c2e3efc4";
  private static final String INTERIM_SERVICE_POINT_NAME = "Interim service point";
  private static final String INTERIM_SERVICE_POINT_CODE = "interimsp";
  private static final String INTERIM_SERVICE_POINT_DISCOVERY_DISPLAY_NAME= "Interim service point";

  @Mock
  private MediatedRequestsRepository mediatedRequestsRepository;

  @Mock
  private MediatedRequestMapper mediatedRequestMapper;

  @Mock
  private InventoryService inventoryService;

  @Mock
  private CirculationRequestService circulationRequestService;

  @Mock
  private EcsRequestService ecsRequestService;

  @Mock
  private FolioExecutionContext folioExecutionContext;

  @Mock
  private SearchService searchService;

  @Mock
  private SearchClient searchClient;

  @Mock
  private SystemUserScopedExecutionService executionService;

  @InjectMocks
  private MediatedRequestActionsServiceImpl mediatedRequestActionsService;

  @Captor
  ArgumentCaptor<MediatedRequestEntity> mediatedRequestEntityCaptor;

  @Test
  void changeStatusToInTransitForApprovalSuccess() {
    UUID mediatedRequestId = UUID.randomUUID();
    MediatedRequestEntity initialRequest = buildMediatedRequestEntity(OPEN_NOT_YET_FILLED)
      .withId(mediatedRequestId);
    when(mediatedRequestsRepository.save(mediatedRequestEntityCaptor.capture())).thenReturn(null);

    mediatedRequestActionsService.changeStatusToInTransitForApproval(initialRequest);

    MediatedRequestEntity updatedRequest = mediatedRequestEntityCaptor.getValue();
    assertNotNull(updatedRequest);
    assertEquals(MediatedRequestStatus.OPEN, updatedRequest.getMediatedRequestStatus());
    assertEquals(OPEN_IN_TRANSIT_FOR_APPROVAL.getValue(), updatedRequest.getStatus());
    assertEquals(MediatedRequestStep.IN_TRANSIT_FOR_APPROVAL.getValue(), updatedRequest.getMediatedRequestStep());
  }

  @Test
  void successfulItemArrivalConfirmation() {
    UUID mediatedRequestId = UUID.randomUUID();
    MediatedRequestEntity initialRequest = buildMediatedRequestEntity(OPEN_IN_TRANSIT_FOR_APPROVAL)
      .withId(mediatedRequestId);
    MediatedRequestEntity updatedRequest = buildMediatedRequestEntity(OPEN_ITEM_ARRIVED)
      .withId(mediatedRequestId);
    MediatedRequest mappedRequest = buildMediatedRequest(OPEN_ITEM_ARRIVED);
    String itemBarcode = initialRequest.getItemBarcode();

    when(mediatedRequestsRepository.findRequestForItemArrivalConfirmation(itemBarcode))
      .thenReturn(Optional.of(initialRequest));
    when(mediatedRequestsRepository.save(any(MediatedRequestEntity.class)))
      .thenReturn(updatedRequest);
    when(mediatedRequestMapper.mapEntityToDto(any(MediatedRequestEntity.class)))
      .thenReturn(mappedRequest);
    when(circulationRequestService.get(anyString())).thenReturn(new Request());
    when(circulationRequestService.update(any(Request.class))).thenReturn(new Request());

    MediatedRequest result = mediatedRequestActionsService.confirmItemArrival(itemBarcode);

    verify(mediatedRequestsRepository).save(any(MediatedRequestEntity.class));
    assertThat(result.getStatus().getValue(), is("Open - Item arrived"));
    assertThat(result.getMediatedRequestStep(), is("Item arrived"));

    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    verify(circulationRequestService).update(requestCaptor.capture());
    verifyDeliveryInfoUpdatedUponArrival(requestCaptor.getValue(), mappedRequest);
  }

  @Test
  void confirmItemArrivalRequestNotFound() {
    when(mediatedRequestsRepository.findRequestForItemArrivalConfirmation(any(String.class)))
      .thenReturn(Optional.empty());

    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
      () -> mediatedRequestActionsService.confirmItemArrival("item-barcode"));
    assertThat(exception.getMessage(),
      is("Mediated request for arrival confirmation of item with barcode 'item-barcode' was not found"));
  }

  @Test
  void sendItemInTransitSuccess() {
    UUID mediatedRequestId = UUID.randomUUID();
    MediatedRequestEntity initialRequest = buildMediatedRequestEntity(OPEN_ITEM_ARRIVED)
      .withId(mediatedRequestId);
    MediatedRequestEntity updatedRequest = buildMediatedRequestEntity(OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT)
      .withId(mediatedRequestId);
    MediatedRequest mappedRequest = buildMediatedRequest(OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT);
    String itemBarcode = initialRequest.getItemBarcode();

    when(mediatedRequestsRepository.findRequestForSendingInTransit(itemBarcode))
      .thenReturn(Optional.of(initialRequest));
    when(mediatedRequestsRepository.save(any(MediatedRequestEntity.class)))
      .thenReturn(updatedRequest);
    when(mediatedRequestMapper.mapEntityToDto(any(MediatedRequestEntity.class)))
      .thenReturn(mappedRequest);

    MediatedRequest result = mediatedRequestActionsService.sendItemInTransit(itemBarcode);

    verify(mediatedRequestsRepository).save(any(MediatedRequestEntity.class));
    assertThat(result.getStatus().getValue(), is("Open - In transit to be checked out"));
    assertThat(result.getMediatedRequestStep(), is("In transit to be checked out"));
  }

  @Test
  void sendItemInTransitRequestNotFound() {
    when(mediatedRequestsRepository.findRequestForSendingInTransit(any(String.class)))
      .thenReturn(Optional.empty());

    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
      () -> mediatedRequestActionsService.sendItemInTransit("item-barcode"));
    assertThat(exception.getMessage(),
      is("Send item in transit: mediated request for item 'item-barcode' was not found"));
  }

  @Test
  void changeStatusToAwaitingPickupSuccess() {
    UUID mediatedRequestId = UUID.randomUUID();
    MediatedRequestEntity initialRequest = buildMediatedRequestEntity(OPEN_NOT_YET_FILLED)
      .withId(mediatedRequestId);
    when(mediatedRequestsRepository.save(mediatedRequestEntityCaptor.capture())).thenReturn(null);

    mediatedRequestActionsService.changeStatusToAwaitingPickup(initialRequest);

    MediatedRequestEntity updatedRequest = mediatedRequestEntityCaptor.getValue();
    assertNotNull(updatedRequest);
    assertEquals(MediatedRequestStatus.OPEN, updatedRequest.getMediatedRequestStatus());
    assertEquals(OPEN_AWAITING_PICKUP.getValue(), updatedRequest.getStatus());
    assertEquals(MediatedRequestStep.AWAITING_PICKUP.getValue(), updatedRequest.getMediatedRequestStep());
  }

  @Test
  void mediatedRequestConfirmationForLocalInstanceAndItem() {
    final UUID mediatedRequestId = randomUUID();
    final UUID instanceId = randomUUID();
    final UUID circulationRequestId = randomUUID();

    MediatedRequestEntity mediatedRequest = buildMediatedRequestEntity(NEW_AWAITING_CONFIRMATION)
      .withId(mediatedRequestId)
      .withInstanceId(instanceId);

    ConsortiumItem requestedItem = new ConsortiumItem().id(mediatedRequest.getItemId().toString());
    Request circulationRequest = new Request().id(circulationRequestId.toString());

    when(mediatedRequestsRepository.findById(mediatedRequestId))
      .thenReturn(Optional.of(mediatedRequest));
    when(inventoryService.fetchInstance(instanceId.toString()))
      .thenReturn(new Instance());
    when(folioExecutionContext.getTenantId())
      .thenReturn("consortium");
    when(searchService.searchItems(instanceId.toString(), "consortium"))
      .thenReturn(List.of(requestedItem));
    when(circulationRequestService.create(mediatedRequest, INTERIM_SERVICE_POINT_ID))
      .thenReturn(circulationRequest);

    mediatedRequestActionsService.confirm(mediatedRequestId);

    verify(mediatedRequestsRepository).save(mediatedRequest.withConfirmedRequestId(circulationRequestId));
    verifyNoInteractions(ecsRequestService);
  }

  @Test
  void mediatedRequestConfirmationForLocalInstanceAndRemoteItem() {
    final UUID mediatedRequestId = randomUUID();
    final UUID instanceId = randomUUID();
    final UUID ecsTlrId = randomUUID();
    final UUID primaryRequestId = randomUUID();

    MediatedRequestEntity mediatedRequest = buildMediatedRequestEntity(NEW_AWAITING_CONFIRMATION)
      .withId(mediatedRequestId)
      .withInstanceId(instanceId);

    EcsTlr ecsTlr = new EcsTlr()
      .id(ecsTlrId.toString())
      .primaryRequestId(primaryRequestId.toString());

    when(mediatedRequestsRepository.findById(mediatedRequestId))
      .thenReturn(Optional.of(mediatedRequest));
    when(inventoryService.fetchInstance(instanceId.toString()))
      .thenReturn(new Instance());
    when(folioExecutionContext.getTenantId())
      .thenReturn("consortium");
    when(searchService.searchItems(instanceId.toString(), "consortium"))
      .thenReturn(emptyList());
    when(ecsRequestService.create(mediatedRequest))
      .thenReturn(ecsTlr);
    when(circulationRequestService.get(primaryRequestId.toString()))
      .thenReturn(new Request().id(primaryRequestId.toString()));
    when(circulationRequestService.update(any(Request.class)))
      .thenReturn(new Request());

    mediatedRequestActionsService.confirm(mediatedRequestId);

    verify(mediatedRequestsRepository)
      .save(mediatedRequest.withConfirmedRequestId(primaryRequestId));
    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    verify(circulationRequestService).update(requestCaptor.capture());
    Request actualRequest = requestCaptor.getValue();
    assertNotNull(actualRequest.getRequesterId());
  }

  @Test
  void mediatedRequestConfirmationForRemoteInstanceAndItem() {
    final UUID mediatedRequestId = randomUUID();
    final UUID instanceId = randomUUID();
    final UUID ecsTlrId = randomUUID();
    final UUID primaryRequestId = randomUUID();

    MediatedRequestEntity mediatedRequest = buildMediatedRequestEntity(NEW_AWAITING_CONFIRMATION)
      .withId(mediatedRequestId)
      .withInstanceId(instanceId);

    EcsTlr ecsTlr = new EcsTlr()
      .id(ecsTlrId.toString())
      .primaryRequestId(primaryRequestId.toString());

    when(mediatedRequestsRepository.findById(mediatedRequestId))
      .thenReturn(Optional.of(mediatedRequest));
    when(inventoryService.fetchInstance(instanceId.toString()))
      .thenReturn(null);
    when(ecsRequestService.create(mediatedRequest))
      .thenReturn(ecsTlr);
    when(circulationRequestService.get(primaryRequestId.toString()))
      .thenReturn(new Request().id(primaryRequestId.toString()));
    when(circulationRequestService.update(any(Request.class)))
      .thenReturn(new Request());

    mediatedRequestActionsService.confirm(mediatedRequestId);

    verify(mediatedRequestsRepository)
      .save(mediatedRequest.withConfirmedRequestId(primaryRequestId));
    verify(inventoryService).fetchInstance(instanceId.toString());
    verifyNoMoreInteractions(inventoryService);
    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    verify(circulationRequestService).update(requestCaptor.capture());
    Request actualRequest = requestCaptor.getValue();
    assertNotNull(actualRequest.getRequesterId());
  }

  @Test
  void mediatedRequestDeclineWrongStatus() {
    UUID mediatedRequestId = randomUUID();
    MediatedRequestEntity mediatedRequest = buildMediatedRequestEntity(OPEN_ITEM_ARRIVED)
      .withId(mediatedRequestId);

    when(mediatedRequestsRepository.findById(mediatedRequestId))
      .thenReturn(Optional.of(mediatedRequest));

    RuntimeException exception = assertThrows(RuntimeException.class,
      () -> mediatedRequestActionsService.decline(mediatedRequestId));
    assertThat(exception.getMessage(),
      is("Mediated request status should be 'New - Awaiting conformation'"));
  }

  @Test
  void mediatedRequestDeclineSuccess() {
    UUID mediatedRequestId = randomUUID();
    MediatedRequestEntity mediatedRequest = buildMediatedRequestEntity(NEW_AWAITING_CONFIRMATION)
      .withId(mediatedRequestId);
    when(mediatedRequestsRepository.findById(mediatedRequestId))
      .thenReturn(Optional.of(mediatedRequest));

    mediatedRequestActionsService.decline(mediatedRequestId);

    verify(mediatedRequestsRepository).save(
      mediatedRequest
        .withMediatedRequestStatus(MediatedRequestStatus.CLOSED)
        .withStatus(MediatedRequest.StatusEnum.CLOSED_DECLINED.getValue())
        .withMediatedRequestStep(MediatedRequestStep.DECLINED.getValue())
        .withMediatedWorkflow(MediatedRequestWorkflow.PRIVATE_REQUEST.getValue())
    );
  }

  @Test
  void changeStatusToClosedFilled() {
    UUID mediatedRequestId = UUID.randomUUID();
    MediatedRequestEntity initialRequest = buildMediatedRequestEntity(OPEN_NOT_YET_FILLED)
      .withId(mediatedRequestId);
    when(mediatedRequestsRepository.save(mediatedRequestEntityCaptor.capture())).thenReturn(null);

    mediatedRequestActionsService.changeStatusToClosedFilled(initialRequest);

    MediatedRequestEntity updatedRequest = mediatedRequestEntityCaptor.getValue();
    assertNotNull(updatedRequest);
    assertEquals(MediatedRequestStatus.CLOSED, updatedRequest.getMediatedRequestStatus());
    assertEquals(CLOSED_FILLED.getValue(), updatedRequest.getStatus());
    assertEquals(MediatedRequestStep.FILLED.getValue(), updatedRequest.getMediatedRequestStep());
  }

  @Test
  void changeStatusToClosedCanceled() {
    UUID mediatedRequestId = UUID.randomUUID();
    MediatedRequestEntity initialRequest = buildMediatedRequestEntity(OPEN_NOT_YET_FILLED)
      .withId(mediatedRequestId);
    when(mediatedRequestsRepository.save(mediatedRequestEntityCaptor.capture())).thenReturn(null);
    Request request = new Request()
      .cancellationReasonId(UUID.randomUUID().toString())
      .cancelledDate(new Date())
      .cancelledByUserId(UUID.randomUUID().toString());

    mediatedRequestActionsService.changeStatusToClosedCanceled(initialRequest, request);

    MediatedRequestEntity updatedRequest = mediatedRequestEntityCaptor.getValue();
    assertNotNull(updatedRequest);
    assertEquals(request.getCancellationReasonId(), updatedRequest.getCancellationReasonId().toString());
    assertEquals(request.getCancelledDate(), updatedRequest.getCancelledDate());
    assertEquals(request.getCancelledByUserId(), updatedRequest.getCancelledByUserId().toString());
    assertEquals(MediatedRequestStatus.CLOSED, updatedRequest.getMediatedRequestStatus());
    assertEquals(CLOSED_CANCELLED.getValue(), updatedRequest.getStatus());
    assertEquals(MediatedRequestStep.CANCELLED.getValue(), updatedRequest.getMediatedRequestStep());
  }

  private void verifyDeliveryInfoUpdatedUponArrival(Request request,
    MediatedRequest mediatedRequest) {

    assertEquals(mediatedRequest.getFulfillmentPreference().getValue(),
      request.getFulfillmentPreference().getValue());
    assertEquals(mediatedRequest.getPickupServicePointId(), request.getPickupServicePointId());

    assertNotNull(request.getDeliveryAddress());
    assertEquals(mediatedRequest.getDeliveryAddress().getRegion(),
      request.getDeliveryAddress().getRegion());
    assertEquals(mediatedRequest.getDeliveryAddress().getCity(),
      request.getDeliveryAddress().getCity());
    assertEquals(mediatedRequest.getDeliveryAddress().getCountryId(),
      request.getDeliveryAddress().getCountryId());
    assertEquals(mediatedRequest.getDeliveryAddress().getAddressTypeId(),
      request.getDeliveryAddress().getAddressTypeId());
    assertEquals(mediatedRequest.getDeliveryAddress().getAddressLine1(),
      request.getDeliveryAddress().getAddressLine1());
    assertEquals(mediatedRequest.getDeliveryAddress().getAddressLine2(),
      request.getDeliveryAddress().getAddressLine2());
    assertEquals(mediatedRequest.getDeliveryAddress().getPostalCode(),
      request.getDeliveryAddress().getPostalCode());

    assertEquals(mediatedRequest.getPickupServicePoint().getName(),
      request.getPickupServicePoint().getName());
    assertEquals(mediatedRequest.getPickupServicePoint().getCode(),
      request.getPickupServicePoint().getCode());
    assertEquals(mediatedRequest.getPickupServicePoint().getDiscoveryDisplayName(),
      request.getPickupServicePoint().getDiscoveryDisplayName());
    assertEquals(mediatedRequest.getPickupServicePoint().getPickupLocation(),
      request.getPickupServicePoint().getPickupLocation());
  }
}
