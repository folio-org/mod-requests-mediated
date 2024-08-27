package org.folio.mr.client;

import org.folio.mr.domain.dto.Library;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "location-units", url = "location-units", configuration = FeignClientConfiguration.class)
public interface LocationUnitClient {

  @GetMapping("/libraries/{id}")
  Library getLibrary(@PathVariable String id);
}