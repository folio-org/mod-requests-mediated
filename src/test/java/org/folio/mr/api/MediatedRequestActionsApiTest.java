package org.folio.mr.api;

import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequestForItemArrivalConfirmation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.mr.domain.dto.ConfirmItemArrivalRequest;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import lombok.SneakyThrows;

@IntegrationTest
public class MediatedRequestActionsApiTest extends BaseIT {

  private static final String CONFIRM_ITEM_ARRIVAL_URL = "/requests-mediated/confirm-item-arrival";

  @Autowired
  private MediatedRequestsRepository mediatedRequestsRepository;

  @BeforeEach
  public void beforeEach() {
    mediatedRequestsRepository.deleteAll();
  }

  @Test
  @SneakyThrows
  void successfulItemArrivalConfirmation() {
    MediatedRequestEntity request = createMediatedRequest();

    confirmItemArrival("test-item-barcode")
      .andExpect(status().isOk())
      .andExpect(jsonPath("arrivalDate", notNullValue()))
      .andExpect(jsonPath("instance.id", is(request.getInstanceId().toString())))
      .andExpect(jsonPath("instance.title", is("test-title")))
      .andExpect(jsonPath("item.id", is(request.getItemId().toString())))
      .andExpect(jsonPath("item.barcode", is("test-item-barcode")))
      .andExpect(jsonPath("item.effectiveCallNumberString", is("test-shelving-order")))
      .andExpect(jsonPath("mediatedRequest.id", is(request.getId().toString())))
      .andExpect(jsonPath("mediatedRequest.status", is("Open - Item arrived")))
      .andExpect(jsonPath("requester.id", is(request.getRequesterId().toString())))
      .andExpect(jsonPath("requester.barcode", is("test-requester-barcode")))
      .andExpect(jsonPath("requester.name", is("Last, First")));

    MediatedRequestEntity updatedRequest = mediatedRequestsRepository.findById(request.getId())
      .orElseThrow();
    assertThat(updatedRequest.getMediatedRequestStep(), is("Item arrived"));
    assertThat(updatedRequest.getStatus(), is("Open - Item arrived"));
  }

  @Test
  @SneakyThrows
  void itemArrivalConfirmationFailsWhenMediatedRequestIsNotFoundByItemBarcode() {
    confirmItemArrival("random-barcode")
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("errors").value(iterableWithSize(1)))
      .andExpect(jsonPath("errors[0].type").value("EntityNotFoundException"))
      .andExpect(jsonPath("errors[0].message")
        .value(is("Mediated request for item with barcode 'random-barcode' was not found")));
  }

  @Test
  @SneakyThrows
  void itemArrivalConfirmationFailsWhenMediatedRequestIsNotFoundByStatus() {
    mediatedRequestsRepository.save(buildMediatedRequestForItemArrivalConfirmation()
      .withItemBarcode("real-barcode")
      .withMediatedRequestStep("Item arrived")
      .withStatus("Open - Item arrived")); // wrong status

    confirmItemArrival("real-barcode")
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("errors").value(iterableWithSize(1)))
      .andExpect(jsonPath("errors[0].type").value("EntityNotFoundException"))
      .andExpect(jsonPath("errors[0].message")
        .value(is("Mediated request for item with barcode 'real-barcode' was not found")));
  }



  private MediatedRequestEntity createMediatedRequest() {
    return mediatedRequestsRepository.save(buildMediatedRequestForItemArrivalConfirmation());
  }

  @SneakyThrows
  private ResultActions confirmItemArrival(String itemBarcode) {
    return mockMvc.perform(
      post(CONFIRM_ITEM_ARRIVAL_URL)
        .headers(buildHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(new ConfirmItemArrivalRequest().itemBarcode(itemBarcode))));
  }
}
