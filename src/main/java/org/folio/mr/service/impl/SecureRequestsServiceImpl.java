package org.folio.mr.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.mr.domain.dto.SecureRequest;
import org.folio.mr.domain.entity.SecureRequestEntity;
import org.folio.mr.domain.mapper.SecureRequestMapper;
import org.folio.mr.repository.SecureRequestsRepository;
import org.folio.mr.service.SecureRequestsService;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class SecureRequestsServiceImpl implements SecureRequestsService {

  private final SecureRequestsRepository secureRequestsRepository;
  private final SecureRequestMapper requestsMapper;

  @Override
  public SecureRequest retrieveSecureRequestById(UUID id) {
    return requestsMapper.mapEntityToDto(findSecureRequestByIdOrNull(id));
  }

  private SecureRequestEntity findSecureRequestByIdOrNull(UUID id) {
    return secureRequestsRepository.findById(id).orElse(null);
  }
}
