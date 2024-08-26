package org.folio.mr.client;

import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.Users;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "users", url = "users", configuration = FeignClientConfiguration.class)
public interface UserClient extends GetByQueryClient<Users> {

  @GetMapping("/{id}")
  User get(@PathVariable String id);

}
