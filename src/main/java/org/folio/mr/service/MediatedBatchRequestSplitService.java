package org.folio.mr.service;

import java.util.List;
import java.util.UUID;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailsDto;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;

public interface MediatedBatchRequestSplitService {

  void create(List<MediatedBatchRequestSplit> requestSplits);

  MediatedBatchRequestDetailsDto getAllByBatchId(UUID batchId, Integer offset, Integer limit);
}
