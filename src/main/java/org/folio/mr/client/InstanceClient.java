package org.folio.mr.client;

import org.folio.mr.domain.dto.Instances;
import org.folio.mr.support.CqlQuery;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "instances", url = "/instance-storage/instances", configuration = FeignClientConfiguration.class)
public interface InstanceClient {

  @GetMapping("/")
  Instances getInstances(@RequestParam("query") CqlQuery cqlQuery);

}
