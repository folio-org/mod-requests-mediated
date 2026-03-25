package org.folio.mr.service.flow.splits;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto.MediatedRequestStatusEnum;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.exception.HoldingNotFoundException;
import org.folio.mr.exception.ItemNotFoundException;
import org.folio.mr.service.ConsortiumService;
import org.folio.mr.service.InventoryService;
import org.folio.mr.service.MediatedRequestsService;
import org.folio.mr.service.SearchService;
import org.folio.spring.FolioExecutionContext;

/**
 * Helper class for executing transactional secure tenant request creation within
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
public class SecureTenantRequestHelper extends AbstractRequestHelper {

  private final SearchService searchService;
  private final InventoryService inventoryService;
  private final ConsortiumService consortiumService;
  private final FolioExecutionContext executionContext;
  private final MediatedRequestsService mediatedRequestsService;

  @Override
  protected void createRequest(UUID splitRequestId, MediatedBatchRequestDto batchRequest,
    MediatedBatchRequestDetailDto splitRequest) {

    var itemId = splitRequest.getItemId();
    var instanceId = searchInstanceIdForItem(itemId);
    var mediatedRequest = new MediatedRequest()
      .requesterId(batchRequest.getRequesterId())
      .itemId(itemId)
      .instanceId(instanceId)
      .pickupServicePointId(splitRequest.getPickupServicePointId())
      .patronComments(splitRequest.getPatronComments())
      .requestDate(batchRequest.getRequestDate())
      .fulfillmentPreference(MediatedRequest.FulfillmentPreferenceEnum.HOLD_SHELF)
      .requestLevel(MediatedRequest.RequestLevelEnum.ITEM);

    var createdMediatedRequest = mediatedRequestsService.post(mediatedRequest);

    splitRequest.setConfirmedRequestId(createdMediatedRequest.getId());
    splitRequest.setMediatedRequestStatus(MediatedRequestStatusEnum.COMPLETED);
    batchRequestSplitService.update(splitRequestId, splitRequest);
  }

  @Override
  protected String getRequestName() {
    return "Secure Tenant";
  }

  private String getHoldingIdForItem(String itemId) {
    return Optional.ofNullable(inventoryService.fetchItem(itemId))
      .orElseThrow(() -> new ItemNotFoundException(itemId))
      .getHoldingsRecordId();
  }

  private String getInstanceIdForHolding(String holdingId) {
    return Optional.ofNullable(inventoryService.fetchHolding(holdingId))
      .orElseThrow(() -> new HoldingNotFoundException(holdingId))
      .getInstanceId();
  }

  /**
   * Performs search to find instance ID for the given item ID. The Item might belong to any tenant in the consortium or
   * may exist in the current (secure/local)tenant.
   * @param itemId ID of the item
   * @return ID of the instance the item belongs to
   */
  private String searchInstanceIdForItem(String itemId) {
    var centralTenantId = consortiumService.getCentralTenantId(executionContext.getTenantId());

    if (centralTenantId.isPresent()) {
      log.debug("Searching for item {} in consortium with central tenantId {}", itemId, centralTenantId.get());
      return searchService.searchItem(itemId)
        .map(ConsortiumItem::getInstanceId)
        .orElse(null);
    }

    // it is Non-ECS env so item must be in the local tenant
    log.debug("Searching for item {} in local tenant {}", itemId, executionContext.getTenantId());
    var holdingId = getHoldingIdForItem(itemId);
    return getInstanceIdForHolding(holdingId);
  }
}
