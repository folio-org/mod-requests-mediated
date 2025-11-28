package org.folio.mr.service.impl;

import java.util.Optional;

import org.folio.mr.client.CheckInClient;
import org.folio.mr.domain.dto.CheckInRequest;
import org.folio.mr.domain.dto.CheckInResponse;
import org.folio.mr.service.CheckInService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

  private final CheckInClient checkInClient;

  @Override
  public CheckInResponse checkIn(CheckInRequest request) {
    log.info("checkIn:: itemBarcode={}", request::getItemBarcode);

    CheckInResponse response = checkInClient.checkIn(request);
    removePersonalDataFromResponse(response);

    return response;
  }

  private void removePersonalDataFromResponse(CheckInResponse response) {
    removePersonalDataFromLoan(response);
    removePersonalDataFromStaffSlipContext(response);
  }

  private void removePersonalDataFromLoan(CheckInResponse response) {
    Optional.ofNullable(response.getLoan())
      .ifPresent(loan -> loan.id(null)
        .userId(null)
        .borrower(null));
  }

  private void removePersonalDataFromStaffSlipContext(CheckInResponse response) {
    Optional.ofNullable(response.getStaffSlipContext())
      .ifPresent(context -> {
        context.requester(null);
        context.request(null);
      });
  }
}
