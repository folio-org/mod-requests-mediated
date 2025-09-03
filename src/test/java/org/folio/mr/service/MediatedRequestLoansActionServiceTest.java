package org.folio.mr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.folio.mr.client.CirculationErrorForwardingClient;
import org.folio.mr.client.LoanClient;
import org.folio.mr.client.TlrErrorForwardingClient;
import org.folio.mr.domain.dto.ClaimItemReturnedCirculationRequest;
import org.folio.mr.domain.dto.ClaimItemReturnedTlrRequest;
import org.folio.mr.domain.dto.DeclareClaimedReturnedItemAsMissingCirculationRequest;
import org.folio.mr.domain.dto.DeclareClaimedReturnedItemAsMissingTlrRequest;
import org.folio.mr.domain.dto.DeclareLostCirculationRequest;
import org.folio.mr.domain.dto.DeclareLostTlrRequest;
import org.folio.mr.domain.dto.Loan;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.impl.MediatedRequestsLoansActionServiceImpl;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediatedRequestLoansActionServiceTest {

  private static final UUID LOAN_ID = UUID.randomUUID();
  private static final UUID REAL_REQUESTER_ID = UUID.randomUUID();
  private static final UUID FAKE_REQUESTER_ID = UUID.randomUUID();
  private static final UUID ITEM_ID = UUID.randomUUID();
  private static final UUID CONFIRMED_REQUEST_ID = UUID.randomUUID();
  private static final UUID MEDIATED_REQUEST_ID = UUID.randomUUID();
  private static final UUID SERVICE_POINT_ID = UUID.randomUUID();
  private static final Date ACTION_DATE = new Date();
  private static final String COMMENT = "Test comment";

  @Mock
  private CirculationErrorForwardingClient circulationClient;
  @Mock
  private CirculationStorageService circulationStorageService;
  @Mock
  private TlrErrorForwardingClient tlrClient;
  @Mock
  private MediatedRequestsRepository mediatedRequestsRepository;
  @Mock
  private LoanClient loanClient;
  @Mock
  private SystemUserScopedExecutionService systemUserService;
  @Mock
  private ConsortiumService consortiumService;
  @InjectMocks
  private MediatedRequestsLoansActionServiceImpl service;

  private void mockSystemUserService() {
    doAnswer(invocation -> {
      invocation.<Runnable>getArgument(1).run();
      return null;
    }).when(systemUserService).executeAsyncSystemUserScoped(anyString(), any(Runnable.class));
  }

  @Test
  void declareItemLostShouldForwardToCirculationAndTlr() {
    initMocksForHappyPath();

    DeclareLostCirculationRequest request = new DeclareLostCirculationRequest()
      .declaredLostDateTime(ACTION_DATE)
      .servicePointId(SERVICE_POINT_ID)
      .comment(COMMENT);

    service.declareLost(LOAN_ID, request);

    ArgumentCaptor<DeclareLostTlrRequest> tlrCaptor =
      ArgumentCaptor.forClass(DeclareLostTlrRequest.class);
    verify(tlrClient).declareItemLost(tlrCaptor.capture());
    DeclareLostTlrRequest tlrRequest = tlrCaptor.getValue();
    assertEquals(ITEM_ID, tlrRequest.getItemId());
    assertEquals(FAKE_REQUESTER_ID, tlrRequest.getUserId());
    assertEquals(ACTION_DATE, tlrRequest.getDeclaredLostDateTime());
    assertEquals(SERVICE_POINT_ID, tlrRequest.getServicePointId());
    assertEquals(COMMENT, tlrRequest.getComment());
  }

  @Test
  void claimItemReturnedShouldForwardToCirculationAndTlr() {
    initMocksForHappyPath();

    ClaimItemReturnedCirculationRequest request = new ClaimItemReturnedCirculationRequest()
      .itemClaimedReturnedDateTime(ACTION_DATE)
      .comment(COMMENT);

    service.claimItemReturned(LOAN_ID, request);

    ArgumentCaptor<ClaimItemReturnedTlrRequest> tlrCaptor =
      ArgumentCaptor.forClass(ClaimItemReturnedTlrRequest.class);
    verify(tlrClient).claimItemReturned(tlrCaptor.capture());
    ClaimItemReturnedTlrRequest tlrRequest = tlrCaptor.getValue();
    assertEquals(ITEM_ID, tlrRequest.getItemId());
    assertEquals(FAKE_REQUESTER_ID, tlrRequest.getUserId());
    assertEquals(ACTION_DATE, tlrRequest.getItemClaimedReturnedDateTime());
    assertEquals(COMMENT, tlrRequest.getComment());
  }

  @Test
  void declareItemMissingShouldForwardToCirculationAndTlr() {
    initMocksForHappyPath();

    DeclareClaimedReturnedItemAsMissingCirculationRequest request =
      new DeclareClaimedReturnedItemAsMissingCirculationRequest()
        .comment(COMMENT);

    service.declareItemMissing(LOAN_ID, request);

    ArgumentCaptor<DeclareClaimedReturnedItemAsMissingTlrRequest> tlrCaptor =
      ArgumentCaptor.forClass(DeclareClaimedReturnedItemAsMissingTlrRequest.class);
    verify(tlrClient).declareClaimedReturnedItemAsMissing(tlrCaptor.capture());
    DeclareClaimedReturnedItemAsMissingTlrRequest tlrRequest = tlrCaptor.getValue();
    assertEquals(ITEM_ID, tlrRequest.getItemId());
    assertEquals(FAKE_REQUESTER_ID, tlrRequest.getUserId());
    assertEquals(COMMENT, tlrRequest.getComment());
  }

  @Test
  void declareItemLostShouldThrowExceptionIfMediatedRequestNotFound() {
    loanActionShouldThrowExceptionWhenMediatedRequestIsNotFound(
      () -> service.declareLost(LOAN_ID, new DeclareLostCirculationRequest()));
  }

  @Test
  void claimItemReturnedShouldThrowExceptionIfMediatedRequestNotFound() {
    loanActionShouldThrowExceptionWhenMediatedRequestIsNotFound(
      () -> service.claimItemReturned(LOAN_ID, new ClaimItemReturnedCirculationRequest()));
  }

  @Test
  void declareItemMissingShouldThrowExceptionIfMediatedRequestNotFound() {
    loanActionShouldThrowExceptionWhenMediatedRequestIsNotFound(
      () -> service.declareItemMissing(LOAN_ID, new DeclareClaimedReturnedItemAsMissingCirculationRequest()));
  }

  private void loanActionShouldThrowExceptionWhenMediatedRequestIsNotFound(Executable action) {
    when(loanClient.getLoanById(LOAN_ID.toString()))
      .thenReturn(Optional.of(buildLoan()));
    when(mediatedRequestsRepository.findLastClosedFilled(REAL_REQUESTER_ID, ITEM_ID))
      .thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, action);
  }

  @Test
  void declareItemLostShouldThrowExceptionIfLoanIsNotFound() {
    loanActionShouldThrowExceptionWhenLoanIsNotFound
      (() -> service.declareLost(LOAN_ID, new DeclareLostCirculationRequest()));
  }

  @Test
  void claimItemReturnedShouldThrowExceptionIfLoanIsNotFound() {
    loanActionShouldThrowExceptionWhenLoanIsNotFound
      (() -> service.claimItemReturned(LOAN_ID, new ClaimItemReturnedCirculationRequest()));
  }

  @Test
  void declareItemMissingShouldThrowExceptionIfLoanIsNotFound() {
    loanActionShouldThrowExceptionWhenLoanIsNotFound
      (() -> service.declareItemMissing(LOAN_ID, new DeclareClaimedReturnedItemAsMissingCirculationRequest()));
  }

  private void loanActionShouldThrowExceptionWhenLoanIsNotFound(Executable action) {
    when(loanClient.getLoanById(any(String.class)))
      .thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, action);
  }

  private static Loan buildLoan() {
    return new Loan()
      .id(LOAN_ID)
      .userId(REAL_REQUESTER_ID.toString())
      .itemId(ITEM_ID);
  }

  private void initMocksForHappyPath() {
    Request requestInCentralTenant = new Request()
      .id(CONFIRMED_REQUEST_ID.toString())
      .requesterId(FAKE_REQUESTER_ID.toString());

    MediatedRequestEntity mediatedRequest = new MediatedRequestEntity();
    mediatedRequest.setId(MEDIATED_REQUEST_ID);
    mediatedRequest.setConfirmedRequestId(CONFIRMED_REQUEST_ID);
    mediatedRequest.setItemId(ITEM_ID);

    mockSystemUserService();
    when(mediatedRequestsRepository.findLastClosedFilled(REAL_REQUESTER_ID, ITEM_ID))
      .thenReturn(Optional.of(mediatedRequest));
    when(loanClient.getLoanById(LOAN_ID.toString()))
      .thenReturn(Optional.of(buildLoan()));
    when(circulationStorageService.fetchRequest(CONFIRMED_REQUEST_ID.toString()))
      .thenReturn(Optional.of(requestInCentralTenant));
    when(consortiumService.getCentralTenantId())
      .thenReturn("central");
  }
}
