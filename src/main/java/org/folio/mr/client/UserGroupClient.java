package org.folio.mr.client;

import org.folio.mr.domain.dto.UserGroup;
import org.folio.mr.domain.dto.UserGroups;
import org.folio.mr.support.CqlQuery;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "groups", url = "groups", configuration = FeignClientConfiguration.class)
public interface UserGroupClient {

  @GetMapping("/{id}")
  UserGroup get(@PathVariable String id);

}
