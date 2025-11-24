package org.folio.mr.service.impl;

import org.folio.mr.client.CheckInClient;
import org.folio.mr.domain.dto.CheckInRequest;
import org.folio.mr.domain.dto.CheckInResponse;
import org.folio.mr.domain.dto.CheckInResponseLoan;
import org.folio.mr.service.CheckInService;
import org.folio.mr.service.CirculationStorageService;
import org.folio.mr.service.TenantSupportService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

  private final SystemUserScopedExecutionService systemUserService;
  private final CheckInClient checkInClient;
  private final CirculationStorageService circulationStorageService;
  private final TenantSupportService tenantSupportService;

  @Override
  public CheckInResponse checkIn(CheckInRequest request) {
    log.info("checkIn:: itemBarcode={}", request::getItemBarcode);
    CheckInResponse response = checkInClient.checkIn(request);
    updateCheckInResponseWithLoanDateFromCentralTenant(response);

    return response;
  }

  private void updateCheckInResponseWithLoanDateFromCentralTenant(CheckInResponse response) {
    if (response.getLoan() == null) {
      log.warn("updateCheckInResponseWithLoanDateFromCentralTenant:: no loan in response, " +
        "skipping replacement");
      return;
    }

    CheckInResponseLoan checkInLoan = response.getLoan();
    tenantSupportService.getCentralTenantId()
      .flatMap(centralTenantId -> systemUserService.executeSystemUserScoped(
        centralTenantId, () -> circulationStorageService.findOpenLoan(checkInLoan.getItem().getId())))
      .ifPresent(centralLoan -> {
        checkInLoan.id(centralLoan.getId().toString());
        checkInLoan.userId(centralLoan.getUserId());
      });
  }
}
