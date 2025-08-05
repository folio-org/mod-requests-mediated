package org.folio.mr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.folio.mr.client.ClaimItemReturnedCirculationClient;
import org.folio.mr.client.ClaimItemReturnedTlrClient;
import org.folio.mr.client.LoanClient;
import org.folio.mr.client.RequestStorageClient;
import org.folio.mr.domain.dto.ClaimItemReturnedCirculationRequest;
import org.folio.mr.domain.dto.ClaimItemReturnedTlrRequest;
import org.folio.mr.domain.dto.Loan;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.impl.ClaimItemReturnedServiceImpl;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ClaimItemReturnedServiceImplTest {

  private ClaimItemReturnedCirculationClient circulationClient;
  private ClaimItemReturnedTlrClient tlrClient;
  private MediatedRequestsRepository mediatedRequestsRepository;
  private LoanClient loanClient;
  private RequestStorageClient requestStorageClient;
  private SystemUserScopedExecutionService systemUserService;
  private ConsortiumService consortiumService;
  private ClaimItemReturnedServiceImpl service;

  @BeforeEach
  void setUp() {
    circulationClient = mock(ClaimItemReturnedCirculationClient.class);
    tlrClient = mock(ClaimItemReturnedTlrClient.class);
    mediatedRequestsRepository = mock(MediatedRequestsRepository.class);
    loanClient = mock(LoanClient.class);
    requestStorageClient = mock(RequestStorageClient.class);
    systemUserService = mock(SystemUserScopedExecutionService.class);
    consortiumService = mock(ConsortiumService.class);
    service = new ClaimItemReturnedServiceImpl(
      circulationClient, tlrClient, mediatedRequestsRepository,
      loanClient, requestStorageClient, systemUserService, consortiumService);
  }

  @Test
  void claimItemReturnedShouldForwardToCirculationAndTlr() {
    UUID loanId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    UUID fakeUserId = UUID.randomUUID();
    UUID mediatedRequestId = UUID.randomUUID();
    String confirmedRequestId = UUID.randomUUID().toString();
    Date dateTime = new Date();
    String comment = "Returned by patron";

    ClaimItemReturnedCirculationRequest request = new ClaimItemReturnedCirculationRequest();
    request.setItemClaimedReturnedDateTime(dateTime);
    request.setComment(comment);

    Loan loan = new Loan();
    loan.setUserId(fakeUserId.toString());
    loan.setItemId(itemId);
    when(loanClient.getLoanById(loanId.toString())).thenReturn(Optional.of(loan));

    MediatedRequestEntity mediatedRequest = new MediatedRequestEntity();
    mediatedRequest.setId(mediatedRequestId);
    mediatedRequest.setConfirmedRequestId(UUID.fromString(confirmedRequestId));
    mediatedRequest.setItemId(itemId);
    when(mediatedRequestsRepository.findLastClosedFilled(fakeUserId, itemId)).thenReturn(Optional.of(mediatedRequest));

    Request centralRequest = new Request();
    centralRequest.setRequesterId(fakeUserId.toString());
    when(requestStorageClient.getRequest(confirmedRequestId)).thenReturn(Optional.of(centralRequest));
    when(consortiumService.getCentralTenantId()).thenReturn("central");
    doAnswer(invocation -> {
      Runnable runnable = invocation.getArgument(1);
      runnable.run();
      return null;
    }).when(systemUserService).executeAsyncSystemUserScoped(eq("central"), any(Runnable.class));

    service.claimItemReturned(loanId, request);

    verify(circulationClient).claimItemReturned(loanId.toString(), request);
    ArgumentCaptor<ClaimItemReturnedTlrRequest> tlrCaptor = ArgumentCaptor.forClass(ClaimItemReturnedTlrRequest.class);
    verify(tlrClient).claimItemReturned(tlrCaptor.capture());
    ClaimItemReturnedTlrRequest tlrReq = tlrCaptor.getValue();
    assertEquals(itemId, tlrReq.getItemId());
    assertEquals(fakeUserId, tlrReq.getUserId());
    assertEquals(dateTime, tlrReq.getItemClaimedReturnedDateTime());
    assertEquals(comment, tlrReq.getComment());
  }

  @Test
  void claimItemReturnedShouldThrowIfMediatedRequestNotFound() {
    UUID loanId = UUID.randomUUID();
    ClaimItemReturnedCirculationRequest request = new ClaimItemReturnedCirculationRequest();
    Loan loan = new Loan();
    loan.setUserId(UUID.randomUUID().toString());
    loan.setItemId(UUID.randomUUID());
    when(loanClient.getLoanById(loanId.toString())).thenReturn(Optional.of(loan));
    when(mediatedRequestsRepository.findLastClosedFilled(any(), any())).thenReturn(Optional.empty());
    assertThrows(RuntimeException.class, () -> service.claimItemReturned(loanId, request));
  }
}
