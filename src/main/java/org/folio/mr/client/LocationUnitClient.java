package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.Campus;
import org.folio.mr.domain.dto.Institution;
import org.folio.mr.domain.dto.Library;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "location-units", url = "location-units",
  configuration = FeignClientConfiguration.class, dismiss404 = true)
public interface LocationUnitClient {

  @GetMapping("/libraries/{id}")
  Optional<Library> getLibrary(@PathVariable String id);

  @GetMapping("/campuses/{id}")
  Optional<Campus> getCampus(@PathVariable String id);

  @GetMapping("/institutions/{id}")
  Optional<Institution> getInstitution(@PathVariable String id);

}
