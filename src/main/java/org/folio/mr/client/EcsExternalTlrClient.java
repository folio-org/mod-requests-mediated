package org.folio.mr.client;

import org.folio.mr.domain.dto.EcsRequestExternal;
import org.folio.mr.domain.dto.EcsTlr;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ecs-external-tlr", url = "tlr", configuration = FeignClientConfiguration.class)
public interface EcsExternalTlrClient {

  @PostMapping("/create-ecs-request-external")
  EcsTlr createEcsExternalRequest(@RequestBody EcsRequestExternal request);

}
