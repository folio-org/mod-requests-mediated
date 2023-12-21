package org.folio.mr.controller;

import org.folio.mr.domain.dto.SecureRequest;
import org.folio.mr.service.SecureRequestsService;
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
class RequestControllerTest {
  @Mock
  private SecureRequestsService requestsService;

  @InjectMocks
  private SecureRequestsController requestsController;

  @Test
  void retrieveRequestByIdNotFoundWhenNullTest() {
    when(requestsService.retrieveMediatedRequestById(any())).thenReturn(null);
    var response = requestsController.retrieveSecureRequestById(any());
    verify(requestsService).retrieveMediatedRequestById(any());
    Assertions.assertEquals(response.getStatusCode(), HttpStatusCode.valueOf(404));
  }

  @Test
  void retrieveRequestByIdTest() {
    when(requestsService.retrieveMediatedRequestById(any())).thenReturn(new SecureRequest());
    var response = requestsController.retrieveSecureRequestById(any());
    Assertions.assertEquals(response.getStatusCode(), HttpStatusCode.valueOf(200));
  }
}
