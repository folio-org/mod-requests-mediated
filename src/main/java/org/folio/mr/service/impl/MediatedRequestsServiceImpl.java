package org.folio.mr.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequestInstanceContributorNamesInner;
import org.folio.mr.domain.dto.MediatedRequestInstancePublicationInner;
import org.folio.mr.domain.dto.MediatedRequestItemCallNumberComponents;
import org.folio.mr.domain.dto.MediatedRequestItemLocation;
import org.folio.mr.domain.dto.MediatedRequestPickupServicePoint;
import org.folio.mr.domain.dto.MediatedRequestProxyPatronGroup;
import org.folio.mr.domain.dto.MediatedRequestRequesterPatronGroup;
import org.folio.mr.domain.dto.MediatedRequests;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.domain.mapper.MediatedRequestMapper;
import org.folio.mr.repository.MediatedRequestsRepository;
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

  @Override
  public Optional<MediatedRequest> get(UUID id) {
    return mediatedRequestsRepository.findById(id)
      .map(requestsMapper::mapEntityToDto)
      .map(this::extendMediatedRequestDto);
  }

  @Override
  public MediatedRequests findBy(String query, Integer offset, Integer limit) {
    var mediatedRequests =
      mediatedRequestsRepository.findByCql(query, OffsetRequest.of(offset, limit)).stream()
        .map(requestsMapper::mapEntityToDto)
        .map(this::extendMediatedRequestDto)
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

    var statusElements = mediatedRequest.getStatus().toString().split(" - ");
    if (statusElements.length == 2) {
      mediatedRequest.setMediatedRequestStatus(
        MediatedRequest.MediatedRequestStatusEnum.fromValue(statusElements[0]));
      mediatedRequest.setMediatedRequestStep(statusElements[1]);
      return mediatedRequest;
    } else {
      log.warn("setStatusBasedOnMediatedRequestStatusAndStep:: Invalid status: {}",
        mediatedRequest.getStatus());
      return null;
    }
  }

  private MediatedRequestEntity refreshCreatedDate(MediatedRequestEntity mediatedRequestEntity) {
    mediatedRequestEntity.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
    return mediatedRequestEntity;
  }

  private MediatedRequestEntity refreshUpdatedDate(MediatedRequestEntity mediatedRequestEntity) {
    mediatedRequestEntity.setUpdatedDate(Timestamp.valueOf(LocalDateTime.now()));
    return mediatedRequestEntity;
  }

  private MediatedRequest extendMediatedRequestDto(MediatedRequest mediatedRequest) {
    mediatedRequest.getItem()
      .location(new MediatedRequestItemLocation()
        .name("default-location-name")
        .libraryName("default-library-name")
        .code("default-location-code"))
      .enumeration("default-enumeration")
      .volume("default-volume")
      .chronology("default-chronology")
      .displaySummary("default-display-summary")
      .status("default-status")
      .callNumber("default-call-number")
      .callNumberComponents(new MediatedRequestItemCallNumberComponents()
        .callNumber("default-call-number")
        .prefix("default-call-number-prefix")
        .suffix("default-call-number-suffix"))
      .copyNumber("default-cope-number");

    mediatedRequest.getInstance()
      .contributorNames(List.of(new MediatedRequestInstanceContributorNamesInner()
        .name("default-contributor")))
      .publication(List.of(new MediatedRequestInstancePublicationInner()
        .publisher("default-publisher")
        .place("default-publication-place")
        .dateOfPublication("default-date-of-publication")
        .role("default-publication-role")))
      .editions(List.of("default-edition"));

    mediatedRequest.getRequester()
      .patronGroupId("358837c8-d6fe-4bd5-b82a-ae6e76c93e61")
      .patronGroup(new MediatedRequestRequesterPatronGroup()
        .id("358837c8-d6fe-4bd5-b82a-ae6e76c93e61")
        .group("default-requester-patron-group")
        .desc("default-requester-patron-group-desc"));

    mediatedRequest.getProxy()
      .patronGroupId("e0630f6c-dda3-4494-ae23-5abaa8f1aabd")
      .patronGroup(new MediatedRequestProxyPatronGroup()
        .id("e0630f6c-dda3-4494-ae23-5abaa8f1aabd")
        .group("default-proxy-patron-group")
        .desc("default-proxy-patron-group-desc"));

    mediatedRequest.setPickupServicePoint(new MediatedRequestPickupServicePoint()
      .name("default-pickup-sp-name")
      .code("default-pickup-sp-code")
      .discoveryDisplayName("default-pickup-sp-discovery-display-name")
      .description("default-pickup-sp-description")
      .shelvingLagTime(1)
      .pickupLocation(true));

    mediatedRequest.getMetadata()
      .createdByUserId(UUID.randomUUID().toString())
      .updatedByUserId(UUID.randomUUID().toString());

    return mediatedRequest;
  }
}
