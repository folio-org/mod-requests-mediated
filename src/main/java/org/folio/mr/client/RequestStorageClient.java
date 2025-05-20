package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.Request;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "request-storage", url = "request-storage/requests", dismiss404 = true,
  configuration = FeignClientConfiguration.class)
public interface RequestStorageClient {

  @GetMapping("/{id}")
  Optional<Request> getRequest(@PathVariable String id);

}
