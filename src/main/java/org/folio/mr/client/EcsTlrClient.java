package org.folio.mr.client;

import org.folio.mr.domain.dto.EcsTlr;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "tlr/ecs-tlr")
public interface EcsTlrClient {

  @PostExchange
  EcsTlr post(@RequestBody EcsTlr ecsTlr);
}
