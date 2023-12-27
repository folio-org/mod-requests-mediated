package org.folio.mr.service;

import org.folio.mr.domain.mapper.SecureRequestMapper;
import org.folio.mr.repository.SecureRequestsRepository;
import org.folio.mr.service.impl.SecureRequestsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SecureRequestsServiceTest {

  @InjectMocks
  private SecureRequestsServiceImpl secureRequestsService;
  @Mock
  private SecureRequestsRepository secureRequestsRepository;
  @Mock
  private SecureRequestMapper secureRequestMapper;

  @Test
  void getById() {
    secureRequestsService.get(any());
    verify(secureRequestsRepository).findById(any());
  }
}
