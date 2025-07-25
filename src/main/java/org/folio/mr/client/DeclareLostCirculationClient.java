package org.folio.mr.client;

import org.folio.mr.config.ErrorForwardingFeignClientConfiguration;
import org.folio.mr.domain.dto.DeclareLostCirculationRequest;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "declare-lost", url = "circulation",
  configuration = { FeignClientConfiguration.class, ErrorForwardingFeignClientConfiguration.class })
public interface DeclareLostCirculationClient {

  @PostMapping("/loans/{id}/declare-item-lost")
  void declareItemLost(@PathVariable("id") String loanId,
    @RequestBody DeclareLostCirculationRequest declareLostRequest);

}
