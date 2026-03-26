package org.folio.mr.client;

import java.util.Map;
import java.util.Optional;

import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.Users;
import org.folio.mr.support.CqlQuery;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "users", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface UserClient extends GetByQueryParamsClient<Users> {

  @Override
  @GetExchange
  Users getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @Override
  @GetExchange
  Users getByQueryParams(@RequestParam Map<String, String> queryParams);

  @GetExchange("/{id}")
  Optional<User> get(@PathVariable String id);

  @PostExchange(contentType = MediaType.APPLICATION_JSON_VALUE)
  User postUser(@RequestBody User user);
}
