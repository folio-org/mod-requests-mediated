package org.folio.mr.client;

import java.util.Collection;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface GetByQueryClient<T> {
  
  @GetMapping("?query={query}")
  Collection<T> getByQuery(@PathVariable String query);
}
