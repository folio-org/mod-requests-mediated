package org.folio.mr.service;

import org.folio.mr.domain.entity.MediatedRequestEntity;

public interface MediatedRequestActionsService {
  MediatedRequestEntity confirmItemArrival(String itemBarcode);
}
