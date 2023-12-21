package org.folio.mr.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.RequestEntity;
import org.folio.mr.domain.mapper.RequestsMapper;
import org.folio.mr.repository.RequestRepository;
import org.folio.mr.service.RequestsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class RequestsServiceImpl implements RequestsService {

  private final RequestRepository requestRepository;
  private final RequestsMapper requestsMapper;

  @Override
  public Request retrieveMediatedRequestById(String id) {
    return requestsMapper.mapEntityToDto(findRequestByIdOrNull(id));
  }

  private RequestEntity findRequestByIdOrNull(String id) {
    return requestRepository.findById(id).orElse(null);
  }
}
