package org.folio.mr.client;

import org.folio.mr.domain.dto.Location;
import org.folio.mr.domain.dto.Locations;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "locations", url = "locations", configuration = FeignClientConfiguration.class)
public interface LocationClient extends GetByQueryClient<Locations> {

  @GetMapping("/{id}")
  Location get(@PathVariable String id);
}
