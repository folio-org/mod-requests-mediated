package org.folio.mr.service.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.mr.support.ServiceUtils.initId;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.service.MediatedBatchRequestSplitService;
import org.folio.mr.service.MediatedBatchRequestsService;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Log4j2
@Service
@RequiredArgsConstructor
public class MediatedBatchRequestsServiceImpl implements MediatedBatchRequestsService {

  private final MediatedBatchRequestRepository repository;
  private final MediatedBatchRequestSplitService requestSplitService;

  @Override
  @Transactional
  public MediatedBatchRequest create(MediatedBatchRequest batchRequestEntity, List<MediatedBatchRequestSplit> batchSplits) {
    log.debug("create:: Attempting to create Mediated Batch Request: {}", batchRequestEntity);

    initId(batchRequestEntity);

    var saved = repository.save(batchRequestEntity);

    updateRequestSplitEntities(saved, batchSplits);
    requestSplitService.create(batchSplits);

    return saved;
  }

  @Override
  public Page<MediatedBatchRequest> getAll(String query, Integer offset, Integer limit) {
    log.debug("getAll:: Attempts to find all Mediated Batch Requests by [offset: {}, limit: {}, query: {}]",
      offset, limit, query);

    return findEntities(query, offset, limit);
  }

  @Override
  public MediatedBatchRequest getById(UUID id) {
    log.debug("getById:: Loading Mediated Batch Request by ID [id: {}]", id);

    return repository.findById(id)
      .orElseThrow(() -> new MediatedBatchRequestNotFoundException(id));
  }

  private Page<MediatedBatchRequest> findEntities(String query, Integer offset, Integer limit) {
    if (isBlank(query)) {
      return repository.findAll(new OffsetRequest(offset, limit));
    }

    return repository.findByCql(query, new OffsetRequest(offset, limit));
  }

  private void updateRequestSplitEntities(MediatedBatchRequest batchRequest, List<MediatedBatchRequestSplit> requestSplits) {
    var patronComments = "%s\n\n\nBatch request ID: %s"
      .formatted(batchRequest.getPatronComments(), batchRequest.getId());
    for (var splitEntity : requestSplits) {
      initId(splitEntity);
      splitEntity.setMediatedBatchRequest(batchRequest);
      splitEntity.setRequesterId(batchRequest.getRequesterId());
      splitEntity.setPatronComments(patronComments);
    }
  }
}
