package org.folio.mr.client;

import java.util.Map;

import org.folio.mr.domain.dto.SearchInstancesResponse;
import org.folio.mr.support.CqlQuery;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "search/instances")
public interface SearchInstancesClient extends GetByQueryParamsClient<SearchInstancesResponse> {

  @Override
  @GetExchange
  SearchInstancesResponse getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @Override
  @GetExchange
  SearchInstancesResponse getByQueryParams(@RequestParam Map<String, String> queryParams);
}
