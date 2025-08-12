package org.folio.mr.client;

import org.folio.mr.config.ErrorForwardingFeignClientConfiguration;
import org.folio.mr.domain.dto.ClaimItemReturnedCirculationRequest;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "claim-item-returned", url = "circulation",
  configuration = { FeignClientConfiguration.class, ErrorForwardingFeignClientConfiguration.class })
public interface ClaimItemReturnedCirculationClient {

  @PostMapping("/loans/{id}/claim-item-returned")
  void claimItemReturned(@PathVariable("id") String loanId,
    @RequestBody ClaimItemReturnedCirculationRequest request);
}

