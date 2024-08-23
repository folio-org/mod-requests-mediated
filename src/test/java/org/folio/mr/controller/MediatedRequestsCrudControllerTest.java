package org.folio.mr.controller;

import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.service.MediatedRequestsService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediatedRequestsCrudControllerTest {
  @Mock
  private MediatedRequestsService requestsService;

  @InjectMocks
  private MediatedRequestsCrudController requestsController;

  @Test
  void getByIdNotFoundWhenNull() {
    when(requestsService.get(any())).thenReturn(Optional.empty());
    var response = requestsController.getMediatedRequestById(any());
    verify(requestsService).get(any());
    Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void getById() {
    when(requestsService.get(any())).thenReturn(Optional.of(new MediatedRequest()));
    var response = requestsController.getMediatedRequestById(any());
    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}
