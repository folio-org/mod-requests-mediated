package org.folio.mr.service.flow.splits;

import static org.folio.mr.domain.RequestLevel.ITEM;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.folio.mr.client.EcsExternalTlrClient;
import org.folio.mr.domain.FulfillmentPreference;
import org.folio.mr.domain.dto.EcsRequestExternal;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.exception.ItemNotFoundException;
import org.folio.mr.service.CirculationRequestService;
import org.folio.mr.service.SearchService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;

/**
 * Helper class for executing transactional ECS request creation within
 * {@link org.folio.spring.FolioExecutionContext} set at the stage level.
 *
 * <p>
 *   Required because placing {@link Transactional} directly on the flow control method
 *   conflicts with {@link org.folio.spring.scope.FolioExecutionContextSetter},
 *   causing database requests to be routed to the last cached tenant ID instead of the correct one.
 * </p>
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class EcsRequestHelper extends AbstractRequestHelper {

  private static final String DEFAULT_FULFILLMENT_PREFERENCE = "Hold Shelf";

  private final SearchService searchService;
  private final FolioExecutionContext executionContext;
  private final EcsExternalTlrClient ecsTlrClient;
  private final SystemUserScopedExecutionService executionService;
  private final CirculationRequestService circulationRequestService;

  @Override
  protected void createRequest(UUID splitRequestId, MediatedBatchRequestDto batchRequest,
    MediatedBatchRequestDetailDto splitRequest) {

    var ecsPostDto = buildEcsRequestPostDto(batchRequest, splitRequest);
    var ecsTlr = executionService.executeSystemUserScoped(executionContext.getTenantId(),
      () -> ecsTlrClient.createEcsExternalRequest(ecsPostDto));
    var requestCreated = circulationRequestService.get(ecsTlr.getPrimaryRequestId());
    updateBatchRequestSplit(splitRequestId, splitRequest, requestCreated);

    log.info("createEcsRequest:: Created ECS request with id {}, for batch split entity {}",
      requestCreated.getId(), splitRequestId);
  }

  @Override
  protected String getRequestName() {
    return "ECS";
  }

  private EcsRequestExternal buildEcsRequestPostDto(MediatedBatchRequestDto batch,
    MediatedBatchRequestDetailDto split) {

    var itemId = split.getItemId();
    var consortiumItem = searchService.searchItem(itemId)
      .orElseThrow(() -> new ItemNotFoundException(itemId));

    return new EcsRequestExternal(consortiumItem.getInstanceId(), split.getRequesterId(), ITEM,
      FulfillmentPreference.fromValue(DEFAULT_FULFILLMENT_PREFERENCE), batch.getRequestDate())
      .withItemId(itemId)
      .withHoldingsRecordId(consortiumItem.getHoldingsRecordId())
      .withPatronComments(split.getPatronComments())
      .withPickupServicePointId(split.getPickupServicePointId())
      // it is central tenant as we have checked it before when setting the deployment env type
      .withPrimaryRequestTenantId(executionContext.getTenantId());
  }
}
