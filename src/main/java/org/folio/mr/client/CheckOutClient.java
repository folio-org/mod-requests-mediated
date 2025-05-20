package org.folio.mr.client;

import org.folio.mr.config.ErrorForwardingFeignClientConfiguration;
import org.folio.mr.domain.dto.CheckOutDryRunRequest;
import org.folio.mr.domain.dto.CheckOutDryRunResponse;
import org.folio.mr.domain.dto.CheckOutRequest;
import org.folio.mr.domain.dto.CheckOutResponse;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "check-out", url = "circulation",
  configuration = { FeignClientConfiguration.class, ErrorForwardingFeignClientConfiguration.class })
public interface CheckOutClient {

  @PostMapping("/check-out-by-barcode")
  CheckOutResponse checkOut(@RequestBody CheckOutRequest request);

  @PostMapping("/check-out-by-barcode-dry-run")
  CheckOutDryRunResponse checkOutDryRun(@RequestBody CheckOutDryRunRequest request);
}
