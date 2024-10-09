package org.folio.mr.client;

import org.folio.mr.domain.dto.ConsortiumItems;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "search", url = "search", configuration = FeignClientConfiguration.class,
  dismiss404 = true)
public interface SearchClient {

  @GetMapping("/consortium/items")
  ConsortiumItems searchItems(@RequestParam String instanceId, @RequestParam String tenantId);

}
