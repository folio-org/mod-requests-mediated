package org.folio.mr.service;

import org.folio.mr.domain.dto.SecureRequest;

public interface SecureRequestsService {
  SecureRequest retrieveMediatedRequestById(String id);
}
