package org.folio.mr.service;

import java.util.UUID;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.springframework.data.domain.Page;

public interface MediatedBatchRequestsService {

  MediatedBatchRequest create(MediatedBatchRequest batchRequestPostDto);

  Page<MediatedBatchRequest> getAll(String query, Integer offset, Integer limit);

  MediatedBatchRequest getById(UUID id);
}
