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
class SecureRequestsControllerTest {
  @Mock
  private SecureRequestsService requestsService;

  @InjectMocks
  private SecureRequestsController requestsController;

  @Test
  void retrieveSecureRequestByIdNotFoundWhenNull() {
    when(requestsService.retrieveSecureRequestById(any())).thenReturn(null);
    var response = requestsController.get(any());
    verify(requestsService).retrieveSecureRequestById(any());
    Assertions.assertEquals(response.getStatusCode(), HttpStatusCode.valueOf(404));
  }

  @Test
  void retrieveSecureRequestById() {
    when(requestsService.retrieveSecureRequestById(any())).thenReturn(new SecureRequest());
    var response = requestsController.get(any());
    Assertions.assertEquals(response.getStatusCode(), HttpStatusCode.valueOf(200));
  }
}
