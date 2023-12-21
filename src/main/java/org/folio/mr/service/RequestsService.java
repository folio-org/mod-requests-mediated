package org.folio.mr.service;

import org.folio.mr.domain.dto.Request;

public interface RequestsService {
  Request retrieveMediatedRequestById(String id);
}
