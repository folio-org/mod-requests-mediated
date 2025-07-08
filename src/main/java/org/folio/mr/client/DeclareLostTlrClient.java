package org.folio.mr.client;

import org.folio.mr.config.ErrorForwardingFeignClientConfiguration;
import org.folio.mr.domain.dto.DeclareLostTlrRequest;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "declare-lost", url = "tlr",
  configuration = { FeignClientConfiguration.class, ErrorForwardingFeignClientConfiguration.class })
public interface DeclareLostTlrClient {

  @PostMapping("/loans/declare-item-lost")
  void declareItemLost(@RequestBody DeclareLostTlrRequest declareLostRequest);

}
