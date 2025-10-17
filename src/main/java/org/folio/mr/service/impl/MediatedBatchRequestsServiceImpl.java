package org.folio.mr.service.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.mr.support.ServiceUtils.initId;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.flow.api.FlowEngine;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.domain.dto.MediatedBatchRequestsDto;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.domain.mapper.MediatedBatchRequestMapper;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.service.MediatedBatchRequestFlowProvider;
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
  private final MediatedBatchRequestMapper mapper;
  private final MediatedBatchRequestSplitService requestSplitService;
  private final FlowEngine flowEngine;
  private final MediatedBatchRequestFlowProvider flowProvider;

  @Override
  @Transactional
  public MediatedBatchRequestDto create(MediatedBatchRequestPostDto batchRequestPostDto) {
    log.debug("create:: Attempting to create Mediated Batch Request [dto: {}]", batchRequestPostDto);

    var batchEntity = mapper.mapPostDtoToEntity(batchRequestPostDto);
    var requestSplits = mapper.mapPostDtoToSplitEntities(batchRequestPostDto);
    initId(batchEntity);

    var saved = repository.saveAndFlush(batchEntity);

    updateRequestSplitEntities(saved, requestSplits);
    requestSplitService.create(requestSplits);

    var flow = flowProvider.createFlow(saved.getId());
    flowEngine.executeAsync(flow);

    return mapper.toDto(saved);
  }

  @Override
  public MediatedBatchRequestsDto getAll(String query, Integer offset, Integer limit) {
    log.debug("getAll:: Attempts to find all Mediated Batch Requests by [offset: {}, limit: {}, query: {}]",
      offset, limit, query);

    var entitiesPage = findEntities(query, offset, limit);
    return mapper.toMediatedBatchRequestsCollection(entitiesPage);
  }

  @Override
  public MediatedBatchRequestDto getById(UUID id) {
    log.debug("getById:: Loading Mediated Batch Request by ID [id: {}]", id);

    return repository.findById(id)
      .map(mapper::toDto)
      .orElseThrow(() -> new MediatedBatchRequestNotFoundException(id));
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

  private Page<MediatedBatchRequest> findEntities(String query, Integer offset, Integer limit) {
    if (isBlank(query)) {
      return repository.findAll(new OffsetRequest(offset, limit));
    }

    return repository.findByCql(query, new OffsetRequest(offset, limit));
  }
}
