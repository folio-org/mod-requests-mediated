package org.folio.mr.client;

import java.util.Map;

import org.folio.mr.support.CqlQuery;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface GetByQueryParamsClient<T> {

  @GetExchange
  T getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @GetExchange
  T getByQueryParams(@RequestParam Map<String, String> queryParams);
}
