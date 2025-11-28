package org.folio.mr.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.mr.domain.BatchRequestSplitStatus.COMPLETED;
import static org.folio.mr.domain.BatchRequestStatus.IN_PROGRESS;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.folio.mr.api.BaseIT;
import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.BatchRequestStatus;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.domain.entity.MetadataEntity;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class MediatedBatchRequestSplitRepositoryIT extends BaseIT {

  @MockitoSpyBean
  private FolioExecutionContext context;

  @Autowired
  private MediatedBatchRequestRepository batchRequestRepository;

  @Autowired
  private MediatedBatchRequestSplitRepository batchRequestSplitRepository;

  @Test
  void batchRequestSplitMetadataShouldBeGeneratedWhenSaveEntity() {
    var randomUUID = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var itemId = UUID.randomUUID();
    var pickupServicePointId = UUID.randomUUID();
    var requestDate = Timestamp.from(Instant.parse("2025-09-10T10:15:30.00Z"));
    var batch = MediatedBatchRequest.builder()
      .id(randomUUID)
      .requesterId(randomUUID)
      .status(BatchRequestStatus.fromValue("In progress"))
      .requestDate(requestDate)
      .build();
    var requestSplit = new MediatedBatchRequestSplit();
    requestSplit.setId(randomUUID);
    requestSplit.setMediatedBatchRequest(batch);
    requestSplit.setRequesterId(randomUUID);
    requestSplit.setItemId(itemId);
    requestSplit.setPickupServicePointId(pickupServicePointId);
    requestSplit.setStatus(BatchRequestSplitStatus.fromValue("Completed"));
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

    assertThat(savedBatch)
      .extracting(MediatedBatchRequest::getRequesterId, MediatedBatchRequest::getRequestDate, MediatedBatchRequest::getStatus)
      .containsExactly(randomUUID, requestDate, IN_PROGRESS);

    assertThat(savedSplit)
      .extracting(MediatedBatchRequestSplit::getRequesterId, MediatedBatchRequestSplit::getStatus)
      .containsExactly(randomUUID, COMPLETED);
  }
}
