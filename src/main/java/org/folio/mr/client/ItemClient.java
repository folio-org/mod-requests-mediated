package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.Items;
import org.folio.mr.support.CqlQuery;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "item-storage/items")
public interface ItemClient extends GetByQueryParamsClient<Items> {

  @GetExchange("/{id}")
  Optional<Item> get(@PathVariable String id);

  @GetExchange
  Items get(@RequestParam("query") CqlQuery cqlQuery);
}
