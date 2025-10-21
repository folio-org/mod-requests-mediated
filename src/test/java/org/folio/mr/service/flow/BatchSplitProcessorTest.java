package org.folio.mr.service.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.folio.mr.client.CirculationClient;
import org.folio.mr.client.EcsExternalTlrClient;
import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.EcsRequestExternal;
import org.folio.mr.domain.dto.EcsTlr;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.exception.MediatedBatchRequestValidationException;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.folio.mr.service.CirculationRequestService;
import org.folio.mr.service.SearchService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
  private EcsExternalTlrClient ecsTlrClient;
  @Mock
  private SystemUserScopedExecutionService executionService;
  @Mock
  private CirculationRequestService circulationRequestService;

  private final ArgumentCaptor<MediatedBatchRequestSplit> splitCaptor =
    ArgumentCaptor.forClass(MediatedBatchRequestSplit.class);

  @Mock
  private BatchSplitContext context;

  @InjectMocks
  BatchSplitProcessor processor;

  @Test
  void execute_positive_shouldCreateRequestWhenInEcsEnv() {
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

    when(context.getBatchSplitEntity()).thenReturn(split);
    when(context.getDeploymentEnvType()).thenReturn(EnvironmentType.ECS);
    when(context.getBatchRequestId()).thenReturn(batch.getId());
    when(batchRequestRepository.findById(any(UUID.class))).thenReturn(Optional.of(batch));
    when(executionContext.getTenantId()).thenReturn(tenant);
    when(ecsTlrClient.createEcsExternalRequest(any(EcsRequestExternal.class))).thenReturn(expectedEcsTlr);
    when(circulationRequestService.get(requestId)).thenReturn(expectedRequest);
    when(searchService.searchItem(any(String.class))).thenReturn(Optional.of(new ConsortiumItem()));
    lenient().when(executionService.executeSystemUserScoped(eq(tenant), any()))
      .thenAnswer(invocation -> invocation.<Callable<?>>getArgument(1).call());
    var ecsRequestCaptor = ArgumentCaptor.forClass(EcsRequestExternal.class);

    processor.execute(context);

    verify(splitRepository).save(splitCaptor.capture());
    var savedSplit = splitCaptor.getValue();
    assertEquals(Request.StatusEnum.OPEN_NOT_YET_FILLED.getValue(), savedSplit.getRequestStatus());
    assertEquals(expectedRequest.getId(), savedSplit.getConfirmedRequestId().toString());
    assertEquals(BatchRequestSplitStatus.COMPLETED, savedSplit.getStatus());
    verify(ecsTlrClient).createEcsExternalRequest(ecsRequestCaptor.capture());
    var ecsRequest = ecsRequestCaptor.getValue();
    assertEquals("Item", ecsRequest.getRequestLevel().getValue());
    assertEquals("Hold Shelf", ecsRequest.getFulfillmentPreference().getValue());
  }

  @Test
  void execute_positive_shouldCreateSingleTenantRequestWhenInSingleTenantEnv() {
    var batch = new MediatedBatchRequest();
    batch.setId(UUID.randomUUID());
    var split = new MediatedBatchRequestSplit();
    split.setItemId(UUID.randomUUID());
    split.setPickupServicePointId(UUID.randomUUID());
    split.setRequesterId(UUID.randomUUID());
    var servicePoint = new ServicePoint().id(split.getPickupServicePointId().toString());

    var requestId = UUID.randomUUID().toString();
    var createdRequest = new Request().id(requestId).status(Request.StatusEnum.OPEN_AWAITING_PICKUP);

    when(context.getBatchSplitEntity()).thenReturn(split);
    when(context.getDeploymentEnvType()).thenReturn(EnvironmentType.SINGLE_TENANT);
    when(context.getBatchRequestId()).thenReturn(batch.getId());
    when(batchRequestRepository.findById(any(UUID.class))).thenReturn(Optional.of(batch));
    when(circulationRequestService.getItemRequestAllowedServicePoints(split.getRequesterId(), split.getItemId()))
      .thenReturn(new CirculationClient.AllowedServicePoints(Set.of(), Set.of(), Set.of(servicePoint)));
    when(circulationRequestService.create(any(Request.class))).thenReturn(createdRequest);

    processor.execute(context);

    verify(splitRepository).save(splitCaptor.capture());
    var savedSplit = splitCaptor.getValue();
    assertEquals(createdRequest.getStatus().getValue(), savedSplit.getRequestStatus());
    assertEquals(createdRequest.getId(), savedSplit.getConfirmedRequestId().toString());
    assertEquals(BatchRequestSplitStatus.COMPLETED, savedSplit.getStatus());
  }

  @Test
  void execute_negative_shouldThrowExceptionOnSingleTenantEnvWhenNoRequestTypeMatchesForGivenServicePointId() {
    var batch = new MediatedBatchRequest();
    batch.setId(UUID.randomUUID());
    var split = new MediatedBatchRequestSplit();
    split.setItemId(UUID.randomUUID());
    split.setPickupServicePointId(UUID.randomUUID());
    split.setRequesterId(UUID.randomUUID());

    when(context.getBatchSplitEntity()).thenReturn(split);
    when(context.getDeploymentEnvType()).thenReturn(EnvironmentType.SINGLE_TENANT);
    when(context.getBatchRequestId()).thenReturn(batch.getId());
    when(batchRequestRepository.findById(any(UUID.class))).thenReturn(Optional.of(batch));
    when(circulationRequestService.getItemRequestAllowedServicePoints(split.getRequesterId(), split.getItemId()))
      .thenReturn(new CirculationClient.AllowedServicePoints(Set.of(), Set.of(), Set.of()));

    assertThrows(MediatedBatchRequestValidationException.class, () -> processor.execute(context),
      "Not allowed to create Request for the given service point id.");

    verifyNoInteractions(splitRepository);
  }

  @Test
  void execute_negative_shouldThrowUnsupportedOperationErrorInSecureTenantEnv() {
    var batch = new MediatedBatchRequest();
    batch.setId(UUID.randomUUID());
    var split = new MediatedBatchRequestSplit();

    when(context.getBatchSplitEntity()).thenReturn(split);
    when(context.getDeploymentEnvType()).thenReturn(EnvironmentType.SECURE_TENANT);
    when(context.getBatchRequestId()).thenReturn(batch.getId());
    when(batchRequestRepository.findById(any(UUID.class))).thenReturn(Optional.of(batch));

    assertThrows(UnsupportedOperationException.class, () -> processor.execute(context));

    verifyNoInteractions(splitRepository);
  }

  @Test
  void onStart_positive_shouldSetStatusToInProgressAndSaveEntity() {
    var split = new MediatedBatchRequestSplit();
    split.setStatus(BatchRequestSplitStatus.PENDING);
    when(context.getBatchSplitEntity()).thenReturn(split);

    processor.onStart(context);

    assertEquals(BatchRequestSplitStatus.IN_PROGRESS, split.getStatus());
    verify(splitRepository).save(split);
  }

  @Test
  void onError_positive_shouldSetStatusToFailedAndSaveEntityWithErrorDetails() {
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
  void onError_positive_shouldSetErrorDetailsWhenNoCause() {
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
