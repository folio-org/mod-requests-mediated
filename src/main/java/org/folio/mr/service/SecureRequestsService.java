package org.folio.mr.service;

import org.folio.mr.domain.dto.SecureRequest;
import java.util.Optional;
import java.util.UUID;

public interface SecureRequestsService {
  Optional<SecureRequest> get(UUID requestId);
}
