package org.folio.mr.client;

import java.util.Map;
import java.util.Optional;

import org.folio.mr.domain.dto.Location;
import org.folio.mr.domain.dto.Locations;
import org.folio.mr.support.CqlQuery;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "locations")
public interface LocationClient extends GetByQueryParamsClient<Locations> {

  @GetExchange("/{id}")
  Optional<Location> get(@PathVariable String id);

  @Override
  @GetExchange
  Locations getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @Override
  @GetExchange
  Locations getByQueryParams(@RequestParam Map<String, String> queryParams);
}
