package org.folio.mr.client;

import org.folio.mr.domain.dto.HoldingsRecord;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "holdings", url = "/holdings-storage/holdings", configuration = FeignClientConfiguration.class)
public interface HoldingsRecordClient {

  @GetMapping("/{id}")
  HoldingsRecord get(@PathVariable String id);

}
