package org.folio.mr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
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
import org.folio.mr.service.impl.MediatedRequestsLoansActionServiceImpl;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediatedRequestLoansActionServiceTest {

  @Mock
  private ClaimItemReturnedCirculationClient circulationClient;
  @Mock
  private CirculationStorageService circulationStorageService;
  @Mock
  private ClaimItemReturnedTlrClient tlrClient;
  @Mock
  private MediatedRequestsRepository mediatedRequestsRepository;
  @Mock
  private LoanClient loanClient;
  @Mock
  private RequestStorageClient requestStorageClient;
  @Mock
  private SystemUserScopedExecutionService systemUserService;
  @Mock
  private ConsortiumService consortiumService;
  @InjectMocks
  private MediatedRequestsLoansActionServiceImpl service;

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
    when(circulationStorageService.fetchRequest(confirmedRequestId)).thenReturn(Optional.of(centralRequest));
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
    assertThrows(NotFoundException.class, () -> service.claimItemReturned(loanId, request));
  }

  @Test
  void claimItemReturnedShouldThrowNotFoundIfLoanNotFound() {
    UUID loanId = UUID.randomUUID();
    ClaimItemReturnedCirculationRequest request = new ClaimItemReturnedCirculationRequest();
    when(loanClient.getLoanById(loanId.toString())).thenReturn(Optional.empty());
    assertThrows(org.folio.spring.exception.NotFoundException.class, () -> service.claimItemReturned(loanId, request));
  }
}
