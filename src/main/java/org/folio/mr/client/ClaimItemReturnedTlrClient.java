package org.folio.mr.client;

import org.folio.mr.config.ErrorForwardingFeignClientConfiguration;
import org.folio.mr.domain.dto.ClaimItemReturnedTlrRequest;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "claim-item-returned-tlr", url = "tlr",
  configuration = { FeignClientConfiguration.class, ErrorForwardingFeignClientConfiguration.class })
public interface ClaimItemReturnedTlrClient {

  @PostMapping("/loans/claim-item-returned")
  void claimItemReturned(@RequestBody ClaimItemReturnedTlrRequest claimItemReturnedRequest);

}

