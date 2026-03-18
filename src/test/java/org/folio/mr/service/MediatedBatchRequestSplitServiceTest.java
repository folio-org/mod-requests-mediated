package org.folio.mr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto.MediatedRequestStatusEnum;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.domain.entity.projection.BatchRequestStats;
import org.folio.mr.domain.mapper.MediatedBatchRequestMapper;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.exception.MediatedBatchRequestSplitNotFoundException;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.folio.mr.service.impl.MediatedBatchRequestSplitServiceImpl;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
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
    assertEquals(detailDto, result.getContent().getFirst());
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
    assertEquals(splitEntity.getId(), result.getFirst().id());
    assertEquals(detailDto, result.getFirst().mediatedBatchRequest());
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
    assertEquals(detailDto, result.getContent().getFirst());
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
    assertEquals(detailDto, result.getContent().getFirst());
  }

  @Test
  void shouldGetBatchRequestStats() {
    var batchId = UUID.randomUUID();
    var expectedStats = mock(BatchRequestStats.class);
    when(splitRepository.findMediatedBatchRequestStats(batchId)).thenReturn(expectedStats);

    var result = service.getBatchRequestStats(batchId);

    assertEquals(expectedStats, result);
  }

  @Test
  void updateStatusById_positive() {
    var id = UUID.randomUUID();
    var splitEntity = mock(MediatedBatchRequestSplit.class);
    when(splitRepository.findById(id)).thenReturn(Optional.of(splitEntity));

    service.updateStatusById(id, BatchRequestSplitStatus.IN_PROGRESS);

    verify(splitEntity).setStatus(BatchRequestSplitStatus.IN_PROGRESS);
    verify(splitRepository).save(splitEntity);
  }

  @Test
  void updateStatusById_negative_entityNotFound() {
    var id = UUID.randomUUID();
    var splitEntity = mock(MediatedBatchRequestSplit.class);
    when(splitRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(
      MediatedBatchRequestSplitNotFoundException.class,
      () -> service.updateStatusById(id, BatchRequestSplitStatus.IN_PROGRESS));

    verify(splitEntity, never()).setStatus(BatchRequestSplitStatus.IN_PROGRESS);
    verify(splitRepository, never()).save(splitEntity);
  }

  @Test
  void getById_positive() {
    var id = UUID.randomUUID();
    var splitEntity = mock(MediatedBatchRequestSplit.class);
    var splitDto = mock(MediatedBatchRequestDetailDto.class);

    when(mapper.toDto(splitEntity)).thenReturn(splitDto);
    when(splitRepository.findById(id)).thenReturn(Optional.of(splitEntity));

    var result = service.getById(id);

    assertEquals(result, splitDto);
  }

  @Test
  void getById_negative_entityNotFound() {
    var id = UUID.randomUUID();
    when(splitRepository.findById(id)).thenReturn(Optional.empty());
    assertThrows(MediatedBatchRequestSplitNotFoundException.class, () -> service.getById(id));
    verify(mapper, never()).toDto(ArgumentMatchers.<MediatedBatchRequestSplit>any());
  }

  @Test
  void markNotCompletedRequestsAsFailed_positive() {
    var split1 = MediatedBatchRequestSplit.builder()
      .id(UUID.randomUUID())
      .status(BatchRequestSplitStatus.IN_PROGRESS)
      .build();

    var split2 = MediatedBatchRequestSplit.builder()
      .id(UUID.randomUUID())
      .status(BatchRequestSplitStatus.COMPLETED)
      .confirmedRequestId(UUID.randomUUID())
      .build();

    var split3 =  MediatedBatchRequestSplit.builder()
      .id(UUID.randomUUID())
      .status(BatchRequestSplitStatus.COMPLETED)
      .confirmedRequestId(null)
      .build();

    var split4 = MediatedBatchRequestSplit.builder()
      .id(UUID.randomUUID())
      .status(BatchRequestSplitStatus.IN_PROGRESS)
      .confirmedRequestId(UUID.randomUUID())
      .build();


    var batchId = UUID.randomUUID();
    var captor = ArgumentCaptor.<List<MediatedBatchRequestSplit>>captor();
    when(splitRepository.findAllByBatchId(batchId)).thenReturn(List.of(split1, split2, split3, split4));
    when(splitRepository.saveAll(captor.capture())).thenAnswer(e -> e.getArgument(0));

    var errorDetails = "Execution Error";
    service.markNotCompletedRequestsAsFailed(batchId, errorDetails);

    var savedSplitRequests = captor.getValue();
    assertEquals(savedSplitRequests.size(), 4);
    assertEquals(BatchRequestSplitStatus.FAILED, savedSplitRequests.getFirst().getStatus());
    assertEquals(errorDetails, savedSplitRequests.getFirst().getErrorDetails());

    assertEquals(BatchRequestSplitStatus.COMPLETED, savedSplitRequests.get(1).getStatus());
    assertNull(savedSplitRequests.get(1).getErrorDetails());

    assertEquals(BatchRequestSplitStatus.COMPLETED, savedSplitRequests.get(2).getStatus());
    assertNull(savedSplitRequests.get(2).getErrorDetails());

    assertEquals(BatchRequestSplitStatus.IN_PROGRESS, savedSplitRequests.get(3).getStatus());
    assertNull(savedSplitRequests.get(3).getErrorDetails());
  }

  @Test
  void update_positive() {
    var splitRequestId = UUID.randomUUID();
    var confirmedRequestId = UUID.randomUUID();
    var splitRequest = new MediatedBatchRequestDetailDto()
      .errorDetails(null)
      .confirmedRequestId(confirmedRequestId.toString())
      .requestStatus(Request.StatusEnum.OPEN_AWAITING_PICKUP.getValue())
      .mediatedRequestStatus(MediatedRequestStatusEnum.COMPLETED);

    var splitEntity = mock(MediatedBatchRequestSplit.class);
    when(splitRepository.findById(splitRequestId)).thenReturn(Optional.of(splitEntity));

    service.update(splitRequestId, splitRequest);

    verify(splitEntity).setConfirmedRequestId(confirmedRequestId);
    verify(splitEntity).setErrorDetails(null);
    verify(splitEntity).setRequestStatus(Request.StatusEnum.OPEN_AWAITING_PICKUP.getValue());
    verify(splitEntity).setStatus(BatchRequestSplitStatus.COMPLETED);
    verify(splitEntity).setMediatedRequestStatus(MediatedRequestStatusEnum.COMPLETED.getValue());

    verify(splitRepository).save(splitEntity);
  }

  @Test
  void update_positive_failedRequest() {
    var splitRequestId = UUID.randomUUID();
    var splitRequest = new MediatedBatchRequestDetailDto()
      .errorDetails("Request execution failed")
      .confirmedRequestId(null)
      .requestStatus(null)
      .mediatedRequestStatus(MediatedRequestStatusEnum.FAILED);

    var splitEntity = mock(MediatedBatchRequestSplit.class);
    when(splitRepository.findById(splitRequestId)).thenReturn(Optional.of(splitEntity));

    service.update(splitRequestId, splitRequest);

    verify(splitEntity, never()).setConfirmedRequestId(any());
    verify(splitEntity).setErrorDetails("Request execution failed");
    verify(splitEntity).setRequestStatus(null);
    verify(splitEntity).setStatus(BatchRequestSplitStatus.FAILED);
    verify(splitEntity).setMediatedRequestStatus(MediatedRequestStatusEnum.FAILED.getValue());

    verify(splitRepository).save(splitEntity);
  }

  @Test
  void update_negative_entityNotFound() {
    var splitRequestId = UUID.randomUUID();
    var confirmedRequestId = UUID.randomUUID();
    var splitRequest = new MediatedBatchRequestDetailDto()
      .errorDetails(null)
      .confirmedRequestId(confirmedRequestId.toString())
      .requestStatus(Request.StatusEnum.OPEN_AWAITING_PICKUP.getValue())
      .mediatedRequestStatus(MediatedRequestStatusEnum.COMPLETED);

    when(splitRepository.findById(splitRequestId)).thenReturn(Optional.empty());

    assertThrows(MediatedBatchRequestSplitNotFoundException.class,
      () -> service.update(splitRequestId, splitRequest));
    verify(splitRepository, never()).save(any());
  }
}
