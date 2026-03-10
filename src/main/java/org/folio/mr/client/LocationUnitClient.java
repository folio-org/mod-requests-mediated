package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.Campus;
import org.folio.mr.domain.dto.Institution;
import org.folio.mr.domain.dto.Library;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "location-units")
public interface LocationUnitClient {

  @GetExchange("/libraries/{id}")
  Optional<Library> getLibrary(@PathVariable String id);

  @GetExchange("/campuses/{id}")
  Optional<Campus> getCampus(@PathVariable String id);

  @GetExchange("/institutions/{id}")
  Optional<Institution> getInstitution(@PathVariable String id);
}
