package org.folio.mr.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

import java.util.UUID;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailsDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.domain.dto.MediatedBatchRequestsDto;
import org.folio.mr.service.MediatedBatchRequestSplitService;
import org.folio.mr.service.MediatedBatchRequestsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class MediatedBatchRequestsControllerTest {

  @Mock
  private MediatedBatchRequestsService service;

  @Mock
  private MediatedBatchRequestSplitService requestSplitService;

  @InjectMocks
  private MediatedBatchRequestsController controller;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void shouldPostBatchMediatedRequests() {
    var postDto = new MediatedBatchRequestPostDto();
    var responseDto = new MediatedBatchRequestDto();
    when(service.create(any(MediatedBatchRequestPostDto.class))).thenReturn(responseDto);

    var response = controller.postBatchMediatedRequests(postDto);

    assertEquals(CREATED, response.getStatusCode());
    assertEquals(responseDto, response.getBody());
  }

  @Test
  void shouldGetMediatedBatchRequestCollection() {
    var responseDto = new MediatedBatchRequestsDto();
    when(service.getAll(anyString(), anyInt(), anyInt())).thenReturn(responseDto);

    var response = controller.getMediatedBatchRequestCollection("query", 0, 10);

    assertEquals(OK, response.getStatusCode());
    assertEquals(responseDto, response.getBody());
  }

  @Test
  void shouldGetMediatedBatchRequestById() {
    var batchRequestId = UUID.randomUUID();
    var responseDto = new MediatedBatchRequestDto();
    when(service.getById(any(UUID.class))).thenReturn(responseDto);

    var response = controller.getMediatedBatchRequestById(batchRequestId);

    assertEquals(OK, response.getStatusCode());
    assertEquals(responseDto, response.getBody());
  }

  @Test
  void shouldGetMediatedBatchRequestDetailsByBatchId() {
    var batchRequestId = UUID.randomUUID();
    MediatedBatchRequestDetailsDto responseDto = new MediatedBatchRequestDetailsDto();
    when(requestSplitService.getAllByBatchId(any(UUID.class), anyInt(), anyInt())).thenReturn(responseDto);

    var response = controller.getMediatedBatchRequestDetailsByBatchId(batchRequestId, 0, 10);

    assertEquals(OK, response.getStatusCode());
    assertEquals(responseDto, response.getBody());
  }
}
