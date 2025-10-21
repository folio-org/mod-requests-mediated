package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.Instances;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "instances", url = "instance-storage/instances",
  configuration = FeignClientConfiguration.class, dismiss404 = true)
public interface InstanceClient extends GetByQueryParamsClient<Instances> {

  @GetMapping("/{id}")
  Optional<Instance> get(@PathVariable String id);

}
