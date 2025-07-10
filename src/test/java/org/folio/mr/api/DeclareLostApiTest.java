package org.folio.mr.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.CLOSED_FILLED;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequestEntity;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.UUID;

import org.folio.mr.domain.dto.DeclareLostCirculationRequest;
import org.folio.mr.domain.dto.Loan;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.github.tomakehurst.wiremock.client.WireMock;

import lombok.SneakyThrows;

@IntegrationTest
class DeclareLostApiTest extends BaseIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private MediatedRequestsRepository mediatedRequestsRepository;

  @Test
  void shouldDeclareItemLostSuccessfully() throws Exception {
    UUID loanId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    UUID confirmedRequestId = UUID.randomUUID();
    UUID fakeRequesterId = UUID.randomUUID();
    DeclareLostCirculationRequest request = createDeclareLostRequest();

    wireMockServer.stubFor(WireMock.post(urlPathEqualTo("/circulation/loans/" + loanId +
      "/declare-item-lost"))
      .withHeader(TENANT, equalTo(TENANT_ID_SECURE))
      .willReturn(noContent()));

    mockHelper.mockGetLoan(new Loan()
      .id(loanId)
      .userId(userId.toString())
      .itemId(itemId),
      TENANT_ID_SECURE);

    mediatedRequestsRepository.save(
      buildMediatedRequestEntity(CLOSED_FILLED)
        .withItemId(itemId)
        .withRequesterId(userId)
        .withConfirmedRequestId(confirmedRequestId));

    mockHelper.mockGetRequest(new Request()
      .id(confirmedRequestId.toString())
      .requesterId(fakeRequesterId.toString()),
      TENANT_ID_CENTRAL);

    wireMockServer.stubFor(WireMock.post(urlPathEqualTo("/tlr/loans/declare-item-lost"))
      .withHeader(TENANT, equalTo(TENANT_ID_CENTRAL))
      .willReturn(noContent()));

    performDeclareLostRequest(loanId, request).andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnNotFoundForInvalidLoanId() throws Exception {
    UUID invalidLoanId = UUID.randomUUID();
    var request = createDeclareLostRequest();

    wireMockServer.stubFor(WireMock.get(urlPathEqualTo("loan-storage/loans/" + invalidLoanId))
      .willReturn(notFound()));

    performDeclareLostRequest(invalidLoanId, request)
      .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturnBadRequestForInvalidRequestBody() throws Exception {
    var invalidRequest = new DeclareLostCirculationRequest();
    performDeclareLostRequest(UUID.randomUUID(), invalidRequest)
      .andExpect(status().isBadRequest());
  }

  private DeclareLostCirculationRequest createDeclareLostRequest() {
    return new DeclareLostCirculationRequest()
      .declaredLostDateTime(new Date())
      .servicePointId(UUID.randomUUID())
      .comment("Test comment");
  }

  @SneakyThrows
  private ResultActions performDeclareLostRequest(UUID loanId,
    DeclareLostCirculationRequest request) {

    final HttpHeaders httpHeaders = defaultHeaders();
    httpHeaders.add(TENANT, TENANT_ID_SECURE);

    return mockMvc.perform(post("/requests-mediated/loans/{loanId}/declare-item-lost", loanId)
      .headers(httpHeaders)
      .contentType(MediaType.APPLICATION_JSON)
      .content(asJsonString(request)));
  }
}
