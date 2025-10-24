package org.folio.mr.service;

import java.util.List;
import java.util.UUID;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.springframework.data.domain.Page;

public interface MediatedBatchRequestsService {

  MediatedBatchRequest create(MediatedBatchRequest batchRequest, List<MediatedBatchRequestSplit> batchSplits);

  Page<MediatedBatchRequest> getAll(String query, Integer offset, Integer limit);

  MediatedBatchRequest getById(UUID id);
}
