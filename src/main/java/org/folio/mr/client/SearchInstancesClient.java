package org.folio.mr.client;

import org.folio.mr.domain.dto.SearchInstancesResponse;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "search/instances")
public interface SearchInstancesClient extends GetByQueryParamsClient<SearchInstancesResponse> {
}
