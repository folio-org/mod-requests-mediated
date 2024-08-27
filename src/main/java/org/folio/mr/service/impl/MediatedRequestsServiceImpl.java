package org.folio.mr.service.impl;

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
import org.springframework.data.domain.Page;
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
      .map(requestDetailsService::addRequestDetailsForGet)
      .map(MediatedRequestsServiceImpl::removeSearchIndex);
  }

  @Override
  public MediatedRequests findBy(String query, Integer offset, Integer limit) {
    return buildResponseForGet(mediatedRequestsRepository.findByCql(query, OffsetRequest.of(offset, limit)));
  }

  @Override
  public MediatedRequests findAll(Integer offset, Integer limit) {
    return buildResponseForGet(mediatedRequestsRepository.findAll(OffsetRequest.of(offset, limit)));
  }

  @Override
  public MediatedRequest post(MediatedRequest mediatedRequest) {
    MediatedRequest extendedRequest = requestDetailsService.addRequestDetailsForCreate(
      mediatedRequest);
    MediatedRequestEntity savedEntity = mediatedRequestsRepository.save(
      requestsMapper.mapDtoToEntity(extendedRequest));
    removeSearchIndex(extendedRequest);

    return extendedRequest.id(savedEntity.getId().toString());
  }

  @Override
  public Optional<MediatedRequest> update(UUID requestId, MediatedRequest mediatedRequest) {
    return mediatedRequestsRepository.findById(requestId)
      .map(ignored -> mediatedRequest)
      .map(requestDetailsService::addRequestDetailsForUpdate)
      .map(requestsMapper::mapDtoToEntity)
      .map(mediatedRequestsRepository::save)
      .map(ignored -> mediatedRequest)
      .map(MediatedRequestsServiceImpl::removeSearchIndex);
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

  private static MediatedRequest removeSearchIndex(MediatedRequest request) {
    log.debug("removeSearchIndex:: removing search index");
    return request.searchIndex(null);
  }

  private MediatedRequests buildResponseForGet(Page<MediatedRequestEntity> entities) {
    var requests = entities.stream()
        .map(requestsMapper::mapEntityToDto)
        .toList();

    requestDetailsService.addRequestDetailsForGet(requests)
      .forEach(MediatedRequestsServiceImpl::removeSearchIndex);

    return new MediatedRequests()
      .mediatedRequests(requests)
      .totalRecords(mediatedRequestsRepository.count());
  }

}
