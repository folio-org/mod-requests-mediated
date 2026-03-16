package org.folio.mr.client;

import org.folio.mr.domain.dto.EcsRequestExternal;
import org.folio.mr.domain.dto.EcsTlr;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "tlr", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface EcsExternalTlrClient {

  @PostExchange("/create-ecs-request-external")
  EcsTlr createEcsExternalRequest(@RequestBody EcsRequestExternal request);
}
