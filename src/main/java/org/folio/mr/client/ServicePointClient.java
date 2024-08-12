package org.folio.mr.client;

import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.support.CqlQuery;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "service-points", url = "/service-points", configuration = FeignClientConfiguration.class)
public interface ServicePointClient {

  @GetMapping("/{id}")
  ServicePoint get(@PathVariable String id);
}
