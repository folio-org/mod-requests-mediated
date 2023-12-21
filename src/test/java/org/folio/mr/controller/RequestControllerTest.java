package org.folio.mr.controller;

import org.folio.mr.domain.dto.Request;
import org.folio.mr.service.RequestsService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RequestControllerTest {
  @Mock
  private RequestsService requestsService;

  @InjectMocks
  private RequestsController requestsController;

  @Test
  void retrieveRequestByIdNotFoundWhenNullTest() {
    when(requestsService.retrieveMediatedRequestById(any())).thenReturn(null);
    var response = requestsController.retrieveRequestById(any());
    verify(requestsService).retrieveMediatedRequestById(any());
    Assertions.assertEquals(response.getStatusCode(), HttpStatusCode.valueOf(404));
  }

  @Test
  void retrieveRequestByIdTest() {
    when(requestsService.retrieveMediatedRequestById(any())).thenReturn(new Request());
    var response = requestsController.retrieveRequestById(any());
    Assertions.assertEquals(response.getStatusCode(), HttpStatusCode.valueOf(200));
  }
}
