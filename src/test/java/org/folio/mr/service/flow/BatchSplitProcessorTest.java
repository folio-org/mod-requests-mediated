package org.folio.mr.service.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.folio.mr.client.EcsTlrClient;
import org.folio.mr.client.ItemClient;
import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.EcsRequestExternal;
import org.folio.mr.domain.dto.EcsTlr;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.folio.mr.service.CirculationRequestService;
import org.folio.mr.service.SearchService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class BatchSplitProcessorTest {

  @Mock
  private MediatedBatchRequestSplitRepository splitRepository;
  @Mock
  private MediatedBatchRequestRepository batchRequestRepository;
  @Mock
  private SearchService searchService;
  @Mock
  private FolioExecutionContext executionContext;
  @Mock
  private EcsTlrClient ecsTlrClient;
  @Mock
  private SystemUserScopedExecutionService executionService;
  @Mock
  private CirculationRequestService circulationRequestService;
  @Mock
  private ItemClient itemClient;

  @Mock
  private BatchSplitContext context;

  @InjectMocks
  BatchSplitProcessor processor;

  @Test
  void execute_shouldThrowException_whenItemIdNotExistForSplitEntity() {
    when(context.getBatchSplitEntity()).thenReturn(new MediatedBatchRequestSplit());
    when(context.getDeploymentEnvType()).thenReturn(EnvironmentType.ECS);
    when(context.getBatchRequestId()).thenReturn(UUID.randomUUID());
    when(batchRequestRepository.findById(any(UUID.class))).thenReturn(Optional.of(new MediatedBatchRequest()));

    assertThrows(IllegalArgumentException.class, () -> processor.execute(context),
      "Item ID or Pickup Service Point ID is null. Skipping Item Request Creation");
  }

  @Test
  void execute_shouldCreateRequest_whenInEcsEnv() {
    var batch = new MediatedBatchRequest();
    batch.setId(UUID.randomUUID());
    var split = new MediatedBatchRequestSplit();
    split.setItemId(UUID.randomUUID());
    split.setPickupServicePointId(UUID.randomUUID());
    split.setRequesterId(UUID.randomUUID());

    var tenant = "central-tenant";
    var requestId = UUID.randomUUID().toString();
    var expectedEcsTlr = new EcsTlr().primaryRequestId(requestId);
    var expectedRequest = new Request().id(requestId).status(Request.StatusEnum.OPEN_NOT_YET_FILLED);
    var consortiumItem = new ConsortiumItem();

    when(context.getBatchSplitEntity()).thenReturn(split);
    when(context.getDeploymentEnvType()).thenReturn(EnvironmentType.ECS);
    when(context.getBatchRequestId()).thenReturn(batch.getId());
    when(batchRequestRepository.findById(any(UUID.class))).thenReturn(Optional.of(batch));
    when(executionContext.getTenantId()).thenReturn(tenant);
    when(ecsTlrClient.createEcsExternalRequest(any(EcsRequestExternal.class))).thenReturn(expectedEcsTlr);
    when(circulationRequestService.get(requestId)).thenReturn(expectedRequest);
    when(searchService.searchItem(any(String.class))).thenReturn(Optional.of(consortiumItem));

    lenient().when(executionService.executeSystemUserScoped(eq(tenant), any()))
      .thenAnswer(invocation -> invocation.<Callable<?>>getArgument(1).call());

    processor.execute(context);

    verify(splitRepository).save(any());
    assertEquals(Request.StatusEnum.OPEN_NOT_YET_FILLED.getValue(), split.getRequestStatus());
    assertEquals(expectedRequest.getId(), split.getConfirmedRequestId().toString());
  }

  @Test
  void execute_shouldCreateSingleTenantRequest_whenEnvTypeIsSecureTenant() {
    var batch = new MediatedBatchRequest();
    batch.setId(UUID.randomUUID());
    var split = new MediatedBatchRequestSplit();
    split.setItemId(UUID.randomUUID());
    split.setPickupServicePointId(UUID.randomUUID());
    split.setRequesterId(UUID.randomUUID());

    var requestId = UUID.randomUUID().toString();
    var expectedRequest = new Request().id(requestId).status(Request.StatusEnum.OPEN_AWAITING_PICKUP);

    var item = new org.folio.mr.domain.dto.Item();
    item.setInstanceId(UUID.randomUUID().toString());
    item.setHoldingsRecordId(UUID.randomUUID().toString());

    when(context.getBatchSplitEntity()).thenReturn(split);
    when(context.getDeploymentEnvType()).thenReturn(EnvironmentType.SECURE_TENANT);
    when(context.getBatchRequestId()).thenReturn(batch.getId());
    when(batchRequestRepository.findById(any(UUID.class))).thenReturn(Optional.of(batch));
    when(itemClient.get(any(String.class))).thenReturn(Optional.of(item));
    when(circulationRequestService.createItemRequest(any(Request.class))).thenReturn(expectedRequest);

    processor.execute(context);

    verify(splitRepository).save(split);
    assertEquals(Request.StatusEnum.OPEN_AWAITING_PICKUP.getValue(), split.getRequestStatus());
    assertEquals(requestId, split.getConfirmedRequestId().toString());
  }

  @Test
  void onStart_shouldSetStatusToInProgress_andSaveEntity() {
    var split = new MediatedBatchRequestSplit();
    split.setStatus(BatchRequestSplitStatus.PENDING);
    when(context.getBatchSplitEntity()).thenReturn(split);

    processor.onStart(context);

    assertEquals(BatchRequestSplitStatus.IN_PROGRESS, split.getStatus());
    verify(splitRepository).save(split);
  }

  @Test
  void onError_shouldSetStatusToFailed_andSaveEntity_withErrorDetails() {
    var split = new MediatedBatchRequestSplit();
    split.setId(UUID.randomUUID());
    when(context.getBatchSplitEntity()).thenReturn(split);

    var cause = new RuntimeException("Root cause");
    var ex = new Exception("Processing failed", cause);

    processor.onError(context, ex);

    assertEquals(BatchRequestSplitStatus.FAILED, split.getStatus());
    assertTrue(split.getErrorDetails().contains("Processing failed"));
    assertTrue(split.getErrorDetails().contains("Root cause"));
    verify(splitRepository).save(split);
  }

  @Test
  void onError_shouldSetErrorDetails_whenNoCause() {
    var split = new MediatedBatchRequestSplit();
    split.setId(UUID.randomUUID());
    when(context.getBatchSplitEntity()).thenReturn(split);

    var ex = new Exception("Simple error");

    processor.onError(context, ex);

    assertEquals(BatchRequestSplitStatus.FAILED, split.getStatus());
    assertEquals("Simple error", split.getErrorDetails());
    verify(splitRepository).save(split);
  }
}
