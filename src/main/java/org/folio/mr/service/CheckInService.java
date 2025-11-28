package org.folio.mr.service;

import org.folio.mr.domain.dto.CheckInRequest;
import org.folio.mr.domain.dto.CheckInResponse;

public interface CheckInService {
  CheckInResponse checkIn(CheckInRequest request);
}
