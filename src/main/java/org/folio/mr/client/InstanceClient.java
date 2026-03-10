package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.Instances;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "instance-storage/instances")
public interface InstanceClient extends GetByQueryParamsClient<Instances> {

  @GetExchange("/{id}")
  Optional<Instance> get(@PathVariable String id);
}
