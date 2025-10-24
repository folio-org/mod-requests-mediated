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
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.folio.mr.service.impl.MediatedBatchRequestSplitServiceImpl;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;

class MediatedBatchRequestSplitServiceTest {

  @Mock
  private MediatedBatchRequestSplitRepository splitRepository;
  @Mock
  private MediatedBatchRequestRepository batchRequestRepository;

  @InjectMocks
  private MediatedBatchRequestSplitServiceImpl service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void shouldCreateBatchRequestSplit() {
    var splits = List.of(new MediatedBatchRequestSplit());

    service.create(splits);

    verify(splitRepository).saveAll(splits);
  }

  @Test
  void shouldGetAllByBatchId() {
    var batchId = UUID.randomUUID();
    var page = mock(Page.class);

    when(batchRequestRepository.findById(batchId)).thenReturn(Optional.of(MediatedBatchRequest.builder().build()));
    when(splitRepository.findAllByBatchId(eq(batchId), any(OffsetRequest.class))).thenReturn(page);

    var result = service.getAllByBatchId(batchId, 0, 10);

    assertEquals(page, result);
  }

  @Test
  void shouldProduceNotFoundOnGetAllByBatchId() {
    var batchId = UUID.randomUUID();
    when(batchRequestRepository.findById(batchId)).thenReturn(Optional.empty());

    var ex = assertThrows(MediatedBatchRequestNotFoundException.class, () -> service.getAllByBatchId(batchId, 0, 10));
    assertTrue(ex.getMessage().contains("Mediated Batch Request with ID [%s] was not found".formatted(batchId)));
  }
}
