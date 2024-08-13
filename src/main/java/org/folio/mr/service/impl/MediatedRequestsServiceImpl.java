package org.folio.mr.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.folio.mr.client.HoldingsRecordClient;
import org.folio.mr.client.InstanceClient;
import org.folio.mr.client.ItemClient;
import org.folio.mr.client.LocationClient;
import org.folio.mr.client.LocationUnitClient;
import org.folio.mr.client.ServicePointClient;
import org.folio.mr.client.UserClient;
import org.folio.mr.client.UserGroupClient;
import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.Library;
import org.folio.mr.domain.dto.Location;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequestInstanceContributorNamesInner;
import org.folio.mr.domain.dto.MediatedRequestInstancePublicationInner;
import org.folio.mr.domain.dto.MediatedRequestItemCallNumberComponents;
import org.folio.mr.domain.dto.MediatedRequestItemLocation;
import org.folio.mr.domain.dto.MediatedRequestPickupServicePoint;
import org.folio.mr.domain.dto.MediatedRequestProxyPatronGroup;
import org.folio.mr.domain.dto.MediatedRequestRequesterPatronGroup;
import org.folio.mr.domain.dto.MediatedRequests;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserGroup;
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
  private final ItemClient itemClient;
  private final InstanceClient instanceClient;
  private final ServicePointClient servicePointClient;
  private final UserClient userClient;
  private final UserGroupClient userGroupClient;
  private final LocationClient locationClient;
  private final LocationUnitClient locationUnitClient;

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
      .map(this::extendMediatedRequestDto)
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

  private MediatedRequest extendMediatedRequestDto(MediatedRequest request) {
    log.info("extendMediatedRequestDto:: fetching item");
    Item item = itemClient.get(request.getItemId());
    log.info("extendMediatedRequestDto:: fetching instance");
    Instance instance = instanceClient.get(request.getInstanceId());
    log.info("extendMediatedRequestDto:: fetching requester");
    User requester = userClient.get(request.getRequesterId());
    log.info("extendMediatedRequestDto:: fetching service point");
    ServicePoint pickupServicePoint = servicePointClient.get(request.getPickupServicePointId());
    log.info("extendMediatedRequestDto:: fetching patron group");
    UserGroup patronGroup = userGroupClient.get(requester.getPatronGroup());
    log.info("extendMediatedRequestDto:: fetching location");
    Location location = locationClient.get(item.getEffectiveLocationId());
    log.info("extendMediatedRequestDto:: fetching library");
    Library library = locationUnitClient.getLibrary(location.getLibraryId());

    if (request.getProxyUserId() != null) {
      log.info("extendMediatedRequestDto:: fetching proxy user");
      User proxy = userClient.get(request.getProxyUserId());
      log.info("extendMediatedRequestDto:: fetching proxy user patron group");
      UserGroup proxyGroup = userGroupClient.get(proxy.getPatronGroup());

      request.getProxy()
        .barcode(proxy.getBarcode())
        .firstName(proxy.getPersonal().getFirstName())
        .middleName(proxy.getPersonal().getMiddleName())
        .lastName(proxy.getPersonal().getLastName())
        .patronGroupId(proxy.getPatronGroup())
        .patronGroup(new MediatedRequestProxyPatronGroup()
          .id(proxyGroup.getId())
          .group(proxyGroup.getGroup())
          .desc(proxyGroup.getDesc()));
    }

    List<MediatedRequestInstanceContributorNamesInner> contributors = instance.getContributors()
      .stream()
      .map(c -> new MediatedRequestInstanceContributorNamesInner().name(c.getName()))
      .toList();

    List<MediatedRequestInstancePublicationInner> publications = instance.getPublication()
      .stream()
      .map(publication -> new MediatedRequestInstancePublicationInner()
        .publisher(publication.getPublisher())
        .place(publication.getPlace())
        .dateOfPublication(publication.getDateOfPublication())
        .role(publication.getRole()))
      .toList();

    request.getItem()
      .location(new MediatedRequestItemLocation()
        .name(location.getName())
        .libraryName(library.getName())
        .code(location.getCode()))
      .enumeration(item.getEnumeration())
      .volume(item.getVolume())
      .chronology(item.getChronology())
      .displaySummary(item.getDisplaySummary())
      .status(item.getStatus().getName().getValue())
      .callNumber(item.getEffectiveCallNumberComponents().getCallNumber())
      .callNumberComponents(new MediatedRequestItemCallNumberComponents()
        .callNumber(item.getEffectiveCallNumberComponents().getCallNumber())
        .prefix(item.getEffectiveCallNumberComponents().getPrefix())
        .suffix(item.getEffectiveCallNumberComponents().getSuffix()))
      .copyNumber(item.getCopyNumber());

    request.getInstance()
      .contributorNames(contributors)
      .publication(publications)
      .editions(new ArrayList<>(instance.getEditions()));

    request.getRequester()
      .barcode(requester.getBarcode())
      .firstName(requester.getPersonal().getFirstName())
      .middleName(requester.getPersonal().getMiddleName())
      .lastName(requester.getPersonal().getLastName())
      .patronGroupId(requester.getPatronGroup())
      .patronGroup(new MediatedRequestRequesterPatronGroup()
        .id(patronGroup.getId())
        .group(patronGroup.getGroup())
        .desc(patronGroup.getDesc()));

    request.setPickupServicePoint(new MediatedRequestPickupServicePoint()
      .name(pickupServicePoint.getName())
      .code(pickupServicePoint.getCode())
      .discoveryDisplayName(pickupServicePoint.getDiscoveryDisplayName())
      .description(pickupServicePoint.getDescription())
      .shelvingLagTime(pickupServicePoint.getShelvingLagTime())
      .pickupLocation(pickupServicePoint.getPickupLocation()));

    request.getMetadata()
      .createdByUserId(UUID.randomUUID().toString())
      .updatedByUserId(UUID.randomUUID().toString());

    return request;
  }
}
