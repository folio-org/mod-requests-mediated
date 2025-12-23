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
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.domain.dto.MediatedBatchRequestsDto;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.domain.mapper.MediatedBatchRequestMapper;
import org.folio.mr.exception.MediatedBatchRequestValidationException;
import org.folio.mr.service.MediatedBatchRequestFlowProvider;
import org.folio.mr.service.MediatedBatchRequestSplitService;
import org.folio.mr.service.MediatedBatchRequestsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class BatchRequestsServiceDelegate {

  public static final String SETTING_SCOPE = "mod-requests-mediated";
  public static final String SETTING_KEY = "multiItemBatchRequestItemsValidation";
  public static final String BATCH_REQUEST_ITEMS_VALIDATION_SETTING_FETCH_QUERY =
    "(scope=%s AND key=%s)".formatted(SETTING_SCOPE, SETTING_KEY);

  @Value("${folio.batch-requests.validation.max-allowed-items}")
  private Integer batchRequestMaxAllowedItemsCount;

  private final MediatedBatchRequestsService batchRequestsService;
  private final MediatedBatchRequestSplitService requestSplitService;
  private final MediatedBatchRequestMapper mapper;
  private final FlowEngine flowEngine;
  private final MediatedBatchRequestFlowProvider flowProvider;
  private final SettingsClient settingsClient;

  public MediatedBatchRequestsDto retrieveBatchRequestsCollection(String query, Integer offset, Integer limit) {
    log.debug("retrieveBatchRequestsCollection:: parameters query: {}, offset: {}, limit: {}", query, offset, limit);

    var entitiesPage = batchRequestsService.getAll(query, offset, limit);
    return mapper.toMediatedBatchRequestsCollection(entitiesPage);
  }

  public MediatedBatchRequestDto createBatchRequest(MediatedBatchRequestPostDto batchRequestDto) {
    log.debug("createBatchRequest:: parameters batchRequestDto: {}", batchRequestDto);

    var batchEntity = mapper.mapPostDtoToEntity(batchRequestDto);
    var batchSplits = mapper.mapPostDtoToSplitEntities(batchRequestDto);

    validateRequestItems(batchSplits);

    var createdEntity = batchRequestsService.create(batchEntity, batchSplits);

    var flow = flowProvider.createFlow(createdEntity.getId());
    flowEngine.executeAsync(flow);

    return mapper.toDto(createdEntity);
  }

  public MediatedBatchRequestDto getBatchRequestById(UUID id) {
    log.debug("getBatchRequestById:: parameters id: {}", id);

    var entity = batchRequestsService.getById(id);

    return mapper.toDto(entity);
  }

  public MediatedBatchRequestDetailsDto getBatchRequestDetailsByBatchId(UUID batchId, Integer offset, Integer limit) {
    log.debug("getBatchRequestDetailsByBatchId:: parameters batchId: {}, offset: {}, limit: {}", batchId, offset, limit);

    var batchSplitEntities = requestSplitService.getAllByBatchId(batchId, offset, limit);
    return mapper.toMediatedBatchRequestDetailsCollection(batchSplitEntities);
  }

  public MediatedBatchRequestDetailsDto retrieveBatchRequestDetailsCollection(String query, Integer offset, Integer limit) {
    log.debug("retrieveBatchRequestDetailsCollection:: parameters query: {}, offset: {}, limit: {}", query, offset, limit);

    var entitiesPage = requestSplitService.getAll(query, offset, limit);
    return mapper.toMediatedBatchRequestDetailsCollection(entitiesPage);
  }

  private void validateRequestItems(List<MediatedBatchRequestSplit> batchSplits) {
    var uniquesItemIds = batchSplits.stream()
      .map(MediatedBatchRequestSplit::getItemId)
      .collect(Collectors.toSet());

    if (uniquesItemIds.size() < batchSplits.size()) {
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
