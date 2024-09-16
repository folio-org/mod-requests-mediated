package org.folio.mr.client;

import org.folio.mr.domain.dto.Request;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "circulation", url = "circulation", configuration = FeignClientConfiguration.class)
public interface CirculationClient {

  @PostMapping("/requests")
  Request createRequest(Request request);

}
