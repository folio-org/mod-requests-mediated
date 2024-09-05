package org.folio.mr.client;

import org.folio.mr.domain.dto.SearchInstancesResponse;
import org.folio.mr.support.CqlQuery;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "search", url = "search", configuration = FeignClientConfiguration.class)
public interface SearchClient {

  @GetMapping("/instances")
  SearchInstancesResponse findInstance(@RequestParam CqlQuery query,
    @RequestParam("expandAll") boolean fetchInstanceDetails);

}
