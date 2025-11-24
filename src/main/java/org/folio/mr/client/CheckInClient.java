package org.folio.mr.client;

import org.folio.mr.config.ErrorForwardingFeignClientConfiguration;
import org.folio.mr.domain.dto.CheckInRequest;
import org.folio.mr.domain.dto.CheckInResponse;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "check-in", url = "circulation",
  configuration = { FeignClientConfiguration.class, ErrorForwardingFeignClientConfiguration.class })
public interface CheckInClient {

  @PostMapping("/check-in-by-barcode")
  CheckInResponse checkIn(@RequestBody CheckInRequest request);
}
