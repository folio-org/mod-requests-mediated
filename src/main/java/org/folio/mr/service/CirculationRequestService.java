package org.folio.mr.service;

import org.folio.mr.domain.dto.Request;

public interface CirculationRequestService {
  Request get(String id);
  Request create(Request request);
  Request createItemRequest(Request request);
  Request update(Request request);
}
