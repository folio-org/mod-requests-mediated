package org.folio.mr.client;

import org.folio.mr.domain.dto.BatchIds;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.ConsortiumItems;
import org.folio.mr.domain.dto.SearchInstancesResponse;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "search", url = "search", configuration = FeignClientConfiguration.class)
public interface SearchClient {

  @GetMapping("/consortium/items")
  ConsortiumItems searchItems(@RequestParam String instanceId, @RequestParam String tenantId);

  @GetMapping("/consortium/item/{itemId}")
  ConsortiumItem searchItem(@PathVariable("itemId") String itemId);

  @PostMapping("/consortium/batch/items")
  ConsortiumItems searchItems(@RequestBody BatchIds batchIds);

  @GetMapping("/instances?query=id=={instanceId}&expandAll=true")
  SearchInstancesResponse searchInstance(@PathVariable("instanceId") String instanceId);
}
