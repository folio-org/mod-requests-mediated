package org.folio.mr.client;

import org.folio.mr.support.CqlQuery;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name="get-by-query", configuration = FeignClientConfiguration.class)
public interface GetByQueryClient<T> {

  int DEFAULT_LIMIT = 1000;

  @GetMapping
  T getByQuery(@RequestParam CqlQuery query, @RequestParam(defaultValue = "1000") int limit);

  default T getByQuery(CqlQuery query) {
    return getByQuery(query, DEFAULT_LIMIT);
  }

}
