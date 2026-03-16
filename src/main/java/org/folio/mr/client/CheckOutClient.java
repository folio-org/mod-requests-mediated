package org.folio.mr.client;

import org.folio.mr.domain.dto.CheckOutDryRunRequest;
import org.folio.mr.domain.dto.CheckOutDryRunResponse;
import org.folio.mr.domain.dto.CheckOutRequest;
import org.folio.mr.domain.dto.CheckOutResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "circulation", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface CheckOutClient {

  @PostExchange("/check-out-by-barcode")
  CheckOutResponse checkOut(@RequestBody CheckOutRequest request);

  @PostExchange("/check-out-by-barcode-dry-run")
  CheckOutDryRunResponse checkOutDryRun(@RequestBody CheckOutDryRunRequest request);
}
