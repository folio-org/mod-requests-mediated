package org.folio.mr.service.flow.splits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.mr.client.EcsExternalTlrClient;
import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.EcsRequestExternal;
import org.folio.mr.domain.dto.EcsTlr;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.exception.MediatedBatchRequestSplitNotFoundException;
import org.folio.mr.service.CirculationRequestService;
import org.folio.mr.service.MediatedBatchRequestSplitService;
import org.folio.mr.service.MediatedBatchRequestsService;
import org.folio.mr.service.SearchService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;

@ExtendWith(MockitoExtension.class)
class EcsRequestHelperTest {

  @InjectMocks private EcsRequestHelper helper;
  @Captor private ArgumentCaptor<MediatedBatchRequestDetailDto> splitCaptor;
  @Mock private MediatedBatchRequestSplitService batchRequestSplitService;
  @Mock private MediatedBatchRequestsService batchRequestService;
  @Mock private SearchService searchService;
  @Mock private FolioExecutionContext executionContext;
  @Mock private EcsExternalTlrClient ecsTlrClient;
  @Mock private SystemUserScopedExecutionService executionService;
  @Mock private CirculationRequestService circulationRequestService;
  @Mock private BatchSplitContext context;

  @BeforeEach
  void setUp() {
    helper.setBatchRequestsService(batchRequestService);
    helper.setBatchRequestSplitService(batchRequestSplitService);
  }

  @Test
  void createRequest_positive_shouldCreateRequestWhenInEcsEnv() {
    var batchId = UUID.randomUUID();
    var splitRequestId = UUID.randomUUID();
    var splitItemId = UUID.randomUUID().toString();
    var batch = new MediatedBatchRequestDto().batchId(batchId.toString());
    var split = new MediatedBatchRequestDetailDto()
      .batchId(batchId.toString())
      .itemId(splitItemId)
      .pickupServicePointId(UUID.randomUUID().toString())
      .requesterId(UUID.randomUUID().toString())
      .patronComments("patron comments");

    var tenant = "central-tenant";
    var requestId = UUID.randomUUID().toString();
    var expectedEcsTlr = new EcsTlr().primaryRequestId(requestId);
    var expectedRequest = new Request().id(requestId).status(Request.StatusEnum.OPEN_NOT_YET_FILLED);
    var consortiumItem = new ConsortiumItem()
      .instanceId(UUID.randomUUID().toString())
      .holdingsRecordId(UUID.randomUUID().toString());

    when(context.getBatchSplitRequestId()).thenReturn(splitRequestId);
    when(context.getBatchRequestId()).thenReturn(batchId);
    when(batchRequestSplitService.getById(splitRequestId)).thenReturn(split);
    when(batchRequestService.getById(batchId)).thenReturn(batch);
    when(executionContext.getTenantId()).thenReturn(tenant);
    when(ecsTlrClient.createEcsExternalRequest(any(EcsRequestExternal.class))).thenReturn(expectedEcsTlr);
    when(circulationRequestService.get(requestId)).thenReturn(expectedRequest);
    when(searchService.searchItem(splitItemId)).thenReturn(Optional.of(consortiumItem));
    lenient().when(executionService.executeSystemUserScoped(eq(tenant), any()))
      .thenAnswer(invocation -> invocation.<Callable<?>>getArgument(1).call());
    var ecsRequestCaptor = ArgumentCaptor.forClass(EcsRequestExternal.class);

    helper.createRequest(context);

    verify(batchRequestSplitService).update(eq(splitRequestId), splitCaptor.capture());
    var savedSplit = splitCaptor.getValue();
    assertEquals(Request.StatusEnum.OPEN_NOT_YET_FILLED.getValue(), savedSplit.getRequestStatus());
    assertEquals(expectedRequest.getId(), savedSplit.getConfirmedRequestId());
    assertEquals(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.COMPLETED, savedSplit.getMediatedRequestStatus());
    verify(ecsTlrClient).createEcsExternalRequest(ecsRequestCaptor.capture());
    var ecsRequest = ecsRequestCaptor.getValue();
    assertEquals("Item", ecsRequest.getRequestLevel().getValue());
    assertEquals("Hold Shelf", ecsRequest.getFulfillmentPreference().getValue());
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
    var splitEntity = new MediatedBatchRequestDetailDto().itemId(UUID.randomUUID().toString());
    when(context.getBatchRequestId()).thenReturn(batchId);
    when(context.getBatchSplitRequestId()).thenReturn(splitRequestId);
    when(batchRequestSplitService.getById(splitRequestId)).thenReturn(splitEntity);
    when(batchRequestService.getById(batchId)).thenThrow(new MediatedBatchRequestNotFoundException(batchId));

    assertThrows(MediatedBatchRequestNotFoundException.class, () -> helper.createRequest(context));
  }
}
