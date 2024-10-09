package org.folio.mr.client;

import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.Items;
import org.folio.mr.support.CqlQuery;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "items", url = "item-storage/items",
  configuration = FeignClientConfiguration.class, dismiss404 = true)
public interface ItemClient {

  @GetMapping("/{id}")
  Item get(@PathVariable String id);

  @GetMapping
  Items get(@RequestParam("query") CqlQuery cqlQuery);
}
