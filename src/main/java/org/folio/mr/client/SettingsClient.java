package org.folio.mr.client;

import java.util.List;
import java.util.UUID;
import org.folio.mr.config.ErrorForwardingFeignClientConfiguration;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "settings", url = "settings",
  configuration = { FeignClientConfiguration.class, ErrorForwardingFeignClientConfiguration.class })
public interface SettingsClient {

  @GetMapping(value = "/entries", produces = APPLICATION_JSON_VALUE)
  SettingsEntries getSettingsEntries(@RequestParam("query") String query, @RequestParam("limit") int limit);

  record SettingsEntries(List<SettingEntry> items, ResultInfo resultInfo) {}

  record SettingEntry(UUID id, String scope, String key, BatchRequestItemsValidationValue value, UUID userId) {
    public SettingEntry(UUID id, String scope, String key, BatchRequestItemsValidationValue value) {
      this(id, scope, key, value, null);
    }
  }

  record BatchRequestItemsValidationValue(Integer maxAllowedItemsCount) {}

  record ResultInfo(Integer totalRecords) {}
}
