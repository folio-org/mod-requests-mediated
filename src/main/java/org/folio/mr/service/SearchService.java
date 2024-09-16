package org.folio.mr.service;

import java.util.Collection;

import org.folio.mr.domain.dto.ConsortiumItem;

public interface SearchService {
  Collection<ConsortiumItem> searchItems(String instanceId, String tenantId);
}
