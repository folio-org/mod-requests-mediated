package org.folio.mr.service;

import org.folio.mr.domain.dto.CheckOutRequest;
import org.folio.mr.domain.dto.CheckOutResponse;

public interface CheckOutService {
  CheckOutResponse checkOut(CheckOutRequest request);
}
