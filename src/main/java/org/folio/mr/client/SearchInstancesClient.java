package org.folio.mr.client;

import org.folio.mr.domain.dto.SearchInstancesResponse;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "searchInstances", url = "search/instances", configuration = FeignClientConfiguration.class)
public interface SearchInstancesClient extends GetByQueryParamsClient<SearchInstancesResponse> {
}
