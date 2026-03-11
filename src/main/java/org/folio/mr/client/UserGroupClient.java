package org.folio.mr.client;

import java.util.Map;
import java.util.Optional;

import org.folio.mr.domain.dto.UserGroup;
import org.folio.mr.domain.dto.UserGroups;
import org.folio.mr.support.CqlQuery;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "groups")
public interface UserGroupClient extends GetByQueryParamsClient<UserGroups> {

  @GetExchange("/{id}")
  Optional<UserGroup> get(@PathVariable String id);

  @Override
  @GetExchange
  UserGroups getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @Override
  @GetExchange
  UserGroups getByQueryParams(@RequestParam Map<String, String> queryParams);
}
