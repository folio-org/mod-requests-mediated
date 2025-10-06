package org.folio.mr.service.flow;

import static org.folio.mr.domain.BatchRequestSplitStatus.FAILED;
import static org.folio.mr.domain.BatchRequestSplitStatus.IN_PROGRESS;
import static org.folio.mr.domain.FulfillmentPreference.HOLD_SHELF;
import static org.folio.mr.domain.RequestLevel.ITEM;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.flow.api.Stage;
import org.folio.mr.client.EcsTlrClient;
import org.folio.mr.client.ItemClient;
import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.domain.dto.EcsRequestExternal;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.exception.ItemNotFoundException;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.folio.mr.service.CirculationRequestService;
import org.folio.mr.service.SearchService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Component
@RequiredArgsConstructor
public class BatchSplitProcessor implements Stage<BatchSplitContext> {

  private final MediatedBatchRequestSplitRepository batchRequestSplitRepository;
  private final MediatedBatchRequestRepository batchRequestRepository;
  private final SearchService searchService;
  private final FolioExecutionContext executionContext;
  private final EcsTlrClient ecsTlrClient;
  private final SystemUserScopedExecutionService executionService;
  private final CirculationRequestService circulationRequestService;
  private final ItemClient itemClient;

  @Override
  @Transactional
  public void onStart(BatchSplitContext context) {
    var splitEntity = context.getBatchSplitEntity();

    splitEntity.setStatus(IN_PROGRESS);
    batchRequestSplitRepository.save(splitEntity);
  }

  @Override
  public void execute(BatchSplitContext context) {
    var splitEntity = context.getBatchSplitEntity();
    var batchId = context.getBatchRequestId();
    var batchEntity = batchRequestRepository.findById(batchId)
      .orElseThrow(() -> new MediatedBatchRequestNotFoundException(batchId));
    var envType = context.getDeploymentEnvType();

    if (splitEntity.getItemId() == null || splitEntity.getPickupServicePointId() == null) {
      throw new IllegalArgumentException("Item ID or Pickup Service Point ID is null. Skipping Item Request Creation");
    }

    log.debug("Start processing batch split entity: {}", splitEntity);
    log.info("execute:: Creating request for item with id: {} at service point with id: {}",
      splitEntity.getItemId(), splitEntity.getPickupServicePointId());

    createRequest(batchEntity, splitEntity, envType);
  }

  @Override
  @Transactional
  public void onError(BatchSplitContext context, Exception exception) {
    var splitEntity = context.getBatchSplitEntity();
    var errorMessage = Optional.ofNullable(exception.getCause())
      .map(cause -> exception.getMessage() + ", cause: " + cause.getMessage())
      .orElse(exception.getMessage());

    log.error("onError:: Batch split entity with id: {} has failed with error: {}",
      splitEntity.getId(), errorMessage);

    splitEntity.setErrorDetails(errorMessage);
    splitEntity.setStatus(FAILED);
    batchRequestSplitRepository.save(splitEntity);
  }

  private void createRequest(MediatedBatchRequest batchRequest, MediatedBatchRequestSplit splitEntity,
                             EnvironmentType envType) {
    if (envType == EnvironmentType.ECS) {
      createEcsRequest(batchRequest, splitEntity);
      return;
    }

    createSingleTenantRequest(batchRequest, splitEntity);
  }

  private void createEcsRequest(MediatedBatchRequest batchEntity, MediatedBatchRequestSplit splitEntity) {
    var ecsPostDto = buildEcsRequestPostDto(batchEntity, splitEntity);
    var ecsTlr = executionService.executeSystemUserScoped(executionContext.getTenantId(),
      () -> ecsTlrClient.createEcsExternalRequest(ecsPostDto));
    var request = circulationRequestService.get(ecsTlr.getPrimaryRequestId());
    updateBatchRequestSplit(splitEntity, request);

    log.info("createEcsRequest:: Created ECS request {}, for batch split entity {}",
      request.getId(), splitEntity.getId());
  }

  private EcsRequestExternal buildEcsRequestPostDto(MediatedBatchRequest batch, MediatedBatchRequestSplit split) {
    var itemId = split.getItemId().toString();
    var consortiumItem = searchService.searchItem(itemId)
      .orElseThrow(() -> new ItemNotFoundException(split.getItemId()));

    return new EcsRequestExternal(consortiumItem.getInstanceId(), split.getRequesterId().toString(), ITEM, HOLD_SHELF, batch.getRequestDate())
      .withItemId(itemId)
      .withHoldingsRecordId(consortiumItem.getHoldingsRecordId())
      .withPatronComments(split.getPatronComments())
      .withPickupServicePointId(split.getPickupServicePointId().toString())
      // it is central tenant as we have checked it before when setting the deployment env type
      .withPrimaryRequestTenantId(executionContext.getTenantId());
  }

  private void createSingleTenantRequest(MediatedBatchRequest batch, MediatedBatchRequestSplit splitEntity) {
    var requestPostDto = buildLocalRequestPostDto(batch, splitEntity);
    var request = circulationRequestService.createItemRequest(requestPostDto);
    updateBatchRequestSplit(splitEntity, request);
  }

  private Request buildLocalRequestPostDto(MediatedBatchRequest batch, MediatedBatchRequestSplit splitEntity) {
    var itemId = splitEntity.getItemId().toString();
    var item = itemClient.get(itemId)
      .orElseThrow(() -> new ItemNotFoundException(splitEntity.getItemId()));

    return new Request()
      .requestLevel(Request.RequestLevelEnum.TITLE)
      .instanceId(item.getInstanceId())
      .holdingsRecordId(item.getHoldingsRecordId())
      .itemId(itemId)
      .requesterId(splitEntity.getRequesterId().toString())
      .fulfillmentPreference(Request.FulfillmentPreferenceEnum.HOLD_SHELF)
      .pickupServicePointId(splitEntity.getPickupServicePointId().toString())
      .requestDate(batch.getRequestDate())
      .patronComments(splitEntity.getPatronComments());
  }

  private void updateBatchRequestSplit(MediatedBatchRequestSplit splitEntity, Request request) {
    splitEntity.setConfirmedRequestId(UUID.fromString(request.getId()));
    if (request.getStatus() != null) {
      splitEntity.setRequestStatus(request.getStatus().getValue());
    }
    batchRequestSplitRepository.save(splitEntity);
  }
}
