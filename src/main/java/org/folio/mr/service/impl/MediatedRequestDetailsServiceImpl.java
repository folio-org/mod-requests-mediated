package org.folio.mr.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.folio.mr.client.InstanceClient;
import org.folio.mr.client.ItemClient;
import org.folio.mr.client.LocationClient;
import org.folio.mr.client.LocationUnitClient;
import org.folio.mr.client.ServicePointClient;
import org.folio.mr.client.UserClient;
import org.folio.mr.client.UserGroupClient;
import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.InstanceContributorsInner;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.Library;
import org.folio.mr.domain.dto.Location;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequestInstanceContributorNamesInner;
import org.folio.mr.domain.dto.MediatedRequestInstanceIdentifiersInner;
import org.folio.mr.domain.dto.MediatedRequestInstancePublicationInner;
import org.folio.mr.domain.dto.MediatedRequestItemCallNumberComponents;
import org.folio.mr.domain.dto.MediatedRequestItemLocation;
import org.folio.mr.domain.dto.MediatedRequestPickupServicePoint;
import org.folio.mr.domain.dto.MediatedRequestProxyPatronGroup;
import org.folio.mr.domain.dto.MediatedRequestRequesterPatronGroup;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserGroup;
import org.folio.mr.domain.dto.UserPersonal;
import org.folio.mr.service.MediatedRequestDetailsService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class MediatedRequestDetailsServiceImpl implements MediatedRequestDetailsService {
  private final ItemClient itemClient;
  private final InstanceClient instanceClient;
  private final ServicePointClient servicePointClient;
  private final UserClient userClient;
  private final UserGroupClient userGroupClient;
  private final LocationClient locationClient;
  private final LocationUnitClient locationUnitClient;

  public MediatedRequest fetchRequestDetails(MediatedRequest request) {
    fetchItem(request);
    fetchRequester(request);
    fetchProxyUser(request);
    fetchInstance(request);
    fetchPickupServicePoint(request);

    //    request.getMetadata()
    //      .createdByUserId(UUID.randomUUID().toString())
    //      .updatedByUserId(UUID.randomUUID().toString());

    return request;
  }

  private void fetchItem(MediatedRequest request) {
    final String itemId = request.getItemId();
    if (itemId == null) {
      log.info("fetchItem:: itemId is null, doing nothing");
      return;
    }

    log.info("fetchItem:: fetching item {}", itemId);
    Item item = itemClient.get(itemId);

    request.getItem()
      .barcode(item.getBarcode())
      .enumeration(item.getEnumeration())
      .volume(item.getVolume())
      .chronology(item.getChronology())
      .displaySummary(item.getDisplaySummary())
      .status(item.getStatus().getName().getValue())
      .copyNumber(item.getCopyNumber());

    var effectiveCallNumberComponents = item.getEffectiveCallNumberComponents();
    if (effectiveCallNumberComponents != null) {
      request.getItem()
        .callNumber(effectiveCallNumberComponents.getCallNumber())
        .callNumberComponents(new MediatedRequestItemCallNumberComponents()
          .callNumber(effectiveCallNumberComponents.getCallNumber())
          .prefix(effectiveCallNumberComponents.getPrefix())
          .suffix(effectiveCallNumberComponents.getSuffix()));
    }

    fetchLocation(request, item.getEffectiveLocationId());
  }

  private void fetchLocation(MediatedRequest request, String effectiveLocationId) {
    if (effectiveLocationId == null) {
      log.info("fetchLocation:: effectiveLocationId is null, doing nothing");
      return;
    }

    log.info("fetchLocation:: fetching location {}", effectiveLocationId);
    Location location = locationClient.get(effectiveLocationId);

    request.getItem()
      .location(new MediatedRequestItemLocation()
        .name(location.getName())
        .code(location.getCode()));

    fetchLibrary(request, location.getLibraryId());
  }

  private void fetchLibrary(MediatedRequest request, String libraryId) {
    if (libraryId == null) {
      log.info("fetchLibrary:: libraryId is null, doing nothing");
      return;
    }

    log.info("fetchLibrary:: fetching library {}", libraryId);
    Library library = locationUnitClient.getLibrary(libraryId);
    request.getItem().getLocation().setLibraryName(library.getName());
  }

  private void fetchRequester(MediatedRequest request) {
    final String requesterId = request.getRequesterId();
    if (requesterId == null) {
      log.info("fetchRequester:: requesterId is null, doing nothing");
      return;
    }

    log.info("fetchRequester:: fetching requester {}", requesterId);
    User requester = userClient.get(request.getRequesterId());

    request.getRequester()
      .barcode(requester.getBarcode())
      .patronGroupId(requester.getPatronGroup());

    UserPersonal personal = requester.getPersonal();
    if (personal != null) {
      request.getRequester()
        .firstName(personal.getFirstName())
        .middleName(personal.getMiddleName())
        .lastName(personal.getLastName());
    }

    fetchRequesterUserGroup(request, requester.getPatronGroup());
  }

  private void fetchRequesterUserGroup(MediatedRequest request, String userGroupId) {
    if (userGroupId == null) {
      log.info("fetchRequesterUserGroup:: userGroupId is null, doing nothing");
      return;
    }

    log.info("fetchRequesterUserGroup:: fetching requester user group {}", userGroupId);
    UserGroup userGroup = userGroupClient.get(userGroupId);

    request.getRequester()
      .patronGroup(new MediatedRequestRequesterPatronGroup()
        .id(userGroup.getId())
        .group(userGroup.getGroup())
        .desc(userGroup.getDesc()));
  }

  private void fetchProxyUser(MediatedRequest request) {
    final String proxyUserId = request.getProxyUserId();
    if (proxyUserId == null) {
      log.info("fetchProxyUser:: proxyUserId is null, doing nothing");
      return;
    }

    log.info("fetchProxyUser:: fetching proxy user {}", proxyUserId);
    User proxyUser = userClient.get(proxyUserId);

    request.getProxy()
      .barcode(proxyUser.getBarcode())
      .patronGroupId(proxyUser.getPatronGroup());

    UserPersonal personal = proxyUser.getPersonal();
    if (personal != null) {
      request.getProxy()
        .firstName(personal.getFirstName())
        .middleName(personal.getMiddleName())
        .lastName(personal.getLastName());
    }

    fetchProxyUserGroup(request, proxyUser.getPatronGroup());
  }

  private void fetchProxyUserGroup(MediatedRequest request, String userGroupId) {
    if (userGroupId == null) {
      log.info("fetchProxyUserGroup:: userGroupId is null, doing nothing");
      return;
    }

    log.info("fetchProxyUserGroup:: fetching proxy user group {}", userGroupId);
    UserGroup userGroup = userGroupClient.get(userGroupId);

    request.getProxy()
      .patronGroup(new MediatedRequestProxyPatronGroup()
        .id(userGroup.getId())
        .group(userGroup.getGroup())
        .desc(userGroup.getDesc()));
  }

  private void fetchInstance(MediatedRequest request) {
    final String instanceId = request.getInstanceId();
    if (instanceId == null) {
      log.info("fetchInstance:: instanceId is null, doing nothing");
      return;
    }

    log.info("fetchInstance:: fetching instance {}", instanceId);
    Instance instance = instanceClient.get(request.getInstanceId());

    var contributors = instance.getContributors()
      .stream()
      .map(InstanceContributorsInner::getName)
      .map(new MediatedRequestInstanceContributorNamesInner()::name)
      .toList();

    var publications = instance.getPublication()
      .stream()
      .map(publication -> new MediatedRequestInstancePublicationInner()
        .publisher(publication.getPublisher())
        .place(publication.getPlace())
        .dateOfPublication(publication.getDateOfPublication())
        .role(publication.getRole()))
      .toList();

    var identifiers = instance.getIdentifiers()
      .stream()
      .map(i -> new MediatedRequestInstanceIdentifiersInner()
        .value(i.getValue())
        .identifierTypeId(i.getIdentifierTypeId()))
      .toList();

    request.getInstance()
      .title(instance.getTitle())
      .contributorNames(contributors)
      .publication(publications)
      .identifiers(identifiers)
      .editions(new ArrayList<>(instance.getEditions()));
  }

  private void fetchPickupServicePoint(MediatedRequest request) {
    final String pickupServicePointId = request.getPickupServicePointId();
    if (pickupServicePointId == null) {
      log.info("fetchPickupServicePoint:: pickupServicePointId is null, doing nothing");
      return;
    }

    log.info("fetchPickupServicePoint:: fetching service point {}", pickupServicePointId);
    ServicePoint pickupServicePoint = servicePointClient.get(request.getPickupServicePointId());

    request.pickupServicePoint(new MediatedRequestPickupServicePoint()
      .name(pickupServicePoint.getName())
      .code(pickupServicePoint.getCode())
      .discoveryDisplayName(pickupServicePoint.getDiscoveryDisplayName())
      .description(pickupServicePoint.getDescription())
      .shelvingLagTime(pickupServicePoint.getShelvingLagTime())
      .pickupLocation(pickupServicePoint.getPickupLocation()));
  }
}
