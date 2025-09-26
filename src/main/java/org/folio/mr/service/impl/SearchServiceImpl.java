package org.folio.mr.service.impl;

import static org.folio.mr.domain.dto.BatchIds.IdentifierTypeEnum.BARCODE;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.folio.mr.client.SearchClient;
import org.folio.mr.domain.dto.BatchIds;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.ConsortiumItems;
import org.folio.mr.domain.dto.SearchInstance;
import org.folio.mr.service.ConsortiumService;
import org.folio.mr.service.SearchService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class SearchServiceImpl implements SearchService {

  private final SearchClient searchClient;
  private final ConsortiumService consortiumService;
  private final SystemUserScopedExecutionService executionService;

  @Override
  public Collection<ConsortiumItem> searchItems(String instanceId, String tenantId) {
    log.info("searchItems:: searching for items of instance {} in tenant {}", instanceId, tenantId);
    // this search can only be done in central tenant
    ConsortiumItems searchItems = executionService.executeSystemUserScoped(consortiumService.getCentralTenantId(),
        () -> searchClient.searchItems(instanceId, tenantId));

    if (searchItems == null || searchItems.getItems() == null) {
      log.debug("searchItems:: items array is missing, returning empty list");
      return List.of();
    }

    return searchItems.getItems();
  }

  @Override
  public Optional<ConsortiumItem> searchItem(String itemId) {
    log.info("searchItem: searching for item {}", itemId);
    // this search can only be done in central tenant
    ConsortiumItem item = executionService.executeSystemUserScoped(
      consortiumService.getCentralTenantId(), () -> searchClient.searchItem(itemId));

    // this search returns 200 with empty body if item is not found
    if (item == null || item.getId() == null) {
      log.info("searchItem: item {} not found", itemId);
      return Optional.empty();
    }

    log.info("searchItem: item {} found in tenant {}", itemId, item.getTenantId());
    return Optional.of(item);
  }

  @Override
  public Optional<SearchInstance> searchInstance(String instanceId) {
    log.info("searchInstance:: parameters instanceId: {}", instanceId);

    return Optional.ofNullable(searchClient.searchInstance(instanceId).getInstances())
      .stream()
      .flatMap(Collection::stream)
      .findFirst();
  }

  @Override
  public Optional<ConsortiumItem> searchItemByBarcode(String itemBarcode) {
    log.info("searchItem:: searching item by barcode: {}", itemBarcode);

    Optional<ConsortiumItem> consortiumItem = searchItems(new BatchIds(BARCODE, List.of(itemBarcode)))
      .getItems()
      .stream()
      .findFirst();

    consortiumItem.ifPresentOrElse(
      item -> log.info("searchItem:: item found: id={}, tenantId={}", item.getId(), item.getTenantId()),
      () -> log.warn("searchItem:: item with barcode {} was not found", itemBarcode));

    return consortiumItem;
  }

  @Override
  public ConsortiumItems searchItems(BatchIds batchIds) {
    // this search can be performed in central tenant only!
    return executionService.executeSystemUserScoped(consortiumService.getCentralTenantId(),
      () -> searchClient.searchItems(batchIds));
  }
}
