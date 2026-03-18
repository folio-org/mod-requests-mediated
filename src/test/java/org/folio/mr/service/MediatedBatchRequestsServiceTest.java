package org.folio.mr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.folio.mr.domain.BatchRequestStatus;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto.MediatedRequestStatusEnum;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.domain.dto.Parameter;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.domain.mapper.MediatedBatchRequestMapper;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.exception.MediatedBatchRequestValidationException;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.folio.mr.service.impl.MediatedBatchRequestsServiceImpl;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class MediatedBatchRequestsServiceTest {

  @InjectMocks private MediatedBatchRequestsServiceImpl service;
  @Mock private MediatedBatchRequestMapper mapper;
  @Mock private MediatedBatchRequestRepository repository;
  @Mock private MediatedBatchRequestSplitRepository batchRequestSplitRepository;

  @Test
  void shouldCreateBatchRequest() {
    var postDto = new MediatedBatchRequestPostDto();
    var batchEntity = new MediatedBatchRequest();
    batchEntity.setPatronComments("comment");
    batchEntity.setRequesterId(UUID.randomUUID());
    var savedEntity = new MediatedBatchRequest();
    savedEntity.setId(UUID.randomUUID());
    savedEntity.setPatronComments(batchEntity.getPatronComments());
    savedEntity.setRequesterId(batchEntity.getRequesterId());
    var createdDto = new MediatedBatchRequestDto().batchId(savedEntity.getId().toString());
    var batchSplits = List.of(MediatedBatchRequestSplit.builder().build());

    when(mapper.mapPostDtoToEntity(postDto)).thenReturn(batchEntity);
    when(mapper.mapPostDtoToSplitEntities(postDto)).thenReturn(batchSplits);
    when(repository.save(any(MediatedBatchRequest.class))).thenReturn(savedEntity);
    when(mapper.toDto(savedEntity)).thenReturn(createdDto);
    var entityCaptor = ArgumentCaptor.forClass(MediatedBatchRequest.class);
    var splitsCaptor = ArgumentCaptor.<List<MediatedBatchRequestSplit>>captor();

    var result = service.create(postDto);

    assertEquals(createdDto, result);
    verify(repository).save(entityCaptor.capture());
    assertNotNull(entityCaptor.getValue().getId());

    verify(batchRequestSplitRepository).saveAll(splitsCaptor.capture());
    var splitsPassed = splitsCaptor.getValue();
    assertEquals(splitsPassed.size(), batchSplits.size());
    var split = splitsPassed.getFirst();
    assertEquals("comment\n\n\nBatch request ID: " + savedEntity.getId(), split.getPatronComments());
    assertEquals(batchEntity.getRequesterId(), split.getRequesterId());
    assertEquals(savedEntity, split.getMediatedBatchRequest());
  }

  @Test
  void shouldCreateBatchRequestWithPredefinedId() {
    var batchId = UUID.randomUUID();
    var postDto = new MediatedBatchRequestPostDto().batchId(batchId.toString());
    var batchEntity = new MediatedBatchRequest();
    batchEntity.setPatronComments("comment");
    batchEntity.setRequesterId(batchId);
    var savedEntity = new MediatedBatchRequest();
    savedEntity.setId(batchId);
    savedEntity.setPatronComments(batchEntity.getPatronComments());
    savedEntity.setRequesterId(batchEntity.getRequesterId());
    var createdDto = new MediatedBatchRequestDto().batchId(savedEntity.getId().toString());
    var batchSplits = List.of(MediatedBatchRequestSplit.builder().build());

    when(mapper.mapPostDtoToEntity(postDto)).thenReturn(batchEntity);
    when(mapper.mapPostDtoToSplitEntities(postDto)).thenReturn(batchSplits);
    when(repository.existsById(batchId)).thenReturn(false);
    when(repository.save(any(MediatedBatchRequest.class))).thenReturn(savedEntity);
    when(mapper.toDto(savedEntity)).thenReturn(createdDto);
    var entityCaptor = ArgumentCaptor.forClass(MediatedBatchRequest.class);
    var splitsCaptor = ArgumentCaptor.<List<MediatedBatchRequestSplit>>captor();

    var result = service.create(postDto);

    assertEquals(createdDto, result);
    verify(repository).save(entityCaptor.capture());
    assertNotNull(entityCaptor.getValue().getId());

    verify(batchRequestSplitRepository).saveAll(splitsCaptor.capture());
    var splitsPassed = splitsCaptor.getValue();
    assertEquals(splitsPassed.size(), batchSplits.size());
    var split = splitsPassed.getFirst();
    assertEquals("comment\n\n\nBatch request ID: " + savedEntity.getId(), split.getPatronComments());
    assertEquals(batchEntity.getRequesterId(), split.getRequesterId());
    assertEquals(savedEntity, split.getMediatedBatchRequest());
  }

  @Test
  void shouldNotCreateBatchRequestWithExistingId() {
    var batchId = UUID.randomUUID();
    var request = new MediatedBatchRequestPostDto().batchId(batchId.toString());

    when(repository.existsById(batchId)).thenReturn(true);

    var err = assertThrows(MediatedBatchRequestValidationException.class, () -> service.create(request));
    var expectedParameter = new Parameter().key("batchId").value(batchId.toString());
    assertEquals(List.of(expectedParameter), err.getParameters());
  }

  @Test
  void shouldGetAllWithBlankQuery() {
    var offset = 0;
    var limit = 10;
    var entity = new MediatedBatchRequest();
    var dto = new MediatedBatchRequestDto().batchId(UUID.randomUUID().toString());
    var page = new PageImpl<>(List.of(entity));

    when(repository.findAll(any(OffsetRequest.class))).thenReturn(page);
    when(mapper.toDto(entity)).thenReturn(dto);

    var result = service.getAll("", offset, limit);

    assertEquals(1, result.getTotalElements());
    assertEquals(dto, result.getContent().getFirst());
    verify(repository).findAll(any(OffsetRequest.class));
  }

  @Test
  void shouldGetAllWithQuery() {
    var offset = 0;
    var limit = 10;
    var query = "someQuery";
    var entity = new MediatedBatchRequest();
    var dto = new MediatedBatchRequestDto().batchId(UUID.randomUUID().toString());
    var page = new PageImpl<>(List.of(entity));

    when(repository.findByCql(eq(query), any(OffsetRequest.class))).thenReturn(page);
    when(mapper.toDto(entity)).thenReturn(dto);

    var result = service.getAll(query, offset, limit);

    assertEquals(1, result.getTotalElements());
    assertEquals(dto, result.getContent().getFirst());
    verify(repository).findByCql(eq(query), any(OffsetRequest.class));
  }

  @Test
  void shouldGetById() {
    var id = UUID.randomUUID();
    var entity = new MediatedBatchRequest();
    var dto = new MediatedBatchRequestDto().batchId(id.toString());

    when(repository.findById(id)).thenReturn(Optional.of(entity));
    when(mapper.toDto(entity)).thenReturn(dto);

    var result = service.getById(id);

    assertEquals(dto, result);
    verify(repository).findById(id);
  }

  @Test
  void shouldProduceNotFoundOnGetById() {
    var id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    var ex = assertThrows(MediatedBatchRequestNotFoundException.class, () -> service.getById(id));
    assertTrue(ex.getMessage().contains("Mediated Batch Request with ID [%s] was not found".formatted(id)));
  }

  @Test
  void shouldUpdateStatusById() {
    var batchId = UUID.randomUUID();
    var entity = mock(MediatedBatchRequest.class);
    when(repository.findById(batchId)).thenReturn(Optional.of(entity));

    service.updateStatusById(batchId, MediatedRequestStatusEnum.IN_PROGRESS);

    verify(entity).setStatus(BatchRequestStatus.IN_PROGRESS);
    verify(repository).save(entity);
  }

  @Test
  void shouldNotUpdateStatusByIdForNotFoundEntity() {
    var batchId = UUID.randomUUID();
    when(repository.findById(batchId)).thenReturn(Optional.empty());

    assertThrows(MediatedBatchRequestNotFoundException.class,
      () -> service.updateStatusById(batchId, MediatedRequestStatusEnum.IN_PROGRESS));


    verify(repository, never()).save(any());
  }
}
