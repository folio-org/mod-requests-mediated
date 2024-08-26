package org.folio.mr.client;

import org.folio.mr.domain.dto.Item;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "items", url = "item-storage/items", configuration = FeignClientConfiguration.class)
public interface ItemClient extends GetByQueryClient<Item> {

  @GetMapping("/{id}")
  Item get(@PathVariable String id);
}
