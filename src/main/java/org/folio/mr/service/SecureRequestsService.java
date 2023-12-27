package org.folio.mr.service;

import org.folio.mr.domain.dto.SecureRequest;
import java.util.UUID;

public interface SecureRequestsService {
  SecureRequest get(UUID requestId);
}
