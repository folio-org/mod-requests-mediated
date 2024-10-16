package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.LoanType;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "loan-types", url = "loan-types",
  configuration = FeignClientConfiguration.class, dismiss404 = true)
public interface LoanTypeClient {

  @GetMapping("/{id}")
  Optional<LoanType> get(@PathVariable String id);

}
