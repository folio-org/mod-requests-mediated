package org.folio.mr.controller.delegate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.flow.api.Flow;
import org.folio.flow.api.FlowEngine;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailsDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.domain.dto.MediatedBatchRequestsDto;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.domain.mapper.MediatedBatchRequestMapper;
import org.folio.mr.service.MediatedBatchRequestFlowProvider;
import org.folio.mr.service.MediatedBatchRequestSplitService;
import org.folio.mr.service.MediatedBatchRequestsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;


@ExtendWith(MockitoExtension.class)
class BatchRequestsServiceDelegateTest {

  @Mock
  private MediatedBatchRequestsService batchRequestsService;
  @Mock
  private MediatedBatchRequestSplitService requestSplitService;
  @Mock
  private MediatedBatchRequestMapper mapper;
  @Mock
  private FlowEngine flowEngine;
  @Mock
  private MediatedBatchRequestFlowProvider flowProvider;

  @InjectMocks
  private BatchRequestsServiceDelegate delegate;

  @Test
  void retrieveBatchRequestsCollection_positive_shouldReturnDtoCollection() {
    var query = "test";
    var offset = 0;
    var limit = 10;
    var entitiesPage = mock(Page.class);
    var expectedDto = mock(MediatedBatchRequestsDto.class);
    when(batchRequestsService.getAll(query, offset, limit)).thenReturn(entitiesPage);
    when(mapper.toMediatedBatchRequestsCollection(entitiesPage)).thenReturn(expectedDto);

    var result = delegate.retrieveBatchRequestsCollection(query, offset, limit);

    assertEquals(expectedDto, result);
  }

  @Test
  void createBatchRequest_positive_shouldCreateAndReturnDto() {
    var postDto = mock(MediatedBatchRequestPostDto.class);
    var batchEntity = mock(MediatedBatchRequest.class);
    var batchSplits = List.of(new MediatedBatchRequestSplit());
    var createdEntity = mock(MediatedBatchRequest.class);
    var flow = mock(Flow.class);
    var expectedDto = mock(MediatedBatchRequestDto.class);
    when(mapper.mapPostDtoToEntity(postDto)).thenReturn(batchEntity);
    when(mapper.mapPostDtoToSplitEntities(postDto)).thenReturn(batchSplits);
    when(batchRequestsService.create(batchEntity, batchSplits)).thenReturn(createdEntity);
    when(flowProvider.createFlow(any())).thenReturn(flow);
    when(mapper.toDto(createdEntity)).thenReturn(expectedDto);

    var result = delegate.createBatchRequest(postDto);

    assertEquals(expectedDto, result);
    verify(flowEngine).executeAsync(flow);
  }

  @Test
  void getBatchRequestById_positive_shouldReturnDto() {
    var id = UUID.randomUUID();
    var entity = mock(MediatedBatchRequest.class);
    var expectedDto = mock(MediatedBatchRequestDto.class);
    when(batchRequestsService.getById(id)).thenReturn(entity);
    when(mapper.toDto(entity)).thenReturn(expectedDto);

    var result = delegate.getBatchRequestById(id);

    assertEquals(expectedDto, result);
  }

  @Test
  void getBatchRequestDetailsByBatchId_positive_shouldReturnDetailsDto() {
    var batchId = UUID.randomUUID();
    var offset = 0;
    var limit = 10;
    var batchSplitEntities = mock(Page.class);
    var expectedDto = mock(MediatedBatchRequestDetailsDto.class);
    when(requestSplitService.getAllByBatchId(batchId, offset, limit)).thenReturn(batchSplitEntities);
    when(mapper.toMediatedBatchRequestDetailsCollection(batchSplitEntities)).thenReturn(expectedDto);

    var result = delegate.getBatchRequestDetailsByBatchId(batchId, offset, limit);

    assertEquals(expectedDto, result);
  }

  @Test
  void getBatchRequestDetailsByQuery_positive_shouldReturnDetailsDto() {
    var query = "query";
    var offset = 0;
    var limit = 10;
    var batchSplitEntities = mock(Page.class);
    var expectedDto = mock(MediatedBatchRequestDetailsDto.class);
    when(requestSplitService.getAll(query, offset, limit)).thenReturn(batchSplitEntities);
    when(mapper.toMediatedBatchRequestDetailsCollection(batchSplitEntities)).thenReturn(expectedDto);

    var result = delegate.retrieveBatchRequestDetailsCollection(query, offset, limit);

    assertEquals(expectedDto, result);
  }
}
