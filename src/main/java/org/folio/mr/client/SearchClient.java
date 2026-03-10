package org.folio.mr.client;

import org.folio.mr.domain.dto.BatchIds;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.ConsortiumItems;
import org.folio.mr.domain.dto.SearchInstancesResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "search")
public interface SearchClient {

  @GetExchange("/consortium/items")
  ConsortiumItems searchItems(@RequestParam String instanceId, @RequestParam String tenantId);

  @GetExchange("/consortium/item/{itemId}")
  ConsortiumItem searchItem(@PathVariable("itemId") String itemId);

  @PostExchange("/consortium/batch/items")
  ConsortiumItems searchItems(@RequestBody BatchIds batchIds);

  @GetExchange("/instances?query=id=={instanceId}&expandAll=true")
  SearchInstancesResponse searchInstance(@PathVariable("instanceId") String instanceId);
}
