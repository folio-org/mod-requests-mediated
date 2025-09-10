package org.folio.mr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.folio.mr.api.BaseIT;
import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.BatchRequestStatus;
import org.folio.mr.domain.entity.BatchRequest;
import org.folio.mr.domain.entity.BatchRequestSplit;
import org.folio.mr.domain.entity.MetadataEntity;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class BatchRequestSplitRepositoryIT extends BaseIT {

  @MockitoSpyBean
  private FolioExecutionContext context;

  @Autowired
  private BatchRequestRepository batchRequestRepository;

  @Autowired
  private BatchRequestSplitRepository batchRequestSplitRepository;

  @Test
  void batchRequestSplitMetadataShouldBeGeneratedWhenSaveEntity() {
    var randomUUID = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var batch = BatchRequest.builder()
      .id(randomUUID).requesterId(randomUUID)
      .status(BatchRequestStatus.IN_PROGRESS)
      .requestDate(Timestamp.from(Instant.now()))
      .build();
    var requestSplit = BatchRequestSplit.builder()
      .id(randomUUID)
      .batchRequest(batch)
      .requesterId(randomUUID)
      .status(BatchRequestSplitStatus.COMPLETED).build();
    when(context.getUserId()).thenReturn(userId);

    var savedBatch = batchRequestRepository.save(batch);
    var savedSplit = batchRequestSplitRepository.save(requestSplit);

    assertThat(List.of(savedBatch, savedSplit))
      .extracting(MetadataEntity::getCreatedByUserId, MetadataEntity::getCreatedDate,
        MetadataEntity::getUpdatedByUserId, MetadataEntity::getUpdatedDate)
      .doesNotContainNull();

    assertThat(List.of(savedBatch, savedSplit))
      .extracting(MetadataEntity::getCreatedByUserId)
      .containsOnly(userId, userId);
  }
}
