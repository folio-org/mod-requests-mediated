package org.folio.mr.service;

import java.util.UUID;

import org.folio.mr.domain.dto.MediatedRequest;

public interface MediatedRequestActionsService {
  MediatedRequest confirmItemArrival(String itemBarcode);
  void confirm(UUID mediatedRequestId);
}
