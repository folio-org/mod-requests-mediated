package org.folio.mr.client;

import java.util.Map;
import java.util.Optional;

import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.domain.dto.ServicePoints;
import org.folio.mr.support.CqlQuery;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "service-points")
public interface ServicePointClient extends GetByQueryParamsClient<ServicePoints> {

  @GetExchange("/{id}")
  Optional<ServicePoint> get(@PathVariable String id);

  @Override
  @GetExchange
  ServicePoints getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @Override
  @GetExchange
  ServicePoints getByQueryParams(@RequestParam Map<String, String> queryParams);
}
