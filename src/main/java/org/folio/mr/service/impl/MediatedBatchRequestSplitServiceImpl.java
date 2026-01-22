package org.folio.mr.service.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.domain.entity.projection.BatchRequestStats;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.folio.mr.service.MediatedBatchRequestSplitService;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class MediatedBatchRequestSplitServiceImpl implements MediatedBatchRequestSplitService {

  private final MediatedBatchRequestSplitRepository repository;
  private final MediatedBatchRequestRepository batchRequestRepository;

  @Override
  @Transactional
  public void create(List<MediatedBatchRequestSplit> requestSplits) {
    repository.saveAll(requestSplits);
  }

  @Override
  public Page<MediatedBatchRequestSplit> getAllByBatchId(UUID batchId, Integer offset, Integer limit) {
    log.debug("getAllByBatchId:: Attempts to find all Batch Request Details by [offset: {}, limit: {}, batchId: {}]",
      offset, limit, batchId);
    if (batchRequestRepository.findById(batchId).isEmpty()) {
      throw new MediatedBatchRequestNotFoundException(batchId);
    }

    return repository.findAllByBatchId(batchId, new OffsetRequest(offset, limit));
  }

  @Override
  public Page<MediatedBatchRequestSplit> getAll(String query, Integer offset, Integer limit) {
    log.debug("getAll:: Attempts to find all Mediated Batch Requests Details by [offset: {}, limit: {}, query: {}]",
      offset, limit, query);

    return findEntities(query, offset, limit);
  }

  @Override
  public BatchRequestStats getBatchRequestStats(UUID batchId) {
    return repository.findMediatedBatchRequestStats(batchId);
  }

  private Page<MediatedBatchRequestSplit> findEntities(String query, Integer offset, Integer limit) {
    if (isBlank(query)) {
      return repository.findAll(new OffsetRequest(offset, limit));
    }

    return repository.findByCql(query, new OffsetRequest(offset, limit));
  }
}
