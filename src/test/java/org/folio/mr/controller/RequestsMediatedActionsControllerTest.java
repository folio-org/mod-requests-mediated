package org.folio.mr.controller;

import org.folio.mr.domain.dto.ConfirmItemArrivalRequest;
import org.folio.mr.domain.dto.SendItemInTransitRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class RequestsMediatedActionsControllerTest {

  @InjectMocks
  private RequestsMediatedActionsController requestsController;

  @Test
  void confirmItemArrival() {
    var response = requestsController.confirmItemArrival(mock(ConfirmItemArrivalRequest.class));
    Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void sendItemInTransit() {
    var response = requestsController.sendItemInTransit(mock(SendItemInTransitRequest.class));
    Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }
}
