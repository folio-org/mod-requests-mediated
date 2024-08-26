package org.folio.mr.client;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface GetByQueryClient<T> {
  
  @GetMapping("?query={query}")
  T getByQuery(@PathVariable String query);
}
