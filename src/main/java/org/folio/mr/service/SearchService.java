package org.folio.mr.service;

import java.util.Collection;
import java.util.Optional;

import org.folio.mr.domain.dto.BatchIds;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.ConsortiumItems;
import org.folio.mr.domain.dto.SearchInstance;

public interface SearchService {
  Collection<ConsortiumItem> searchItems(String instanceId, String tenantId);
  Optional<ConsortiumItem> searchItem(String itemId);
  Optional<SearchInstance> searchInstance(String instanceId);
  Optional<ConsortiumItem> searchItemByBarcode(String itemBarcode);
  ConsortiumItems searchItems(BatchIds batchIds);
}
