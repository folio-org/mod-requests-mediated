package org.folio.mr.service.flow.splits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.mr.client.CirculationClient;
import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.domain.RequestType;
import org.folio.mr.domain.dto.HoldingsRecord;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.exception.MediatedBatchRequestSplitNotFoundException;
import org.folio.mr.exception.MediatedBatchRequestValidationException;
import org.folio.mr.service.CirculationRequestService;
import org.folio.mr.service.InventoryService;
import org.folio.mr.service.MediatedBatchRequestSplitService;
import org.folio.mr.service.MediatedBatchRequestsService;

@ExtendWith(MockitoExtension.class)
class SingleTenantRequestHelperTest {

  @InjectMocks private SingleTenantRequestHelper helper;
  @Captor private ArgumentCaptor<MediatedBatchRequestDetailDto> splitCaptor;
  @Mock private MediatedBatchRequestSplitService batchRequestSplitService;
  @Mock private MediatedBatchRequestsService batchRequestService;
  @Mock private CirculationRequestService circulationRequestService;
  @Mock private InventoryService inventoryService;
  @Mock private BatchSplitContext context;

  @BeforeEach
  void setUp() {
    helper.setBatchRequestsService(batchRequestService);
    helper.setBatchRequestSplitService(batchRequestSplitService);
  }

  @Test
  void execute_positive_shouldCreateSingleTenantRequestWhenInSingleTenantEnv() {
    var batchId = UUID.randomUUID();
    var splitRequestId = UUID.randomUUID();
    var itemId = UUID.randomUUID().toString();
    var requesterId = UUID.randomUUID().toString();
    var pickupServicePointId = UUID.randomUUID().toString();
    var batch = new MediatedBatchRequestDto()
      .batchId(batchId.toString());
    var holdingId = UUID.randomUUID().toString();
    var instanceId = UUID.randomUUID().toString();
    var split = new MediatedBatchRequestDetailDto()
      .batchId(batchId.toString())
      .itemId(itemId)
      .pickupServicePointId(pickupServicePointId)
      .requesterId(requesterId);
    var servicePoint = new ServicePoint().id(pickupServicePointId);

    var requestId = UUID.randomUUID().toString();
    var createdRequest = new Request().id(requestId).status(Request.StatusEnum.OPEN_AWAITING_PICKUP);

    when(context.getBatchSplitRequestId()).thenReturn(splitRequestId);
    when(context.getBatchRequestId()).thenReturn(batchId);
    when(batchRequestSplitService.getById(splitRequestId)).thenReturn(split);
    when(batchRequestService.getById(batchId)).thenReturn(batch);
    when(circulationRequestService.getItemRequestAllowedServicePoints(UUID.fromString(requesterId), UUID.fromString(itemId)))
      .thenReturn(new CirculationClient.AllowedServicePoints(Set.of(), Set.of(), Set.of(servicePoint)));
    when(inventoryService.fetchItem(itemId)).thenReturn(new Item().holdingsRecordId(holdingId));
    when(inventoryService.fetchHolding(holdingId)).thenReturn(new HoldingsRecord().instanceId(instanceId));
    when(circulationRequestService.create(any(Request.class))).thenReturn(createdRequest);
    var valueCaptor = ArgumentCaptor.forClass(Request.class);

    helper.createRequest(context);

    verify(batchRequestSplitService).update(eq(splitRequestId), splitCaptor.capture());
    var savedSplit = splitCaptor.getValue();
    assertEquals(createdRequest.getStatus().getValue(), savedSplit.getRequestStatus());
    assertEquals(createdRequest.getId(), savedSplit.getConfirmedRequestId());
    assertEquals(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.COMPLETED, savedSplit.getMediatedRequestStatus());
    verify(circulationRequestService).create(valueCaptor.capture());
    assertEquals(holdingId, valueCaptor.getValue().getHoldingsRecordId());
    assertEquals(instanceId, valueCaptor.getValue().getInstanceId());
    assertEquals(RequestType.RECALL.getValue(), valueCaptor.getValue().getRequestType().getValue());
  }

  @Test
  void execute_negative_shouldThrowExceptionOnSingleTenantEnvWhenNoRequestTypeMatchesForGivenServicePointId() {
    var batchId = UUID.randomUUID();
    var splitRequestId = UUID.randomUUID();
    var split = new MediatedBatchRequestDetailDto()
      .batchId(batchId.toString())
      .itemId(UUID.randomUUID().toString())
      .pickupServicePointId(UUID.randomUUID().toString())
      .requesterId(UUID.randomUUID().toString());
    var batch = new MediatedBatchRequestDto().batchId(batchId.toString());

    when(context.getBatchSplitRequestId()).thenReturn(splitRequestId);
    when(context.getBatchRequestId()).thenReturn(batchId);
    when(batchRequestSplitService.getById(splitRequestId)).thenReturn(split);
    when(batchRequestService.getById(batchId)).thenReturn(batch);
    when(circulationRequestService.getItemRequestAllowedServicePoints(
      UUID.fromString(split.getRequesterId()), UUID.fromString(split.getItemId())))
      .thenReturn(new CirculationClient.AllowedServicePoints(Set.of(), Set.of(), Set.of()));

    assertThrows(MediatedBatchRequestValidationException.class, () -> helper.createRequest(context),
      "Not allowed to create Request for the given service point id.");

    verify(batchRequestSplitService, never()).update(any(UUID.class), any(MediatedBatchRequestDetailDto.class));
  }

  @Test
  void createRequest_negative_splitEntityNotFound() {
    var splitRequestId = UUID.randomUUID();
    when(context.getBatchSplitRequestId()).thenReturn(splitRequestId);
    when(batchRequestSplitService.getById(splitRequestId))
      .thenThrow(new MediatedBatchRequestSplitNotFoundException(splitRequestId));

    assertThrows(
      MediatedBatchRequestSplitNotFoundException.class,
      () -> helper.createRequest(context));
  }

  @Test
  void createRequest_negative_batchEntityNotFound() {
    var batchId = UUID.randomUUID();
    var splitRequestId = UUID.randomUUID();
    var split = new MediatedBatchRequestDetailDto()
      .batchId(batchId.toString())
      .itemId(UUID.randomUUID().toString())
      .pickupServicePointId(UUID.randomUUID().toString())
      .requesterId(UUID.randomUUID().toString());
    when(context.getBatchRequestId()).thenReturn(batchId);
    when(context.getBatchSplitRequestId()).thenReturn(splitRequestId);
    when(batchRequestSplitService.getById(splitRequestId)).thenReturn(split);
    when(batchRequestService.getById(batchId))
      .thenThrow(new MediatedBatchRequestNotFoundException(batchId));

    assertThrows(MediatedBatchRequestNotFoundException.class, () -> helper.createRequest(context));
  }
}
