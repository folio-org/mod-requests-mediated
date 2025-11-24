package org.folio.mr.service.impl;

import lombok.SneakyThrows;

import org.folio.mr.client.CheckInClient;
import org.folio.mr.domain.dto.CheckInRequest;
import org.folio.mr.domain.dto.CheckInResponse;
import org.folio.mr.domain.dto.CheckInResponseLoan;
import org.folio.mr.domain.dto.CheckInResponseLoanItem;
import org.folio.mr.domain.dto.Loan;
import org.folio.mr.service.CirculationStorageService;
import org.folio.mr.service.TenantSupportService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckInServiceImplTest {

  private static final String ITEM_BARCODE = "item-barcode-123";
  private static final String CENTRAL_TENANT_ID = "central-tenant";
  private static final String CENTRAL_USER_ID = "central-user-id";
  private UUID itemId;

  @Mock
  private SystemUserScopedExecutionService systemUserService;
  @Mock
  private CheckInClient checkInClient;
  @Mock
  private CirculationStorageService circulationStorageService;
  @Mock
  private TenantSupportService tenantSupportService;

  @InjectMocks
  private CheckInServiceImpl checkInService;

  @BeforeEach
  void setUp() {
    itemId = UUID.randomUUID();
  }

  private CheckInRequest buildRequest() {
    CheckInRequest request = new CheckInRequest();
    request.setItemBarcode(ITEM_BARCODE);
    return request;
  }

  private CheckInResponseLoanItem buildLoanItem() {
    CheckInResponseLoanItem item = new CheckInResponseLoanItem();
    item.setId(itemId.toString());
    return item;
  }

  private CheckInResponseLoan buildLoan(String id, CheckInResponseLoanItem item) {
    CheckInResponseLoan loan = new CheckInResponseLoan();
    loan.setId(id);
    loan.setItem(item);
    return loan;
  }

  private CheckInResponse buildResponse(CheckInResponseLoan loan) {
    CheckInResponse response = new CheckInResponse();
    response.setLoan(loan);
    return response;
  }

  private Loan buildCentralLoan(UUID centralLoanId) {
    Loan centralLoan = new Loan();
    centralLoan.setId(centralLoanId);
    centralLoan.setUserId(CENTRAL_USER_ID);
    return centralLoan;
  }

  @SneakyThrows
  private void mockSystemUserScoped() {
    when(systemUserService.executeSystemUserScoped(eq(CENTRAL_TENANT_ID), any()))
      .thenAnswer(invocation -> invocation.getArgument(1, Callable.class).call());
  }

  @SneakyThrows
  @Test
  void checkInShouldUpdateLoanWithCentralTenantDataWhenCentralTenantExists() {
    CheckInRequest request = buildRequest();
    CheckInResponseLoanItem item = buildLoanItem();
    CheckInResponseLoan checkInLoan = buildLoan("local-loan-id", item);
    CheckInResponse response = buildResponse(checkInLoan);
    UUID centralLoanId = UUID.randomUUID();
    Loan centralLoan = buildCentralLoan(centralLoanId);

    when(checkInClient.checkIn(request)).thenReturn(response);
    when(tenantSupportService.getCentralTenantId()).thenReturn(Optional.of(CENTRAL_TENANT_ID));
    mockSystemUserScoped();
    when(circulationStorageService.findOpenLoan(itemId.toString())).thenReturn(Optional.of(centralLoan));

    CheckInResponse result = checkInService.checkIn(request);
    assertThat(result, notNullValue());
    assertThat(result.getLoan(), notNullValue());
    assertThat(result.getLoan().getId(), is(centralLoanId.toString()));
    verify(checkInClient).checkIn(request);
    verify(tenantSupportService).getCentralTenantId();
    verify(systemUserService).executeSystemUserScoped(eq(CENTRAL_TENANT_ID), any());
    verify(circulationStorageService).findOpenLoan(itemId.toString());
  }

  @Test
  void checkInShouldNotUpdateLoanWhenCentralTenantDoesNotExist() {
    CheckInRequest request = buildRequest();
    CheckInResponseLoanItem item = buildLoanItem();
    CheckInResponseLoan checkInLoan = buildLoan("local-loan-id", item);
    CheckInResponse response = buildResponse(checkInLoan);

    when(checkInClient.checkIn(request)).thenReturn(response);
    when(tenantSupportService.getCentralTenantId()).thenReturn(Optional.empty());

    CheckInResponse result = checkInService.checkIn(request);
    assertThat(result, notNullValue());
    assertThat(result.getLoan(), notNullValue());
    assertThat(result.getLoan().getId(), is("local-loan-id"));
    verify(checkInClient).checkIn(request);
    verify(tenantSupportService).getCentralTenantId();
    verify(systemUserService, never()).executeSystemUserScoped(any(), any());
    verify(circulationStorageService, never()).findOpenLoan(any());
  }

  @Test
  void checkInShouldNotUpdateLoanWhenResponseHasNoLoan() {
    CheckInRequest request = buildRequest();
    CheckInResponse response = buildResponse(null);

    when(checkInClient.checkIn(request)).thenReturn(response);

    CheckInResponse result = checkInService.checkIn(request);
    assertThat(result, notNullValue());
    verify(checkInClient).checkIn(request);
    verify(tenantSupportService, never()).getCentralTenantId();
    verify(systemUserService, never()).executeSystemUserScoped(any(), any());
    verify(circulationStorageService, never()).findOpenLoan(any());
  }

  @SneakyThrows
  @Test
  void checkInShouldNotUpdateLoanWhenCentralLoanNotFound() {
    CheckInRequest request = buildRequest();
    CheckInResponseLoanItem item = buildLoanItem();
    CheckInResponseLoan checkInLoan = buildLoan("local-loan-id", item);
    CheckInResponse response = buildResponse(checkInLoan);

    when(checkInClient.checkIn(request)).thenReturn(response);
    when(tenantSupportService.getCentralTenantId()).thenReturn(Optional.of(CENTRAL_TENANT_ID));
    mockSystemUserScoped();
    when(circulationStorageService.findOpenLoan(itemId.toString())).thenReturn(Optional.empty());

    CheckInResponse result = checkInService.checkIn(request);
    assertThat(result, notNullValue());
    assertThat(result.getLoan(), notNullValue());
    assertThat(result.getLoan().getId(), is("local-loan-id"));
    verify(checkInClient).checkIn(request);
    verify(tenantSupportService).getCentralTenantId();
    verify(systemUserService).executeSystemUserScoped(eq(CENTRAL_TENANT_ID), any());
    verify(circulationStorageService).findOpenLoan(itemId.toString());
  }
}
