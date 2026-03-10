package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.Users;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "users")
public interface UserClient extends GetByQueryParamsClient<Users> {

  @GetExchange("/{id}")
  Optional<User> get(@PathVariable String id);

  @PostExchange(contentType = MediaType.APPLICATION_JSON_VALUE)
  User postUser(@RequestBody User user);
}
