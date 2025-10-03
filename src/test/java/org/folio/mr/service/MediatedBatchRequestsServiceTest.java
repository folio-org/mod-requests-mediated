package org.folio.mr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.flow.api.FlowEngine;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.domain.dto.MediatedBatchRequestsDto;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.domain.mapper.MediatedBatchRequestMapper;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.service.impl.MediatedBatchRequestsServiceImpl;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;


class MediatedBatchRequestsServiceTest {

  @Mock
  private MediatedBatchRequestRepository repository;
  @Mock
  private MediatedBatchRequestMapper mapper;
  @Mock
  private MediatedBatchRequestSplitService requestSplitService;
  @Mock
  private FlowEngine flowEngine;
  @Mock
  private MediatedBatchRequestFlowProvider flowProvider;

  @InjectMocks
  private MediatedBatchRequestsServiceImpl service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void shouldCreateBatchRequest() {
    var postDto = new MediatedBatchRequestPostDto();
    var batchEntity = new MediatedBatchRequest();
    var splitEntities = List.of(new MediatedBatchRequestSplit());
    var savedEntity = new MediatedBatchRequest();
    var dto = new MediatedBatchRequestDto();

    when(mapper.mapPostDtoToEntity(postDto)).thenReturn(batchEntity);
    when(mapper.mapPostDtoToSplitEntities(postDto)).thenReturn(splitEntities);
    when(repository.saveAndFlush(any(MediatedBatchRequest.class))).thenReturn(savedEntity);
    when(mapper.toDto(savedEntity)).thenReturn(dto);

    var result = service.create(postDto);

    assertEquals(dto, result);
    verify(requestSplitService).create(splitEntities);
    verify(flowProvider).createFlow(savedEntity.getId());
    verify(flowEngine).executeAsync(any());
  }

  @Test
  void shouldGetAllWithBlankQuery() {
    var offset = 0;
    var limit = 10;
    var page = mock(Page.class);
    var dto = new MediatedBatchRequestsDto();

    when(repository.findAll(any(OffsetRequest.class))).thenReturn(page);
    when(mapper.toMediatedBatchRequestsCollection(page)).thenReturn(dto);

    var result = service.getAll("", offset, limit);

    assertEquals(dto, result);
  }

  @Test
  void shouldGetAllWithQuery() {
    var offset = 0;
    var limit = 10;
    var query = "someQuery";
    var page = mock(Page.class);
    var dto = new MediatedBatchRequestsDto();

    when(repository.findByCql(eq(query), any(OffsetRequest.class))).thenReturn(page);
    when(mapper.toMediatedBatchRequestsCollection(page)).thenReturn(dto);

    var result = service.getAll(query, offset, limit);

    assertEquals(dto, result);
  }

  @Test
  void shouldGetById() {
    var id = UUID.randomUUID();
    var entity = new MediatedBatchRequest();
    var dto = new MediatedBatchRequestDto();

    when(repository.findById(id)).thenReturn(Optional.of(entity));
    when(mapper.toDto(entity)).thenReturn(dto);

    var result = service.getById(id);

    assertEquals(dto, result);
  }

  @Test
  void shouldProduceNotFoundOnGetById() {
    var id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    var ex = assertThrows(RuntimeException.class, () -> service.getById(id));
    assertTrue(ex.getMessage().contains("Mediated Batch Request not found by ID"));
  }
}
