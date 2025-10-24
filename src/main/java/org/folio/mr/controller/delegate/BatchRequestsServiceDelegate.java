package org.folio.mr.controller.delegate;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.folio.flow.api.FlowEngine;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailsDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.domain.dto.MediatedBatchRequestsDto;
import org.folio.mr.domain.mapper.MediatedBatchRequestMapper;
import org.folio.mr.service.MediatedBatchRequestFlowProvider;
import org.folio.mr.service.MediatedBatchRequestSplitService;
import org.folio.mr.service.MediatedBatchRequestsService;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class BatchRequestsServiceDelegate {

  private final MediatedBatchRequestsService batchRequestsService;
  private final MediatedBatchRequestSplitService requestSplitService;
  private final MediatedBatchRequestMapper mapper;
  private final FlowEngine flowEngine;
  private final MediatedBatchRequestFlowProvider flowProvider;

  public MediatedBatchRequestsDto retrieveBatchRequestsCollection(String query, Integer offset, Integer limit) {
    log.debug("retrieveBatchRequestsCollection:: parameters query: {}, offset: {}, limit: {}", query, offset, limit);

    var entitiesPage = batchRequestsService.getAll(query, offset, limit);
    return mapper.toMediatedBatchRequestsCollection(entitiesPage);
  }

  public MediatedBatchRequestDto createBatchRequest(MediatedBatchRequestPostDto batchRequestDto) {
    log.debug("createBatchRequest:: parameters batchRequestDto: {}", batchRequestDto);

    var batchEntity = mapper.mapPostDtoToEntity(batchRequestDto);
    var batchSplits = mapper.mapPostDtoToSplitEntities(batchRequestDto);

    var createdEntity = batchRequestsService.create(batchEntity, batchSplits);

    var flow = flowProvider.createFlow(createdEntity.getId());
    flowEngine.executeAsync(flow);

    return mapper.toDto(createdEntity);
  }

  public MediatedBatchRequestDto getBatchRequestById(UUID id) {
    log.debug("getBatchRequestById:: parameters id: {}", id);

    var entity = batchRequestsService.getById(id);

    return mapper.toDto(entity);
  }

  public MediatedBatchRequestDetailsDto getBatchRequestDetailsByBatchId(UUID batchId, Integer offset, Integer limit) {
    log.debug("getBatchRequestDetailsByBatchId:: parameters batchId: {}, offset: {}, limit: {}", batchId, offset, limit);

    var batchSplitEntities = requestSplitService.getAllByBatchId(batchId, offset, limit);
    return mapper.toMediatedBatchRequestDetailsCollection(batchSplitEntities);
  }
}
