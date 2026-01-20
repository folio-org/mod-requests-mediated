package org.folio.mr.service;

import java.util.List;
import java.util.UUID;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.domain.entity.projection.BatchRequestStats;
import org.springframework.data.domain.Page;

public interface MediatedBatchRequestSplitService {

  void create(List<MediatedBatchRequestSplit> requestSplits);

  Page<MediatedBatchRequestSplit> getAllByBatchId(UUID batchId, Integer offset, Integer limit);

  Page<MediatedBatchRequestSplit> getAll(String query, Integer offset, Integer limit);

  BatchRequestStats getBatchRequestStats(UUID batchId);
}
