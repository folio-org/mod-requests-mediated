package org.folio.mr.client;

import org.folio.mr.domain.dto.ClaimItemReturnedCirculationRequest;
import org.folio.mr.domain.dto.DeclareClaimedReturnedItemAsMissingCirculationRequest;
import org.folio.mr.domain.dto.DeclareLostCirculationRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "circulation")
public interface CirculationErrorForwardingClient {

  @PostExchange("/loans/{id}/declare-item-lost")
  void declareItemLost(@PathVariable("id") String loanId,
    @RequestBody DeclareLostCirculationRequest declareLostRequest);

  @PostExchange("/loans/{id}/claim-item-returned")
  void claimItemReturned(@PathVariable("id") String loanId,
    @RequestBody ClaimItemReturnedCirculationRequest request);

  @PostExchange("/loans/{id}/declare-claimed-returned-item-as-missing")
  void declareClaimedReturnedItemAsMissing(@PathVariable("id") String loanId,
    @RequestBody DeclareClaimedReturnedItemAsMissingCirculationRequest request);
}
