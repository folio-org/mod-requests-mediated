package org.folio.mr.service;

import java.util.List;
import java.util.UUID;

import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.dto.IdentifiableMediatedBatchSplit;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.entity.projection.BatchRequestStats;
import org.springframework.data.domain.Page;

public interface MediatedBatchRequestSplitService {

  void updateStatusById(UUID id, BatchRequestSplitStatus status);

  List<IdentifiableMediatedBatchSplit> getAllByBatchId(UUID batchId);

  Page<MediatedBatchRequestDetailDto> getPageByBatchId(UUID batchId, Integer offset, Integer limit);

  Page<MediatedBatchRequestDetailDto> getAll(String query, Integer offset, Integer limit);

  BatchRequestStats getBatchRequestStats(UUID batchId);

  /**
   * Marks all split requests that are not in a completed state as failed for the given batch request.
   *
   * @param batchId - batch request identifier
   * @param errorMessage - error message to be set on the failed split requests
   */
  void markNotCompletedRequestsAsFailed(UUID batchId, String errorMessage);

  /**
   * Retrieves split request entity by request identifier.
   *
   * @param splitEntityId - entity identifier
   * @return {@link MediatedBatchRequestDetailDto} object
   * @throws org.folio.mr.exception.MediatedBatchRequestSplitNotFoundException if entity not found
   */
  MediatedBatchRequestDetailDto getById(UUID splitEntityId);

  void update(UUID splitRequestId, MediatedBatchRequestDetailDto splitRequest);
}
