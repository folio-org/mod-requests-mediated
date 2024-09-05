package org.folio.mr.service;

import java.util.Optional;

import org.folio.mr.domain.dto.SearchInstance;

public interface SearchService {
  Optional<SearchInstance> findInstance(String instanceId);
}
