package org.folio.mr.service;

import java.util.UUID;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.domain.dto.MediatedBatchRequestsDto;

public interface MediatedBatchRequestsService {

  MediatedBatchRequestDto create(MediatedBatchRequestPostDto batchRequestPostDto);

  MediatedBatchRequestsDto getAll(String query, Integer offset, Integer limit);

  MediatedBatchRequestDto getById(UUID id);
}
