package org.folio.mr.client;

import org.folio.mr.domain.dto.Libraries;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "libraries", url = "location-units/libraries",
  configuration = FeignClientConfiguration.class, dismiss404 = true)
public interface LibraryClient extends GetByQueryParamsClient<Libraries> {
}
