package org.folio.mr.client;

import org.folio.mr.domain.dto.GetUserTenantsResponse;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-tenants", url = "user-tenants",
  configuration = FeignClientConfiguration.class, dismiss404 = true)
public interface UserTenantsClient {

  @GetMapping()
  GetUserTenantsResponse getUserTenants(@RequestParam(name = "limit", required = false) Integer limit);
}