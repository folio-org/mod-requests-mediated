package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.Users;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "users", url = "users", configuration = FeignClientConfiguration.class,
  dismiss404 = true)
public interface UserClient extends GetByQueryClient<Users> {

  @GetMapping("/{id}")
  Optional<User> get(@PathVariable String id);

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  User postUser(@RequestBody User user);

}
