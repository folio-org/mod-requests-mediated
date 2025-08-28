package org.folio.mr.client;

import org.folio.mr.config.ErrorForwardingFeignClientConfiguration;
import org.folio.mr.domain.dto.ClaimItemReturnedTlrRequest;
import org.folio.mr.domain.dto.DeclareClaimedReturnedItemAsMissingTlrRequest;
import org.folio.mr.domain.dto.DeclareLostTlrRequest;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "declare-lost-tlr", url = "tlr",
  configuration = { FeignClientConfiguration.class, ErrorForwardingFeignClientConfiguration.class })
public interface TlrErrorForwardingClient {

  @PostMapping("/loans/declare-item-lost")
  void declareItemLost(@RequestBody DeclareLostTlrRequest declareLostRequest);

  @PostMapping("/loans/claim-item-returned")
  void claimItemReturned(@RequestBody ClaimItemReturnedTlrRequest claimItemReturnedRequest);

  @PostMapping("/loans/declare-claimed-returned-item-as-missing")
  void declareClaimedReturnedItemAsMissing(@RequestBody DeclareClaimedReturnedItemAsMissingTlrRequest request);

}
