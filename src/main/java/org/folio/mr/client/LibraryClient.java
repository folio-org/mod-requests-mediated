package org.folio.mr.client;

import java.util.Map;

import org.folio.mr.domain.dto.Libraries;
import org.folio.mr.support.CqlQuery;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "location-units/libraries", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface LibraryClient extends GetByQueryParamsClient<Libraries> {

  @Override
  @GetExchange
  Libraries getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @Override
  @GetExchange
  Libraries getByQueryParams(@RequestParam Map<String, String> queryParams);
}
