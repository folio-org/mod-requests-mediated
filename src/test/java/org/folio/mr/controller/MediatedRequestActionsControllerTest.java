package org.folio.mr.controller;

import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_ITEM_ARRIVED;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequest;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.folio.mr.domain.dto.ConfirmItemArrivalRequest;
import org.folio.mr.domain.dto.ConfirmItemArrivalResponse;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.SendItemInTransitRequest;
import org.folio.mr.service.MediatedRequestActionsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class MediatedRequestActionsControllerTest {

  @Mock
  private MediatedRequestActionsService mediatedRequestActionsService;

  @InjectMocks
  private MediatedRequestActionsController requestsController;

  @Test
  void confirmItemArrivalTest() {
    MediatedRequest mediatedRequest = buildMediatedRequest(OPEN_ITEM_ARRIVED)
      .mediatedRequestStep("Item arrived");
    String itemBarcode = mediatedRequest.getItem().getBarcode();
    when(mediatedRequestActionsService.confirmItemArrival(itemBarcode))
      .thenReturn(mediatedRequest);
    var responseEntity = requestsController.confirmItemArrival(
      new ConfirmItemArrivalRequest(itemBarcode));

    assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
    ConfirmItemArrivalResponse response = responseEntity.getBody();
    assertThat(response, notNullValue());
    assertThat(response.getArrivalDate(), notNullValue());
    assertThat(response.getInstance().getId(), is(UUID.fromString("69640328-788e-43fc-9c3c-af39e243f3b7")));
    assertThat(response.getInstance().getTitle(), is("ABA Journal"));
    assertThat(response.getItem().getId(), is(UUID.fromString("9428231b-dd31-4f70-8406-fe22fbdeabc2")));
    assertThat(response.getItem().getBarcode(), is("A14837334314"));
    assertThat(response.getItem().getCallNumberComponents().getPrefix(), is("PFX"));
    assertThat(response.getItem().getCallNumberComponents().getCallNumber(), is("CN"));
    assertThat(response.getItem().getCallNumberComponents().getSuffix(), is("SFX"));
    assertThat(response.getMediatedRequest().getId(), is(UUID.fromString(mediatedRequest.getId())));
    assertThat(response.getMediatedRequest().getStatus(), is("Open - Item arrived"));
    assertThat(response.getRequester().getId(), is(UUID.fromString("9812e24b-0a66-457a-832c-c5e789797e35")));
    assertThat(response.getRequester().getBarcode(), is("111"));
    assertThat(response.getRequester().getFirstName(), is("Requester"));
    assertThat(response.getRequester().getMiddleName(), is("X"));
    assertThat(response.getRequester().getLastName(), is("Mediated"));
  }

  @Test
  void sendItemInTransit() {
    var response = requestsController.sendItemInTransit(mock(SendItemInTransitRequest.class));
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }
}