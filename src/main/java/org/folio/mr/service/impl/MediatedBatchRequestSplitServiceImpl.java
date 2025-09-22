package org.folio.mr.service.impl;

import static org.folio.mr.exception.ExceptionFactory.notFound;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailsDto;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.domain.mapper.MediatedBatchRequestMapper;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.folio.mr.service.MediatedBatchRequestSplitService;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class MediatedBatchRequestSplitServiceImpl implements MediatedBatchRequestSplitService {

  private final MediatedBatchRequestSplitRepository repository;
  private final MediatedBatchRequestRepository batchRequestRepository;
  private final MediatedBatchRequestMapper mapper;

  @Override
  @Transactional
  public void create(List<MediatedBatchRequestSplit> requestSplits) {
    repository.saveAll(requestSplits);
  }

  @Override
  public MediatedBatchRequestDetailsDto getAllByBatchId(UUID batchId, Integer offset, Integer limit) {
    log.debug("getAllByBatchId:: Attempts to find all Batch Request Details by [offset: {}, limit: {}, batchId: {}]",
      offset, limit, batchId);
    if (batchRequestRepository.findById(batchId).isEmpty()) {
      throw notFound(String.format("Batch request not found by ID: %s", batchId));
    }

    var entitiesPage = repository.findAllByBatchId(batchId, new OffsetRequest(offset, limit));
    return mapper.toMediatedBatchRequestDetailsCollection(entitiesPage);
  }
}
