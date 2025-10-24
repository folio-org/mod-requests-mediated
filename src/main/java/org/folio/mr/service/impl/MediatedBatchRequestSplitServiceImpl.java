package org.folio.mr.service.impl;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
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
}
