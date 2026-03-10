package org.folio.mr.client;

import org.folio.mr.domain.dto.ClaimItemReturnedTlrRequest;
import org.folio.mr.domain.dto.DeclareClaimedReturnedItemAsMissingTlrRequest;
import org.folio.mr.domain.dto.DeclareLostTlrRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "tlr")
public interface TlrErrorForwardingClient {

  @PostExchange("/loans/declare-item-lost")
  void declareItemLost(@RequestBody DeclareLostTlrRequest declareLostRequest);

  @PostExchange("/loans/claim-item-returned")
  void claimItemReturned(@RequestBody ClaimItemReturnedTlrRequest claimItemReturnedRequest);

  @PostExchange("/loans/declare-claimed-returned-item-as-missing")
  void declareClaimedReturnedItemAsMissing(@RequestBody DeclareClaimedReturnedItemAsMissingTlrRequest request);
}
