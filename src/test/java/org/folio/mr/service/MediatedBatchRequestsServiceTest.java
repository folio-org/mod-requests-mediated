package org.folio.mr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.service.impl.MediatedBatchRequestsServiceImpl;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;


class MediatedBatchRequestsServiceTest {

  @Mock
  private MediatedBatchRequestRepository repository;

  @InjectMocks
  private MediatedBatchRequestsServiceImpl service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void shouldCreateBatchRequest() {
    var batchEntity = new MediatedBatchRequest();
    var savedEntity = new MediatedBatchRequest();

    when(repository.save(any(MediatedBatchRequest.class))).thenReturn(savedEntity);
    var captor = ArgumentCaptor.forClass(MediatedBatchRequest.class);

    var result = service.create(batchEntity);

    assertEquals(savedEntity, result);
    verify(repository).save(captor.capture());
    assertNotNull(captor.getValue().getId());
  }

  @Test
  void shouldGetAllWithBlankQuery() {
    var offset = 0;
    var limit = 10;
    var page = mock(Page.class);

    when(repository.findAll(any(OffsetRequest.class))).thenReturn(page);

    var result = service.getAll("", offset, limit);

    assertEquals(page, result);
    verify(repository).findAll(any(OffsetRequest.class));
  }

  @Test
  void shouldGetAllWithQuery() {
    var offset = 0;
    var limit = 10;
    var query = "someQuery";
    var page = mock(Page.class);

    when(repository.findByCql(eq(query), any(OffsetRequest.class))).thenReturn(page);

    var result = service.getAll(query, offset, limit);

    assertEquals(page, result);
    verify(repository).findByCql(eq(query), any(OffsetRequest.class));
  }

  @Test
  void shouldGetById() {
    var id = UUID.randomUUID();
    var entity = new MediatedBatchRequest();

    when(repository.findById(id)).thenReturn(Optional.of(entity));

    var result = service.getById(id);

    assertEquals(entity, result);
    verify(repository).findById(id);
  }

  @Test
  void shouldProduceNotFoundOnGetById() {
    var id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    var ex = assertThrows(MediatedBatchRequestNotFoundException.class, () -> service.getById(id));
    assertTrue(ex.getMessage().contains("Mediated Batch Request with ID [%s] was not found".formatted(id)));
  }
}
