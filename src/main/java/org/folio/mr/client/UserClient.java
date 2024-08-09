package org.folio.mr.client;

import org.folio.mr.domain.dto.Users;
import org.folio.mr.support.CqlQuery;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "users", url = "users", configuration = FeignClientConfiguration.class)
public interface UserClient {

  @GetMapping("/")
  Users getUsers(@RequestParam("query") CqlQuery cqlQuery);

}
