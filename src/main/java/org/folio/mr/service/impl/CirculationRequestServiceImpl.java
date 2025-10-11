package org.folio.mr.service.impl;

import java.util.UUID;
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

  private static final String CREATE_REQUEST_OPERATION_NAME = "create";

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
  public Request update(Request request) {
    log.info("update:: update circulation request {}", request.getId());
    return circulationClient.updateRequest(request.getId(), request);
  }

  @Override
  public CirculationClient.AllowedServicePoints getItemRequestAllowedServicePoints(UUID requesterId, UUID itemId) {
    if (requesterId == null || itemId == null) {
      throw new IllegalArgumentException("requesterId and itemId should not be null");
    }

    log.info("getAllowedServicePointsByItem:: get allowed service points for item id [{}] and requester id [{}]",
      itemId, requesterId);
    return circulationClient.allowedServicePointsByItem(requesterId.toString(), CREATE_REQUEST_OPERATION_NAME, itemId.toString());
  }
}
