package org.folio.mr.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequests;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.domain.mapper.MediatedRequestMapper;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.MediatedRequestDetailsService;
import org.folio.mr.service.MediatedRequestsService;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class MediatedRequestsServiceImpl implements MediatedRequestsService {

  private final MediatedRequestsRepository mediatedRequestsRepository;
  private final MediatedRequestMapper requestsMapper;
  private final MediatedRequestDetailsService requestDetailsService;

  @Override
  public Optional<MediatedRequest> get(UUID id) {
    return mediatedRequestsRepository.findById(id)
      .map(requestsMapper::mapEntityToDto)
      .map(requestDetailsService::fetchRequestDetails);
  }

  @Override
  public MediatedRequests findBy(String query, Integer offset, Integer limit) {
    var mediatedRequests =
      mediatedRequestsRepository.findByCql(query, OffsetRequest.of(offset, limit)).stream()
        .map(requestsMapper::mapEntityToDto)
        .map(requestDetailsService::fetchRequestDetails)
        .toList();

    var totalRecords = mediatedRequestsRepository.count(query);

    return new MediatedRequests().mediatedRequests(mediatedRequests).totalRecords(totalRecords);
  }

  @Override
  public MediatedRequests findAll(Integer offset, Integer limit) {
    var mediatedRequests = mediatedRequestsRepository.findAll(OffsetRequest.of(offset, limit))
      .stream()
      .map(requestsMapper::mapEntityToDto)
      .map(requestDetailsService::fetchRequestDetails)
      .toList();

    var totalRecords = mediatedRequestsRepository.count();

    return new MediatedRequests().mediatedRequests(mediatedRequests).totalRecords(totalRecords);
  }

  @Override
  public MediatedRequest post(MediatedRequest mediatedRequest) {
    var mediatedRequestEntity = requestsMapper.mapDtoToEntity(
      setStatusBasedOnMediatedRequestStatusAndStep(mediatedRequest));

    if (mediatedRequestEntity.getCreatedDate() == null) {
      log.info("post:: New mediated request. Initializing metadata.");
      refreshCreatedDate(mediatedRequestEntity);
      refreshUpdatedDate(mediatedRequestEntity);
    }

    return requestsMapper.mapEntityToDto(mediatedRequestsRepository.save(mediatedRequestEntity));
  }

  @Override
  public Optional<MediatedRequest> update(UUID requestId, MediatedRequest mediatedRequest) {
    return mediatedRequestsRepository.findById(requestId)
      .map(this::refreshUpdatedDate)
      .map(mediatedRequestEntity -> requestsMapper.mapEntityToDto(
        mediatedRequestsRepository.save(requestsMapper.mapDtoToEntity(
          setStatusBasedOnMediatedRequestStatusAndStep(mediatedRequest)))));
  }

  @Override
  public Optional<MediatedRequest> delete(UUID requestId) {
    return mediatedRequestsRepository.findById(requestId)
      .map(mediatedRequestEntity -> {
        log.info("delete:: found mediatedRequestEntity: {}", () -> mediatedRequestEntity);
        mediatedRequestsRepository.delete(mediatedRequestEntity);
        return requestsMapper.mapEntityToDto(mediatedRequestEntity);
      });
   }

  private MediatedRequest setStatusBasedOnMediatedRequestStatusAndStep(
    MediatedRequest mediatedRequest) {

    if (mediatedRequest.getStatus() != null) {
      var statusElements = mediatedRequest.getStatus().toString().split(" - ");
      if (statusElements.length == 2) {
        mediatedRequest.setMediatedRequestStatus(
          MediatedRequest.MediatedRequestStatusEnum.fromValue(statusElements[0]));
        mediatedRequest.setMediatedRequestStep(statusElements[1]);
        return mediatedRequest;
      }
    }

    log.warn("setStatusBasedOnMediatedRequestStatusAndStep:: Invalid status: {}",
      mediatedRequest.getStatus());
    return mediatedRequest;
  }

  private MediatedRequestEntity refreshCreatedDate(MediatedRequestEntity mediatedRequestEntity) {
    mediatedRequestEntity.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
    return mediatedRequestEntity;
  }

  private MediatedRequestEntity refreshUpdatedDate(MediatedRequestEntity mediatedRequestEntity) {
    mediatedRequestEntity.setUpdatedDate(Timestamp.valueOf(LocalDateTime.now()));
    return mediatedRequestEntity;
  }

}
