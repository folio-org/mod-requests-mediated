package org.folio.mr.service.impl;

import java.util.Optional;

import org.folio.mr.client.SearchClient;
import org.folio.mr.domain.dto.SearchInstance;
import org.folio.mr.service.SearchService;
import org.folio.mr.support.CqlQuery;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

  private final SearchClient searchClient;
  private final SystemUserScopedExecutionService executionService;

  @Override
  public Optional<SearchInstance> findInstance(String instanceId) {
    log.info("findInstance:: searching for instance {}", instanceId);
    return searchClient.findInstance(CqlQuery.exactMatch("id", instanceId), true)
      .getInstances()
      .stream()
      .findFirst();
  }

}
