package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.Location;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "locations", url = "locations",
  configuration = FeignClientConfiguration.class, dismiss404 = true)
public interface LocationClient {

  @GetMapping("/{id}")
  Optional<Location> get(@PathVariable String id);
}
