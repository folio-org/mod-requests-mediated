package org.folio.mr.service.impl;

import org.folio.mr.client.CirculationClient;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.service.CirculationRequestService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class CirculationRequestServiceImpl implements CirculationRequestService {
  private final CirculationClient circulationClient;

  @Override
  public Request get(String id) {
    log.info("get:: get circulation request by id {}", id);
    return circulationClient.getRequest(id);
  }

  @Override
  public Request create(Request request) {
    return circulationClient.createRequest(request);
  }

  @Override
  public Request createItemRequest(Request request) {
    return circulationClient.createItemRequest(request);
  }

  @Override
  public Request update(Request request) {
    log.info("update:: update circulation request {}", request.getId());
    return circulationClient.updateRequest(request.getId(), request);
  }
}
