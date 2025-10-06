package org.folio.mr.client;

import org.folio.mr.domain.dto.Request;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "circulation", url = "circulation",
  configuration = FeignClientConfiguration.class)
public interface CirculationClient {

  @GetMapping("/requests/{id}")
  Request getRequest(@PathVariable String id);

  @PostMapping("/requests")
  Request createRequest(Request request);

  @PostMapping("/requests/items")
  Request createItemRequest(Request request);

  @PutMapping("/requests/{id}")
  Request updateRequest(@PathVariable String id, Request request);

}
