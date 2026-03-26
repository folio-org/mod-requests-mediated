package org.folio.mr.client;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "settings", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface SettingsClient {

  @GetExchange(value = "/entries", accept = MediaType.APPLICATION_JSON_VALUE)
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
