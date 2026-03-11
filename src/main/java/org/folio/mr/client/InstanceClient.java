package org.folio.mr.client;

import java.util.Map;
import java.util.Optional;

import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.Instances;
import org.folio.mr.support.CqlQuery;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "instance-storage/instances")
public interface InstanceClient extends GetByQueryParamsClient<Instances> {

  @Override
  @GetExchange
  Instances getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @Override
  @GetExchange
  Instances getByQueryParams(@RequestParam Map<String, String> queryParams);

  @GetExchange("/{id}")
  Optional<Instance> get(@PathVariable String id);
}
