package org.folio.mr.service.flow.splits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.HoldingsRecord;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.exception.MediatedBatchRequestSplitNotFoundException;
import org.folio.mr.service.ConsortiumService;
import org.folio.mr.service.InventoryService;
import org.folio.mr.service.MediatedBatchRequestSplitService;
import org.folio.mr.service.MediatedBatchRequestsService;
import org.folio.mr.service.MediatedRequestsService;
import org.folio.mr.service.SearchService;
import org.folio.spring.FolioExecutionContext;

@ExtendWith(MockitoExtension.class)
class SecureTenantRequestHelperTest {

  @InjectMocks private SecureTenantRequestHelper helper;
  @Captor private ArgumentCaptor<MediatedBatchRequestDetailDto> splitCaptor;
  @Mock private MediatedBatchRequestsService batchRequestService;
  @Mock private MediatedBatchRequestSplitService batchRequestSplitService;
  @Mock private SearchService searchService;
  @Mock private FolioExecutionContext executionContext;
  @Mock private InventoryService inventoryService;
  @Mock private MediatedRequestsService mediatedRequestsService;
  @Mock private ConsortiumService consortiumService;
  @Mock private BatchSplitContext context;

  @BeforeEach
  void setUp() {
    helper.setBatchRequestsService(batchRequestService);
    helper.setBatchRequestSplitService(batchRequestSplitService);
  }

  @Test
  void execute_positive_shouldCreateMediatedRequestsWithSecureTenantInNonEcsEnvWithLocalItem() {
    var batchId = UUID.randomUUID();
    var splitId = UUID.randomUUID();
    var requesterId = UUID.randomUUID().toString();
    var pickupServicePointId = UUID.randomUUID().toString();
    var batch = new MediatedBatchRequestDto()
      .batchId(batchId.toString())
      .requesterId(requesterId);
    var itemId = UUID.randomUUID().toString();
    var holdingId = UUID.randomUUID().toString();
    var instanceId = UUID.randomUUID().toString();
    var split = new MediatedBatchRequestDetailDto()
      .batchId(batchId.toString())
      .itemId(itemId)
      .pickupServicePointId(pickupServicePointId)
      .requesterId(requesterId)
      .patronComments("comments");
    var savedMediatedRequest = new MediatedRequest().id(UUID.randomUUID().toString());
    var item = new Item().id(itemId).holdingsRecordId(holdingId);
    var holding = new HoldingsRecord().id(holdingId).instanceId(instanceId);

    var tenantId = "secure-tenant";
    when(executionContext.getTenantId()).thenReturn(tenantId);
    when(consortiumService.getCentralTenantId(tenantId)).thenReturn(Optional.empty());
    when(inventoryService.fetchItem(itemId)).thenReturn(item);
    when(inventoryService.fetchHolding(holdingId)).thenReturn(holding);

    when(context.getBatchSplitRequestId()).thenReturn(splitId);
    when(context.getBatchRequestId()).thenReturn(batchId);
    when(batchRequestSplitService.getById(splitId)).thenReturn(split);
    when(batchRequestService.getById(batchId)).thenReturn(batch);
    when(mediatedRequestsService.post(any(MediatedRequest.class))).thenReturn(savedMediatedRequest);
    var mediatedRequestCaptor = ArgumentCaptor.forClass(MediatedRequest.class);

    helper.createRequest(context);

    verify(mediatedRequestsService).post(mediatedRequestCaptor.capture());
    var mediatedRequest = mediatedRequestCaptor.getValue();
    assertEquals(itemId, mediatedRequest.getItemId());
    assertEquals(split.getPickupServicePointId(), mediatedRequest.getPickupServicePointId());
    assertEquals(instanceId, mediatedRequest.getInstanceId());

    verify(batchRequestSplitService).update(eq(splitId), splitCaptor.capture());
    assertEquals(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.COMPLETED,
      splitCaptor.getValue().getMediatedRequestStatus());
    assertEquals(savedMediatedRequest.getId(), splitCaptor.getValue().getConfirmedRequestId());
  }

  @Test
  void execute_positive_shouldCreateMediatedRequestsWithSecureTenantInEcsEnv() {
    var batchId = UUID.randomUUID();
    var splitId = UUID.randomUUID();
    var requesterId = UUID.randomUUID().toString();
    var pickupServicePointId = UUID.randomUUID().toString();
    var batch = new MediatedBatchRequestDto()
      .batchId(batchId.toString())
      .requesterId(requesterId);
    var itemId = UUID.randomUUID().toString();
    var instanceId = UUID.randomUUID().toString();
    var split = new MediatedBatchRequestDetailDto()
      .itemId(itemId)
      .pickupServicePointId(pickupServicePointId)
      .requesterId(requesterId);
    var savedMediatedRequest = new MediatedRequest().id(UUID.randomUUID().toString());

    var tenantId = "secure-tenant";
    when(executionContext.getTenantId()).thenReturn(tenantId);
    when(consortiumService.getCentralTenantId(tenantId)).thenReturn(Optional.of("central"));
    when(searchService.searchItem(itemId)).thenReturn(Optional.of(new ConsortiumItem().instanceId(instanceId)));

    when(context.getBatchSplitRequestId()).thenReturn(splitId);
    when(context.getBatchRequestId()).thenReturn(batchId);
    when(batchRequestSplitService.getById(splitId)).thenReturn(split);
    when(batchRequestService.getById(batchId)).thenReturn(batch);
    when(mediatedRequestsService.post(any(MediatedRequest.class))).thenReturn(savedMediatedRequest);
    var mediatedRequestCaptor = ArgumentCaptor.forClass(MediatedRequest.class);

    helper.createRequest(context);

    verify(mediatedRequestsService).post(mediatedRequestCaptor.capture());
    var mediatedRequest = mediatedRequestCaptor.getValue();
    assertEquals(itemId, mediatedRequest.getItemId());
    assertEquals(split.getPickupServicePointId(), mediatedRequest.getPickupServicePointId());
    assertEquals(instanceId, mediatedRequest.getInstanceId());

    verify(batchRequestSplitService).update(eq(splitId), splitCaptor.capture());
    assertEquals(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.COMPLETED,
      splitCaptor.getValue().getMediatedRequestStatus());
    assertEquals(savedMediatedRequest.getId(), splitCaptor.getValue().getConfirmedRequestId());
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
    var splitEntity = new MediatedBatchRequestDetailDto()
      .itemId(UUID.randomUUID().toString())
      .pickupServicePointId(UUID.randomUUID().toString())
      .requesterId(UUID.randomUUID().toString());
    when(context.getBatchRequestId()).thenReturn(batchId);
    when(context.getBatchSplitRequestId()).thenReturn(splitRequestId);
    when(batchRequestSplitService.getById(splitRequestId)).thenReturn(splitEntity);
    when(batchRequestService.getById(batchId)).thenThrow(new MediatedBatchRequestNotFoundException(batchId));

    assertThrows(MediatedBatchRequestNotFoundException.class, () -> helper.createRequest(context));
  }
}
