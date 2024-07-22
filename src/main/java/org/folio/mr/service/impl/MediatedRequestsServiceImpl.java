package org.folio.mr.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequests;
import org.folio.mr.domain.mapper.MediatedRequestMapper;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.MediatedRequestsService;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class MediatedRequestsServiceImpl implements MediatedRequestsService {

  private final MediatedRequestsRepository mediatedRequestsRepository;
  private final MediatedRequestMapper requestsMapper;

  @Override
  public Optional<MediatedRequest> get(UUID id) {
    return mediatedRequestsRepository.findById(id)
      .map(requestsMapper::mapEntityToDto);
  }

  @Override
  public MediatedRequests findBy(String query, Integer offset, Integer limit) {
    var mediatedRequests =
      mediatedRequestsRepository.findByCql(query, OffsetRequest.of(offset, limit)).stream()
      .map(requestsMapper::mapEntityToDto)
      .toList();

    var totalRecords = mediatedRequestsRepository.count(query);

    return new MediatedRequests().mediatedRequests(mediatedRequests).totalRecords(totalRecords);
  }

  @Override
  public MediatedRequests findAll(Integer offset, Integer limit) {
    var mediatedRequests = mediatedRequestsRepository.findAll(OffsetRequest.of(offset, limit))
      .stream()
      .map(requestsMapper::mapEntityToDto)
      .toList();

    var totalRecords = mediatedRequestsRepository.count();

    return new MediatedRequests().mediatedRequests(mediatedRequests).totalRecords(totalRecords);
  }

  @Override
  public MediatedRequest post(MediatedRequest mediatedRequest) {
    return requestsMapper.mapEntityToDto(mediatedRequestsRepository.save(
      requestsMapper.mapDtoToEntity(mediatedRequest)));
  }

  @Override
  public Optional<MediatedRequest> update(UUID requestId, MediatedRequest mediatedRequest) {
    return mediatedRequestsRepository.findById(requestId)
      .map(mediatedRequestEntity -> requestsMapper.mapEntityToDto(
        mediatedRequestsRepository.save(requestsMapper.mapDtoToEntity(mediatedRequest))));
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
}
