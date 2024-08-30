package org.folio.mr.service;

import org.folio.mr.domain.dto.MediatedRequest;

public interface MediatedRequestActionsService {
  MediatedRequest confirmItemArrival(String itemBarcode);
}
