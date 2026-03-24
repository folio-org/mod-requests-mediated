package org.folio.mr.service.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.mr.support.ServiceUtils.initId;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.mr.config.BatchRequestExecutionProperties;
import org.folio.mr.domain.BatchRequestStatus;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto.MediatedRequestStatusEnum;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.domain.mapper.MediatedBatchRequestMapper;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.exception.MediatedBatchRequestValidationException;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.folio.mr.service.MediatedBatchRequestsService;
import org.folio.spring.data.OffsetRequest;

@Log4j2
@Service
@RequiredArgsConstructor
public class MediatedBatchRequestsServiceImpl implements MediatedBatchRequestsService {

  private final MediatedBatchRequestMapper mapper;
  private final MediatedBatchRequestRepository repository;
  private final BatchRequestExecutionProperties batchRequestExecutionProperties;
  private final MediatedBatchRequestSplitRepository batchRequestSplitRepository;

  @Override
  @Transactional
  public MediatedBatchRequestDto create(MediatedBatchRequestPostDto batchRequestDto) {
    var entityId = batchRequestDto.getBatchId();
    if (entityId != null && repository.existsById(UUID.fromString(entityId))) {
      throw MediatedBatchRequestValidationException.requestExistsException(UUID.fromString(entityId));
    }

    var batchRequestEntity = mapper.mapPostDtoToEntity(batchRequestDto);
    var batchSplits = mapper.mapPostDtoToSplitEntities(batchRequestDto);
    log.debug("create:: Attempting to create Mediated Batch Request: {}", batchRequestEntity);

    initId(batchRequestEntity);

    var saved = repository.save(batchRequestEntity);

    updateRequestSplitEntities(saved, batchSplits);
    batchRequestSplitRepository.saveAll(batchSplits);

    return mapper.toDto(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<MediatedBatchRequestDto> getAll(String query, Integer offset, Integer limit) {
    log.debug("getAll:: Attempts to find all Mediated Batch Requests by [offset: {}, limit: {}, query: {}]",
      offset, limit, query);

    return findEntities(query, offset, limit).map(mapper::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public MediatedBatchRequestDto getById(UUID id) {
    log.debug("getById:: Loading Mediated Batch Request by ID [id: {}]", id);
    return mapper.toDto(getEntityById(id));
  }

  @Override
  @Transactional
  public void updateStatusById(UUID id, MediatedRequestStatusEnum status) {
    log.debug("updateStatusById:: Updating status for entity: {}", id);
    var entity = getEntityById(id);
    entity.setStatus(BatchRequestStatus.fromValue(status.getValue()));
    entity.setLastProcessedDate(Timestamp.from(Instant.now()));
    repository.save(entity);
  }

  @Override
  @Transactional
  public void updateLastProcessedDateById(UUID batchId) {
    var entityById = getEntityById(batchId);
    entityById.setLastProcessedDate(Timestamp.from(Instant.now()));
    repository.save(entityById);
  }

  @Override
  @Transactional
  public List<MediatedBatchRequestDto> getStaleBatchRequests() {
    var limit = batchRequestExecutionProperties.getStaleRequestsQueryLimit();
    var staleRequestThreshold = batchRequestExecutionProperties.getStaleRequestThreshold();
    var threshold = Timestamp.from(Instant.now().minus(staleRequestThreshold));
    log.debug("getStaleBatchRequests:: Searching for stuck batch requests older than {}", threshold);

    return repository.getStaleRequests(threshold, limit).stream().map(mapper::toDto).toList();
  }

  private MediatedBatchRequest getEntityById(UUID id) {
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
