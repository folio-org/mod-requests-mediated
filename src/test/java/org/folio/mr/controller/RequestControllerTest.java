package org.folio.mr.controller;

import org.folio.mr.domain.dto.Request;
import org.folio.mr.service.RequestsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
  void retrieveRequestByIdTest() {
    when(requestsService.retrieveMediatedRequestById(any())).thenReturn(new Request());
    requestsController.retrieveRequestById(any());
    verify(requestsService).retrieveMediatedRequestById(any());
  }
}
