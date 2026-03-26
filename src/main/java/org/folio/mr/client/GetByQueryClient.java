package org.folio.mr.client;

import org.folio.mr.support.CqlQuery;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface GetByQueryClient<T> {

  int DEFAULT_LIMIT = 1000;

  @GetExchange
  T getByQuery(@RequestParam CqlQuery query, @RequestParam(defaultValue = "1000") int limit);

  default T getByQuery(CqlQuery query) {
    return getByQuery(query, DEFAULT_LIMIT);
  }
}
