package org.folio.mr.service;

import org.folio.mr.domain.mapper.RequestsMapper;
import org.folio.mr.repository.RequestRepository;
import org.folio.mr.service.impl.RequestsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RequestsServiceTest {

  @InjectMocks
  private  RequestsServiceImpl requestsService;
  @Mock
  private RequestRepository requestRepository;
  @Mock
  private RequestsMapper requestsMapper;

  @Test
  void retrieveMediatedRequestByIdTest() {
    when(requestsMapper.mapEntityToDto(any())).thenReturn(null);
    requestsService.retrieveMediatedRequestById(any());
    verify(requestRepository).findById(any());
  }
}
