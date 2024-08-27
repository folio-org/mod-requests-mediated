package org.folio.mr.client;

import org.folio.mr.domain.dto.ServicePoint;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "service-points", url = "service-points", configuration = FeignClientConfiguration.class)
public interface ServicePointClient {

  @GetMapping("/{id}")
  ServicePoint get(@PathVariable String id);
}
