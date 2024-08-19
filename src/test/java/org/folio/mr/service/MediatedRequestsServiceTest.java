package org.folio.mr.service;

import org.folio.mr.domain.mapper.MediatedRequestMapper;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.impl.MediatedRequestsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MediatedRequestsServiceTest {

  @InjectMocks
  private MediatedRequestsServiceImpl mediatedRequestsService;
  @Mock
  private MediatedRequestsRepository mediatedRequestsRepository;
  @Mock
  private MediatedRequestMapper mediatedRequestMapper;
  @Mock
  private MediatedRequestDetailsService mediatedRequestDetailsService;

  @Test
  void getById() {
    mediatedRequestsService.get(any());
    verify(mediatedRequestsRepository).findById(any());
  }
}
