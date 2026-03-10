package org.folio.mr.service;

import java.util.UUID;

import org.springframework.data.domain.Page;

import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto.MediatedRequestStatusEnum;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;

public interface MediatedBatchRequestsService {

  MediatedBatchRequestDto create(MediatedBatchRequestPostDto mediatedBatchRequest);

  Page<MediatedBatchRequestDto> getAll(String query, Integer offset, Integer limit);

  MediatedBatchRequestDto getById(UUID id);

  /**
   * Updates status for entity using id.
   *
   * @param id     - entity identifier
   * @param status - new status for entity
   * @throws MediatedBatchRequestNotFoundException if entity is not found by id
   */
  void updateStatusById(UUID id, MediatedRequestStatusEnum status);
}
