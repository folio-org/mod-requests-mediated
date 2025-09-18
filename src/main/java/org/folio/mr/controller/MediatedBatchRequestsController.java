package org.folio.mr.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailsDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.domain.dto.MediatedBatchRequestsDto;
import org.folio.mr.rest.resource.MediatedBatchRequestsApi;
import org.folio.mr.service.MediatedBatchRequestSplitService;
import org.folio.mr.service.MediatedBatchRequestsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
@AllArgsConstructor
public class MediatedBatchRequestsController implements MediatedBatchRequestsApi {

  private final MediatedBatchRequestsService service;
  private final MediatedBatchRequestSplitService requestSplitService;

  @Override
  public ResponseEntity<MediatedBatchRequestDto> postBatchMediatedRequests(MediatedBatchRequestPostDto batchRequestPostDto) {
    log.info("Creating mediated batch request");
    var createdBatch = service.create(batchRequestPostDto);
    return ResponseEntity.status(CREATED).body(createdBatch);
  }

  @Override
  public ResponseEntity<MediatedBatchRequestsDto> getMediatedBatchRequestCollection(String query, Integer offset, Integer limit) {
    var collectionDto = service.getAll(query, offset, limit);
    return ResponseEntity.status(OK).body(collectionDto);
  }

  @Override
  public ResponseEntity<MediatedBatchRequestDto> getMediatedBatchRequestById(UUID batchRequestId) {
    var entity = service.getById(batchRequestId);
    return ResponseEntity.status(OK).body(entity);
  }

  @Override
  public ResponseEntity<MediatedBatchRequestDetailsDto> getMediatedBatchRequestDetailsByBatchId(
    UUID batchRequestId, Integer offset, Integer limit) {
    var collectionDto = requestSplitService.getAllByBatchId(batchRequestId, offset, limit);
    return ResponseEntity.status(OK).body(collectionDto);
  }
}
