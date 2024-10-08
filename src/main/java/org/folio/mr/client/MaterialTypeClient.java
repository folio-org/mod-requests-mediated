package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.MaterialType;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "material-types", url = "material-types",
  configuration = FeignClientConfiguration.class, dismiss404 = true)
public interface MaterialTypeClient {

  @GetMapping("/{id}")
  Optional<MaterialType> get(@PathVariable String id);

}
