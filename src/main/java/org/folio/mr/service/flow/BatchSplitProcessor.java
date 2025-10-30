package org.folio.mr.service.flow;

import static org.folio.mr.domain.BatchRequestSplitStatus.FAILED;
import static org.folio.mr.domain.BatchRequestSplitStatus.IN_PROGRESS;
import static org.folio.mr.domain.RequestLevel.ITEM;
import static org.folio.mr.exception.MediatedBatchRequestValidationException.invalidPickupServicePoint;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.flow.api.Stage;
import org.folio.mr.client.EcsExternalTlrClient;
import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.domain.FulfillmentPreference;
import org.folio.mr.domain.dto.EcsRequestExternal;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.dto.ServicePoint;
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

  private static final String DEFAULT_FULFILLMENT_PREFERENCE = "Hold Shelf";

  private final MediatedBatchRequestSplitRepository batchRequestSplitRepository;
  private final MediatedBatchRequestRepository batchRequestRepository;
  private final SearchService searchService;
  private final FolioExecutionContext executionContext;
  private final EcsExternalTlrClient ecsTlrClient;
  private final SystemUserScopedExecutionService executionService;
  private final CirculationRequestService circulationRequestService;

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

    if (StringUtils.isBlank(errorMessage)) {
      errorMessage = "Failed to create request for item %s".formatted(splitEntity.getItemId());
    }
    log.error("onError:: Batch split entity with id: {} has failed with error: {}",
      splitEntity.getId(), errorMessage);

    splitEntity.setErrorDetails(errorMessage);
    splitEntity.setStatus(FAILED);
    batchRequestSplitRepository.save(splitEntity);
  }

  private void createRequest(MediatedBatchRequest batchRequest, MediatedBatchRequestSplit splitEntity,
                             EnvironmentType envType) {
    if (envType == EnvironmentType.ECS) {
      log.info("createRequest:: creating ECS request");
      createEcsRequest(batchRequest, splitEntity);
      return;
    } else if (envType == EnvironmentType.SINGLE_TENANT) {
      log.info("createRequest:: creating Single Tenant request");
      createSingleTenantRequest(batchRequest, splitEntity);
      return;
    }

    // secure tenant case
    log.info("createRequest:: creating Secure Tenant Mediated Request");
    createMediatedRequests();
  }

  private void createEcsRequest(MediatedBatchRequest batchEntity, MediatedBatchRequestSplit splitEntity) {
    var ecsPostDto = buildEcsRequestPostDto(batchEntity, splitEntity);
    var ecsTlr = executionService.executeSystemUserScoped(executionContext.getTenantId(),
      () -> ecsTlrClient.createEcsExternalRequest(ecsPostDto));
    var requestCreated = circulationRequestService.get(ecsTlr.getPrimaryRequestId());
    updateBatchRequestSplit(splitEntity, requestCreated);

    log.info("createEcsRequest:: Created ECS request with id {}, for batch split entity {}",
      requestCreated.getId(), splitEntity.getId());
  }

  private EcsRequestExternal buildEcsRequestPostDto(MediatedBatchRequest batch, MediatedBatchRequestSplit split) {
    var itemId = split.getItemId().toString();
    var consortiumItem = searchService.searchItem(itemId)
      .orElseThrow(() -> new ItemNotFoundException(split.getItemId()));

    return new EcsRequestExternal(consortiumItem.getInstanceId(), split.getRequesterId().toString(), ITEM,
      FulfillmentPreference.fromValue(DEFAULT_FULFILLMENT_PREFERENCE), batch.getRequestDate())
      .withItemId(itemId)
      .withHoldingsRecordId(consortiumItem.getHoldingsRecordId())
      .withPatronComments(split.getPatronComments())
      .withPickupServicePointId(split.getPickupServicePointId().toString())
      // it is central tenant as we have checked it before when setting the deployment env type
      .withPrimaryRequestTenantId(executionContext.getTenantId());
  }

  private void createSingleTenantRequest(MediatedBatchRequest batch, MediatedBatchRequestSplit splitEntity) {
    var requestType = findMatchingRequestType(splitEntity)
      .orElseThrow(() -> invalidPickupServicePoint(batch.getId(), splitEntity.getPickupServicePointId(), splitEntity.getItemId()));
    var requestPostDto = buildLocalRequestPostDto(splitEntity, batch.getRequestDate(), requestType);
    var requestCreated = circulationRequestService.create(requestPostDto);
    updateBatchRequestSplit(splitEntity, requestCreated);

    log.info("createSingleTenantRequest:: Created Single Tenant request with id {}, for batch split entity {}",
      requestCreated.getId(), splitEntity.getId());
  }

  Optional<Request.RequestTypeEnum> findMatchingRequestType(MediatedBatchRequestSplit splitEntity) {
    var allowedServicePoints = circulationRequestService
      .getItemRequestAllowedServicePoints(splitEntity.getRequesterId(), splitEntity.getItemId());

    var pickupServicePointId = splitEntity.getPickupServicePointId();
    if (containsServicePoint(allowedServicePoints.page(), pickupServicePointId)) {
      return Optional.of(Request.RequestTypeEnum.PAGE);
    }

    if (containsServicePoint(allowedServicePoints.hold(), pickupServicePointId)) {
      return Optional.of(Request.RequestTypeEnum.HOLD);
    }

    if (containsServicePoint(allowedServicePoints.recall(), pickupServicePointId)) {
      return Optional.of(Request.RequestTypeEnum.RECALL);
    }

    log.warn("findMatchingRequestType:: Pickup Service point id [{}] is not allowed for item id: [{}] and " +
        "requester id: [{}]", pickupServicePointId, splitEntity.getItemId(), splitEntity.getRequesterId());
    return Optional.empty();
  }

  private boolean containsServicePoint(Set<ServicePoint> servicePoints, UUID servicePointId) {
    if (CollectionUtils.isEmpty(servicePoints) || servicePointId == null) {
      return false;
    }

    var servicePointIdStr = servicePointId.toString();
    return servicePoints.stream()
      .map(ServicePoint::getId)
      .anyMatch(servicePointIdStr::equals);
  }

  private Request buildLocalRequestPostDto(MediatedBatchRequestSplit splitEntity, Date requestDate,
                                           Request.RequestTypeEnum requestType) {
    return new Request()
      .requestLevel(Request.RequestLevelEnum.ITEM)
      .requestType(requestType)
      .itemId(splitEntity.getItemId().toString())
      .requesterId(splitEntity.getRequesterId().toString())
      .fulfillmentPreference(Request.FulfillmentPreferenceEnum.fromValue(DEFAULT_FULFILLMENT_PREFERENCE))
      .pickupServicePointId(splitEntity.getPickupServicePointId().toString())
      .requestDate(requestDate)
      .patronComments(splitEntity.getPatronComments());
  }

  private void createMediatedRequests() {
    throw new UnsupportedOperationException(
      "Multi-Item Request is not supported in Secure Tenant environment");
  }

  private void updateBatchRequestSplit(MediatedBatchRequestSplit splitEntity, Request request) {
    splitEntity.setConfirmedRequestId(UUID.fromString(request.getId()));
    if (request.getStatus() == null) {
      log.warn("updateBatchRequestSplit:: Request status is null for created request with id: {}",
        request.getId());
    }
    splitEntity.setRequestStatus(request.getStatus().getValue());
    splitEntity.setStatus(BatchRequestSplitStatus.COMPLETED);
    batchRequestSplitRepository.save(splitEntity);
  }
}
