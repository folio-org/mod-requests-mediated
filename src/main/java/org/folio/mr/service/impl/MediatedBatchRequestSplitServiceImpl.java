package org.folio.mr.service.impl;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.mr.support.ServiceUtils.mapItems;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.dto.IdentifiableMediatedBatchSplit;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.domain.entity.projection.BatchRequestStats;
import org.folio.mr.domain.mapper.MediatedBatchRequestMapper;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.exception.MediatedBatchRequestSplitNotFoundException;
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

  private final MediatedBatchRequestMapper mapper;
  private final MediatedBatchRequestSplitRepository repository;
  private final MediatedBatchRequestRepository batchRequestRepository;

  @Override
  @Transactional
  public void updateStatusById(UUID id, BatchRequestSplitStatus status) {
    var entityById = getEntityById(id);
    entityById.setStatus(status);
    repository.save(entityById);
  }

  @Override
  @Transactional(readOnly = true)
  public List<IdentifiableMediatedBatchSplit> getAllByBatchId(UUID batchId) {
    return mapItems(repository.findAllByBatchId(batchId),
      entity -> new IdentifiableMediatedBatchSplit(entity.getId(), mapper.toDto(entity)));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<MediatedBatchRequestDetailDto> getPageByBatchId(UUID batchId, Integer offset, Integer limit) {
    log.debug("getAllByBatchId:: Attempts to find all Batch Request Details by [offset: {}, limit: {}, batchId: {}]",
      offset, limit, batchId);
    if (batchRequestRepository.findById(batchId).isEmpty()) {
      throw new MediatedBatchRequestNotFoundException(batchId);
    }

    return repository.findAllByBatchId(batchId, new OffsetRequest(offset, limit)).map(mapper::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<MediatedBatchRequestDetailDto> getAll(String query, Integer offset, Integer limit) {
    log.debug("getAll:: Attempts to find all Mediated Batch Requests Details by [offset: {}, limit: {}, query: {}]",
      offset, limit, query);

    return findEntities(query, offset, limit).map(mapper::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public BatchRequestStats getBatchRequestStats(UUID batchId) {
    return repository.findMediatedBatchRequestStats(batchId);
  }

  @Override
  public void markNotCompletedRequestsAsFailed(UUID batchId, String errorMessage) {
    var splitEntities = repository.findAllByBatchId(batchId);
    splitEntities.stream()
      .filter(split -> split.getStatus() != BatchRequestSplitStatus.COMPLETED)
      .filter(split -> split.getConfirmedRequestId() == null)
      .forEach(requestSplit -> {
        requestSplit.setStatus(BatchRequestSplitStatus.FAILED);
        requestSplit.setErrorDetails(errorMessage);
      });
    repository.saveAll(splitEntities);
  }

  @Override
  @Transactional(readOnly = true)
  public MediatedBatchRequestDetailDto getById(UUID splitEntityId) {
    return repository.findById(splitEntityId)
      .map(mapper::toDto)
      .orElseThrow(() -> new MediatedBatchRequestSplitNotFoundException(splitEntityId));
  }

  @Override
  @Transactional
  public void update(UUID id, MediatedBatchRequestDetailDto request) {
    var entity = getEntityById(id);

    var mediatedRequestStatus = request.getMediatedRequestStatus().getValue();
    if (request.getConfirmedRequestId() != null) {
      entity.setConfirmedRequestId(UUID.fromString(request.getConfirmedRequestId()));
    }
    entity.setErrorDetails(request.getErrorDetails());
    entity.setRequestStatus(request.getRequestStatus());
    entity.setStatus(BatchRequestSplitStatus.fromValue(mediatedRequestStatus));
    entity.setMediatedRequestStatus(mediatedRequestStatus);
    repository.save(entity);
  }

  private Page<MediatedBatchRequestSplit> findEntities(String query, Integer offset, Integer limit) {
    if (isBlank(query)) {
      return repository.findAll(new OffsetRequest(offset, limit));
    }

    return repository.findByCql(query, new OffsetRequest(offset, limit));
  }

  private MediatedBatchRequestSplit getEntityById(UUID id) {
    return repository.findById(id).orElseThrow(() -> new MediatedBatchRequestSplitNotFoundException(id));
  }
}
