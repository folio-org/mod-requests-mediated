package org.folio.mr.service.impl;

import java.util.Collection;
import java.util.Optional;

import org.folio.mr.client.SearchClient;
import org.folio.mr.domain.dto.ConsortiumItem;
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
    log.info("searchItems: searching for items of instance {} in tenant {}", instanceId, tenantId);
    // this search can only be done in central tenant
    return executionService.executeSystemUserScoped(consortiumService.getCentralTenantId(),
        () -> searchClient.searchItems(instanceId, tenantId))
      .getItems();
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
}
