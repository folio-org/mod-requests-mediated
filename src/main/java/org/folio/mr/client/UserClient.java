package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.User;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "users", url = "users", configuration = FeignClientConfiguration.class,
  dismiss404 = true)
public interface UserClient {

  @GetMapping("/{id}")
  Optional<User> get(@PathVariable String id);

}
