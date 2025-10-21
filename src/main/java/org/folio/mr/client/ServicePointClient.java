package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.domain.dto.ServicePoints;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "service-points", url = "service-points",
  configuration = FeignClientConfiguration.class, dismiss404 = true)
public interface ServicePointClient extends GetByQueryParamsClient<ServicePoints> {

  @GetMapping("/{id}")
  Optional<ServicePoint> get(@PathVariable String id);
}
