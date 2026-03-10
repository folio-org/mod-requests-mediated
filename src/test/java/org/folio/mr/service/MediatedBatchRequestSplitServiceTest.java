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
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.domain.entity.projection.BatchRequestStats;
import org.folio.mr.domain.mapper.MediatedBatchRequestMapper;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.folio.mr.service.impl.MediatedBatchRequestSplitServiceImpl;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class MediatedBatchRequestSplitServiceTest {

  @InjectMocks private MediatedBatchRequestSplitServiceImpl service;
  @Mock private MediatedBatchRequestSplitRepository splitRepository;
  @Mock private MediatedBatchRequestRepository batchRequestRepository;
  @Mock private MediatedBatchRequestMapper mapper;

  @Test
  void shouldCreateBatchRequestSplit() {
    var splits = List.of(new MediatedBatchRequestSplit());

    service.create(splits);

    verify(splitRepository).saveAll(splits);
  }

  @Test
  void shouldGetPageByBatchId() {
    var batchId = UUID.randomUUID();
    var splitEntity = MediatedBatchRequestSplit.builder().id(UUID.randomUUID()).build();
    var detailDto = new MediatedBatchRequestDetailDto().itemId(UUID.randomUUID().toString());
    var page = new PageImpl<>(List.of(splitEntity));

    when(batchRequestRepository.findById(batchId)).thenReturn(Optional.of(MediatedBatchRequest.builder().build()));
    when(splitRepository.findAllByBatchId(eq(batchId), any(OffsetRequest.class))).thenReturn(page);
    when(mapper.toDto(splitEntity)).thenReturn(detailDto);

    var result = service.getPageByBatchId(batchId, 0, 10);

    assertEquals(1, result.getTotalElements());
    assertEquals(detailDto, result.getContent().get(0));
  }

  @Test
  void shouldProduceNotFoundOnGetPageByBatchId() {
    var batchId = UUID.randomUUID();
    when(batchRequestRepository.findById(batchId)).thenReturn(Optional.empty());

    var ex = assertThrows(MediatedBatchRequestNotFoundException.class, () -> service.getPageByBatchId(batchId, 0, 10));
    assertTrue(ex.getMessage().contains("Mediated Batch Request with ID [%s] was not found".formatted(batchId)));
  }

  @Test
  void shouldGetAllByBatchId() {
    var batchId = UUID.randomUUID();
    var splitEntity = MediatedBatchRequestSplit.builder().id(UUID.randomUUID()).build();
    var detailDto = new MediatedBatchRequestDetailDto().itemId(UUID.randomUUID().toString());
    when(splitRepository.findAllByBatchId(batchId)).thenReturn(List.of(splitEntity));
    when(mapper.toDto(splitEntity)).thenReturn(detailDto);

    var result = service.getAllByBatchId(batchId);

    assertEquals(1, result.size());
    assertEquals(splitEntity.getId(), result.get(0).id());
    assertEquals(detailDto, result.get(0).mediatedBatchRequest());
  }

  @Test
  void shouldGetPagedBatchRequestSplits() {
    var offset = 0;
    var limit = 10;
    var splitEntity = MediatedBatchRequestSplit.builder().id(UUID.randomUUID()).build();
    var detailDto = new MediatedBatchRequestDetailDto().itemId(UUID.randomUUID().toString());
    var expected = new PageImpl<>(List.of(splitEntity));
    when(splitRepository.findAll(any(OffsetRequest.class))).thenReturn(expected);
    when(mapper.toDto(splitEntity)).thenReturn(detailDto);

    var result = service.getAll("", offset, limit);

    assertEquals(1, result.getTotalElements());
    assertEquals(detailDto, result.getContent().get(0));
  }

  @Test
  void shouldGetPagedBatchRequestSplitsByQuery() {
    var offset = 0;
    var limit = 10;
    var query = "requesterId==some-id";
    var splitEntity = MediatedBatchRequestSplit.builder().id(UUID.randomUUID()).build();
    var detailDto = new MediatedBatchRequestDetailDto().itemId(UUID.randomUUID().toString());
    var expected = new PageImpl<>(List.of(splitEntity));
    when(splitRepository.findByCql(eq(query), any(OffsetRequest.class))).thenReturn(expected);
    when(mapper.toDto(splitEntity)).thenReturn(detailDto);

    var result = service.getAll(query, offset, limit);

    assertEquals(1, result.getTotalElements());
    assertEquals(detailDto, result.getContent().get(0));
  }

  @Test
  void shouldGetBatchRequestStats() {
    var batchId = UUID.randomUUID();
    var expectedStats = mock(BatchRequestStats.class);
    when(splitRepository.findMediatedBatchRequestStats(batchId)).thenReturn(expectedStats);

    var result = service.getBatchRequestStats(batchId);

    assertEquals(expectedStats, result);
  }
}
