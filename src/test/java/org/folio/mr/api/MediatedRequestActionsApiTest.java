package org.folio.mr.api;

import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_FOR_APPROVAL;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_ITEM_ARRIVED;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequestEntity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.mr.domain.dto.ConfirmItemArrivalRequest;
import org.folio.mr.domain.dto.SendItemInTransitRequest;
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
class MediatedRequestActionsApiTest extends BaseIT {

  private static final String CONFIRM_ITEM_ARRIVAL_URL = "/requests-mediated/confirm-item-arrival";
  private static final String SEND_ITEM_IN_TRANSIT_URL = "/requests-mediated/send-item-in-transit";

  @Autowired
  private MediatedRequestsRepository mediatedRequestsRepository;

  @BeforeEach
  public void beforeEach() {
    mediatedRequestsRepository.deleteAll();
  }

  @Test
  @SneakyThrows
  void successfulItemArrivalConfirmation() {
    MediatedRequestEntity request = mediatedRequestsRepository.save(
      buildMediatedRequestEntity(OPEN_IN_TRANSIT_FOR_APPROVAL)
    );

    confirmItemArrival("A14837334314")
      .andExpect(status().isOk())
      .andExpect(jsonPath("arrivalDate", notNullValue()))
      .andExpect(jsonPath("instance.id", is("69640328-788e-43fc-9c3c-af39e243f3b7")))
      .andExpect(jsonPath("instance.title", is("ABA Journal")))
      .andExpect(jsonPath("item.id", is("9428231b-dd31-4f70-8406-fe22fbdeabc2")))
      .andExpect(jsonPath("item.barcode", is("A14837334314")))
      .andExpect(jsonPath("item.enumeration", is("v.70:no.7-12")))
      .andExpect(jsonPath("item.volume", is("vol.1")))
      .andExpect(jsonPath("item.chronology", is("1984:July-Dec.")))
      .andExpect(jsonPath("item.displaySummary", is("test summary")))
      .andExpect(jsonPath("item.copyNumber", is("cp.1")))
      .andExpect(jsonPath("item.callNumberComponents.prefix", is("PFX")))
      .andExpect(jsonPath("item.callNumberComponents.callNumber", is("CN")))
      .andExpect(jsonPath("item.callNumberComponents.suffix", is("SFX")))
      .andExpect(jsonPath("mediatedRequest.id", is(request.getId().toString())))
      .andExpect(jsonPath("mediatedRequest.status", is("Open - Item arrived")))
      .andExpect(jsonPath("requester.id", is("9812e24b-0a66-457a-832c-c5e789797e35")))
      .andExpect(jsonPath("requester.barcode", is("111")))
      .andExpect(jsonPath("requester.firstName", is("Requester")))
      .andExpect(jsonPath("requester.middleName", is("X")))
      .andExpect(jsonPath("requester.lastName", is("Mediated")));

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
        .value(is("Mediated request for arrival confirmation of item with barcode 'random-barcode' was not found")));
  }

  @Test
  @SneakyThrows
  void itemArrivalConfirmationFailsWhenMediatedRequestIsNotFoundByStatus() {
    mediatedRequestsRepository.save(buildMediatedRequestEntity(OPEN_ITEM_ARRIVED)); // wrong status

    confirmItemArrival("A14837334314")
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("errors").value(iterableWithSize(1)))
      .andExpect(jsonPath("errors[0].type").value("EntityNotFoundException"))
      .andExpect(jsonPath("errors[0].message")
        .value(is("Mediated request for arrival confirmation of item with barcode 'A14837334314' was not found")));
  }

  @SneakyThrows
  private ResultActions confirmItemArrival(String itemBarcode) {
    return mockMvc.perform(
      post(CONFIRM_ITEM_ARRIVAL_URL)
        .headers(buildHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(new ConfirmItemArrivalRequest().itemBarcode(itemBarcode))));
  }

  @Test
  @SneakyThrows
  void sendItemInTransitSuccess() {
    MediatedRequestEntity request = mediatedRequestsRepository.save(
      buildMediatedRequestEntity(OPEN_ITEM_ARRIVED)
    );

    sendItemInTransit("A14837334314")
      .andExpect(status().isOk())
      .andExpect(jsonPath("inTransitDate", notNullValue()))
      .andExpect(jsonPath("instance.id", is("69640328-788e-43fc-9c3c-af39e243f3b7")))
      .andExpect(jsonPath("instance.title", is("ABA Journal")))
      .andExpect(jsonPath("item.id", is("9428231b-dd31-4f70-8406-fe22fbdeabc2")))
      .andExpect(jsonPath("item.barcode", is("A14837334314")))
      .andExpect(jsonPath("item.enumeration", is("v.70:no.7-12")))
      .andExpect(jsonPath("item.volume", is("vol.1")))
      .andExpect(jsonPath("item.chronology", is("1984:July-Dec.")))
      .andExpect(jsonPath("item.displaySummary", is("test summary")))
      .andExpect(jsonPath("item.copyNumber", is("cp.1")))
      .andExpect(jsonPath("item.callNumberComponents.prefix", is("PFX")))
      .andExpect(jsonPath("item.callNumberComponents.callNumber", is("CN")))
      .andExpect(jsonPath("item.callNumberComponents.suffix", is("SFX")))
      .andExpect(jsonPath("mediatedRequest.id", is(request.getId().toString())))
      .andExpect(jsonPath("mediatedRequest.status", is("Open - In transit to be checked out")))
      .andExpect(jsonPath("requester.id", is("9812e24b-0a66-457a-832c-c5e789797e35")))
      .andExpect(jsonPath("requester.barcode", is("111")))
      .andExpect(jsonPath("requester.firstName", is("Requester")))
      .andExpect(jsonPath("requester.middleName", is("X")))
      .andExpect(jsonPath("requester.lastName", is("Mediated")));

    MediatedRequestEntity updatedRequest = mediatedRequestsRepository.findById(request.getId())
      .orElseThrow();
    assertThat(updatedRequest.getMediatedRequestStep(), is("In transit to be checked out"));
    assertThat(updatedRequest.getStatus(), is("Open - In transit to be checked out"));
  }

  @Test
  @SneakyThrows
  void sendItemInTransitFailsWhenMediatedRequestIsNotFoundByItemBarcode() {
    sendItemInTransit("random-barcode")
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("errors").value(iterableWithSize(1)))
      .andExpect(jsonPath("errors[0].type").value("EntityNotFoundException"))
      .andExpect(jsonPath("errors[0].message")
        .value(is("Mediated request for in transit sending of item with barcode 'random-barcode' was not found")));
  }

  @Test
  @SneakyThrows
  void sendItemInTransitFailsWhenMediatedRequestIsNotFoundByStatus() {
    mediatedRequestsRepository.save(buildMediatedRequestEntity(OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT)); // wrong status

    sendItemInTransit("A14837334314")
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("errors").value(iterableWithSize(1)))
      .andExpect(jsonPath("errors[0].type").value("EntityNotFoundException"))
      .andExpect(jsonPath("errors[0].message")
        .value(is("Mediated request for in transit sending of item with barcode 'A14837334314' was not found")));
  }

  @SneakyThrows
  private ResultActions sendItemInTransit(String itemBarcode) {
    return mockMvc.perform(
      post(SEND_ITEM_IN_TRANSIT_URL)
        .headers(buildHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(new SendItemInTransitRequest().itemBarcode(itemBarcode))));
  }

}
