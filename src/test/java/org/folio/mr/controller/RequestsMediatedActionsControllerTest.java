package org.folio.mr.controller;

import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequestForItemArrivalConfirmation;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.folio.mr.domain.dto.ConfirmItemArrivalRequest;
import org.folio.mr.domain.dto.ConfirmItemArrivalResponse;
import org.folio.mr.domain.dto.SendItemInTransitRequest;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.service.MediatedRequestActionsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class RequestsMediatedActionsControllerTest {

  @Mock
  private MediatedRequestActionsService mediatedRequestActionsService;

  @InjectMocks
  private RequestsMediatedActionsController requestsController;

  @Test
  void confirmItemArrivalTest() {
    MediatedRequestEntity mediatedRequest = buildMediatedRequestForItemArrivalConfirmation();
    String itemBarcode = mediatedRequest.getItemBarcode();
    when(mediatedRequestActionsService.confirmItemArrival(itemBarcode))
      .thenReturn(mediatedRequest);
    var responseEntity = requestsController.confirmItemArrival(
      new ConfirmItemArrivalRequest(itemBarcode));

    assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
    ConfirmItemArrivalResponse response = responseEntity.getBody();
    assertThat(response, notNullValue());
    assertThat(response.getArrivalDate(), notNullValue());
    assertThat(response.getInstance().getId(), is(mediatedRequest.getInstanceId()));
    assertThat(response.getInstance().getTitle(), is(mediatedRequest.getInstanceTitle()));
    assertThat(response.getItem().getId(), is(mediatedRequest.getItemId()));
    assertThat(response.getItem().getBarcode(), is(itemBarcode));
    assertThat(response.getItem().getEffectiveCallNumberString(), is(mediatedRequest.getShelvingOrder()));
    assertThat(response.getMediatedRequest().getId(), is(mediatedRequest.getId()));
    assertThat(response.getMediatedRequest().getStatus(), is(mediatedRequest.getStatus()));
    assertThat(response.getRequester().getId(), is(mediatedRequest.getRequesterId()));
    assertThat(response.getRequester().getBarcode(), is(mediatedRequest.getRequesterBarcode()));
    assertThat(response.getRequester().getName(),
      is(mediatedRequest.getRequesterLastName() + ", " + mediatedRequest.getRequesterFirstName()));
  }

  @Test
  void sendItemInTransit() {
    var response = requestsController.sendItemInTransit(mock(SendItemInTransitRequest.class));
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }
}
