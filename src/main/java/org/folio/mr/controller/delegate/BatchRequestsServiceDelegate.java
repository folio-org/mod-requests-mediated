package org.folio.mr.controller.delegate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.flow.api.FlowEngine;
import org.folio.mr.client.SettingsClient;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailsDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDtoItemRequestsStats;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDtoItemRequestsInner;
import org.folio.mr.domain.dto.MediatedBatchRequestsDto;
import org.folio.mr.exception.MediatedBatchRequestValidationException;
import org.folio.mr.service.MediatedBatchRequestFlowProvider;
import org.folio.mr.service.MediatedBatchRequestSplitService;
import org.folio.mr.service.MediatedBatchRequestsService;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class BatchRequestsServiceDelegate {

  public static final String SETTING_SCOPE = "mod-requests-mediated.manage";
  public static final String SETTING_KEY = "multiItemBatchRequestItemsValidation";
  public static final String BATCH_REQUEST_ITEMS_VALIDATION_SETTING_FETCH_QUERY =
    "(scope=%s AND key=%s)".formatted(SETTING_SCOPE, SETTING_KEY);

  @Value("${folio.batch-requests.validation.max-allowed-items}")
  private Integer batchRequestMaxAllowedItemsCount;

  private final MediatedBatchRequestsService batchRequestsService;
  private final MediatedBatchRequestSplitService requestSplitService;
  private final FlowEngine flowEngine;
  private final MediatedBatchRequestFlowProvider flowProvider;
  private final SettingsClient settingsClient;

  public MediatedBatchRequestsDto retrieveBatchRequestsCollection(String query, Integer offset, Integer limit) {
    log.debug("retrieveBatchRequestsCollection:: parameters query: {}, offset: {}, limit: {}", query, offset, limit);

    var resultPage = batchRequestsService.getAll(query, offset, limit);
    return new MediatedBatchRequestsDto(resultPage.getContent(), (int) resultPage.getTotalElements());
  }

  public MediatedBatchRequestDto createBatchRequest(MediatedBatchRequestPostDto batchRequestDto) {
    log.debug("createBatchRequest:: parameters batchRequestDto: {}", batchRequestDto);

    validateRequestItems(batchRequestDto.getItemRequests());
    var createdRequest = batchRequestsService.create(batchRequestDto);
    var flow = flowProvider.createFlow(UUID.fromString(createdRequest.getBatchId()));
    flowEngine.executeAsync(flow);

    return createdRequest;
  }

  public MediatedBatchRequestDto getBatchRequestById(UUID id) {
    log.debug("getBatchRequestById:: parameters id: {}", id);

    var dto = batchRequestsService.getById(id);
    var stats = requestSplitService.getBatchRequestStats(id);

    dto.setItemRequestsStats(new MediatedBatchRequestDtoItemRequestsStats()
      .total(stats.getTotal())
      .pending(stats.getPending())
      .inProgress(stats.getInProgress())
      .completed(stats.getCompleted())
      .failed(stats.getFailed()));

    return dto;
  }

  public MediatedBatchRequestDetailsDto getBatchRequestDetailsByBatchId(UUID batchId, Integer offset, Integer limit) {
    log.debug("getBatchRequestDetailsByBatchId:: parameters batchId: {}, offset: {}, limit: {}", batchId, offset, limit);

    var splitRequestsPage = requestSplitService.getPageByBatchId(batchId, offset, limit);
    return new MediatedBatchRequestDetailsDto()
      .mediatedBatchRequestDetails(splitRequestsPage.getContent())
      .totalRecords((int) splitRequestsPage.getTotalElements());
  }

  public MediatedBatchRequestDetailsDto retrieveBatchRequestDetailsCollection(String query, Integer offset, Integer limit) {
    log.debug("retrieveBatchRequestDetailsCollection:: parameters query: {}, offset: {}, limit: {}", query, offset, limit);

    var splitRequestsPage = requestSplitService.getAll(query, offset, limit);
    return new MediatedBatchRequestDetailsDto()
      .mediatedBatchRequestDetails(splitRequestsPage.getContent())
      .totalRecords((int) splitRequestsPage.getTotalElements());
  }

  private void validateRequestItems(List<MediatedBatchRequestPostDtoItemRequestsInner> itemRequests) {
    var uniquesItemIds = ListUtils.emptyIfNull(itemRequests).stream()
      .map(MediatedBatchRequestPostDtoItemRequestsInner::getItemId)
      .collect(Collectors.toSet());

    if (uniquesItemIds.size() < itemRequests.size()) {
      throw MediatedBatchRequestValidationException.duplicateBatchRequestItems();
    }

    var maxAllowedItemsCount = fetchMaxAllowedItemsCount();
    var requestedItemsCount = uniquesItemIds.size();
    if (requestedItemsCount > maxAllowedItemsCount) {
      throw MediatedBatchRequestValidationException.itemsCountExceedMaxLimit(requestedItemsCount, maxAllowedItemsCount);
    }
  }

  private int fetchMaxAllowedItemsCount() {
    var settingsEntries = settingsClient.getSettingsEntries(BATCH_REQUEST_ITEMS_VALIDATION_SETTING_FETCH_QUERY, 1);
    if (settingsEntries == null || CollectionUtils.isEmpty(settingsEntries.items())) {
      log.warn("fetchMaxAllowedItemsCount:: No settings found for batch request items max count validation, using the one from " +
        "environment variable: {}", batchRequestMaxAllowedItemsCount);
      return batchRequestMaxAllowedItemsCount;
    }

    return Optional.of(settingsEntries.items().getFirst())
      .map(SettingsClient.SettingEntry::value)
      .map(SettingsClient.BatchRequestItemsValidationValue::maxAllowedItemsCount)
      .orElseGet(() -> {
        log.warn("fetchMaxAllowedItemsCount:: No value is set for setting entry with scope {} and key {}, using the one from " +
          "environment variable: {}", SETTING_SCOPE, SETTING_KEY, batchRequestMaxAllowedItemsCount);
        return batchRequestMaxAllowedItemsCount;
      });
  }
}
