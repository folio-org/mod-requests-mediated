package org.folio.mr.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.dto.ServicePoint;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange(url = "circulation", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface CirculationClient {

  @GetExchange("/requests/{id}")
  Request getRequest(@PathVariable String id);

  @PostExchange("/requests")
  Request createRequest(@RequestBody Request request);

  @PutExchange("/requests/{id}")
  Request updateRequest(@PathVariable String id, @RequestBody Request request);

  @GetExchange("/requests/allowed-service-points")
  AllowedServicePoints allowedServicePointsByItem(
    @RequestParam("requesterId") String requesterId,
    @RequestParam("operation") String operation,
    @RequestParam("itemId") String itemId);

  record AllowedServicePoints(@JsonProperty("Page") Set<ServicePoint> page, @JsonProperty("Hold") Set<ServicePoint> hold, @JsonProperty("Recall") Set<ServicePoint> recall) { }
}
