package org.folio.mr.api;

import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.CLOSED_FILLED;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequestEntity;

import java.util.UUID;

import org.folio.mr.domain.dto.DeclareClaimedReturnedItemAsMissingCirculationRequest;
import org.folio.mr.domain.dto.Loan;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

@IntegrationTest
class DeclareClaimedReturnedItemAsMissingApiTest extends BaseIT {

  private static final UUID LOAN_ID = UUID.randomUUID();
  private static final UUID REAL_REQUESTER_ID = UUID.randomUUID();
  private static final UUID FAKE_REQUESTER_ID = UUID.randomUUID();
  private static final UUID ITEM_ID = UUID.randomUUID();
  private static final UUID CONFIRMED_REQUEST_ID = UUID.randomUUID();

  private static final String COMMENT = "Test comment";
  private static final String REQUESTS_MEDIATED_DECLARE_MISSING_URL_TEMPLATE =
    "/requests-mediated/loans/%s/declare-claimed-returned-item-as-missing";
  private static final String CIRCULATION_DECLARE_MISSING_URL_TEMPLATE =
    "/circulation/loans/%s/declare-claimed-returned-item-as-missing";
  private static final String CIRCULATION_DECLARE_MISSING_URL =
    String.format(CIRCULATION_DECLARE_MISSING_URL_TEMPLATE, LOAN_ID);
  private static final String TLR_DECLARE_MISSING_URL =
    "/tlr/loans/declare-claimed-returned-item-as-missing";
  private static final String LOAN_STORAGE_URL = "/loan-storage/loans";
  private static final String REQUEST_STORAGE_URL = "/request-storage/requests";

  @Autowired
  private MediatedRequestsRepository mediatedRequestsRepository;

  @Test
  void itemIsDeclaredMissing() {
    mockHelper.mockPostNoContentResponse(CIRCULATION_DECLARE_MISSING_URL, TENANT_ID_CONSORTIUM);
    mockHelper.mockGetLoan(buildLoan(), TENANT_ID_CONSORTIUM);
    mediatedRequestsRepository.save(buildMediatedRequest());
    mockHelper.mockGetRequest(buildCentalTenantRequest(), TENANT_ID_CENTRAL);
    mockHelper.mockPostNoContentResponse(TLR_DECLARE_MISSING_URL, TENANT_ID_CENTRAL);

    declareItemMissing()
      .expectStatus().isNoContent();
  }

  @Test
  void declareItemMissingFailsWhenLoanIsNotFound() {
    UUID invalidLoanId = UUID.randomUUID();
    mockHelper.mockGetNotFoundResponse(LOAN_STORAGE_URL + "/" + invalidLoanId, TENANT_ID_CONSORTIUM);

    declareItemMissing(invalidLoanId, buildDeclareItemMissingRequest())
      .expectStatus().isNotFound();
  }

  @Test
  void declareItemMissingFailsWhenMediatedRequestIsNotFound() {
    mockHelper.mockPostNoContentResponse(CIRCULATION_DECLARE_MISSING_URL, TENANT_ID_CONSORTIUM);
    mockHelper.mockGetLoan(buildLoan(), TENANT_ID_CONSORTIUM);

    declareItemMissing()
      .expectStatus().isNotFound();
  }

  @Test
  void declareItemMissingFailsWhenRequestIsNotFoundInCentralTenant() {
    mockHelper.mockPostNoContentResponse(CIRCULATION_DECLARE_MISSING_URL, TENANT_ID_CONSORTIUM);
    mockHelper.mockGetLoan(buildLoan(), TENANT_ID_CONSORTIUM);
    mockHelper.mockGetNotFoundResponse(REQUEST_STORAGE_URL + "/" + CONFIRMED_REQUEST_ID, TENANT_ID_CENTRAL);

    declareItemMissing()
      .expectStatus().isNotFound();
  }

  @Test
  void declareItemMissingFailsWhenRequestDoesNotContainComment() {
    declareItemMissing(LOAN_ID, new DeclareClaimedReturnedItemAsMissingCirculationRequest().comment(null))
      .expectStatus().isBadRequest();
  }

  private static MediatedRequestEntity buildMediatedRequest() {
    return buildMediatedRequestEntity(CLOSED_FILLED)
      .withItemId(ITEM_ID)
      .withRequesterId(REAL_REQUESTER_ID)
      .withConfirmedRequestId(CONFIRMED_REQUEST_ID);
  }

  private static Loan buildLoan() {
    return new Loan()
      .id(LOAN_ID)
      .userId(REAL_REQUESTER_ID.toString())
      .itemId(ITEM_ID);
  }

  private static DeclareClaimedReturnedItemAsMissingCirculationRequest buildDeclareItemMissingRequest() {
    return new DeclareClaimedReturnedItemAsMissingCirculationRequest()
      .comment(COMMENT);
  }

  private static Request buildCentalTenantRequest() {
    return new Request()
      .id(CONFIRMED_REQUEST_ID.toString())
      .requesterId(FAKE_REQUESTER_ID.toString());
  }

  private WebTestClient.ResponseSpec declareItemMissing() {
    return declareItemMissing(LOAN_ID, buildDeclareItemMissingRequest());
  }

  private WebTestClient.ResponseSpec declareItemMissing(UUID loanId,
    DeclareClaimedReturnedItemAsMissingCirculationRequest request) {

    return doPost(REQUESTS_MEDIATED_DECLARE_MISSING_URL_TEMPLATE.formatted(loanId), request);
  }
}

