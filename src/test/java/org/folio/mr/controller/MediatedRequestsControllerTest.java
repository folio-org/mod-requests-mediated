package org.folio.mr.controller;

import org.folio.mr.domain.dto.ConfirmItemArrivalRequest;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.service.MediatedRequestsService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediatedRequestsControllerTest {
  @Mock
  private MediatedRequestsService requestsService;

  @InjectMocks
  private MediatedRequestsController requestsController;

  @Test
  void getByIdNotFoundWhenNull() {
    when(requestsService.get(any())).thenReturn(Optional.empty());
    var response = requestsController.getMediatedRequestById(any());
    verify(requestsService).get(any());
    Assertions.assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());
  }

  @Test
  void getById() {
    when(requestsService.get(any())).thenReturn(Optional.of(new MediatedRequest()));
    var response = requestsController.getMediatedRequestById(any());
    Assertions.assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
  }

  @Test
  void confirmItemArrival() {
    var response = requestsController.confirmItemArrival(mock(ConfirmItemArrivalRequest.class));
    Assertions.assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
  }
}
