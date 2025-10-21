package org.folio.mr.service;

import java.util.UUID;
import org.folio.mr.client.CirculationClient;
import org.folio.mr.domain.dto.Request;

public interface CirculationRequestService {
  Request get(String id);
  Request create(Request request);
  Request update(Request request);

  CirculationClient.AllowedServicePoints getItemRequestAllowedServicePoints(UUID requesterId, UUID itemId);
}
