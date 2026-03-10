package org.folio.mr.client;

import org.folio.mr.domain.dto.GetUserTenantsResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "user-tenants")
public interface UserTenantsClient {

  @GetExchange
  GetUserTenantsResponse getUserTenants(@RequestParam(name = "limit", required = false) Integer limit);

  @GetExchange(accept = MediaType.APPLICATION_JSON_VALUE)
  GetUserTenantsResponse getUserTenants(@RequestParam("tenantId") String tenantId);
}
