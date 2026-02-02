package org.folio.mr.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name = "circulation", url = "circulation",
  configuration = FeignClientConfiguration.class)
public interface CirculationClient {

  @GetMapping("/requests/{id}")
  Request getRequest(@PathVariable String id);

  @PostMapping("/requests")
  Request createRequest(Request request);

  @PutMapping("/requests/{id}")
  Request updateRequest(@PathVariable String id, Request request);

  @GetMapping("/requests/allowed-service-points")
  AllowedServicePoints allowedServicePointsByItem(
    @RequestParam("requesterId") String requesterId,
    @RequestParam("operation") String operation,
    @RequestParam("itemId") String itemId);

  record AllowedServicePoints(@JsonProperty("Page") Set<ServicePoint> page, @JsonProperty("Hold") Set<ServicePoint> hold, @JsonProperty("Recall") Set<ServicePoint> recall) { }
}
