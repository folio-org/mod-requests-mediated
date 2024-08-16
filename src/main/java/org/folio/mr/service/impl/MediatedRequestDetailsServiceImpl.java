package org.folio.mr.service.impl;

import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.NEW_AWAITING_CONFIRMATION;

import java.util.ArrayList;

import org.folio.mr.client.InstanceClient;
import org.folio.mr.client.ItemClient;
import org.folio.mr.client.LocationClient;
import org.folio.mr.client.LocationUnitClient;
import org.folio.mr.client.ServicePointClient;
import org.folio.mr.client.UserClient;
import org.folio.mr.client.UserGroupClient;
import org.folio.mr.domain.context.MediatedRequestContext;
import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.InstanceContributorsInner;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.ItemEffectiveCallNumberComponents;
import org.folio.mr.domain.dto.Library;
import org.folio.mr.domain.dto.Location;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequestInstance;
import org.folio.mr.domain.dto.MediatedRequestInstanceContributorNamesInner;
import org.folio.mr.domain.dto.MediatedRequestInstanceIdentifiersInner;
import org.folio.mr.domain.dto.MediatedRequestInstancePublicationInner;
import org.folio.mr.domain.dto.MediatedRequestItem;
import org.folio.mr.domain.dto.MediatedRequestItemCallNumberComponents;
import org.folio.mr.domain.dto.MediatedRequestItemLocation;
import org.folio.mr.domain.dto.MediatedRequestPickupServicePoint;
import org.folio.mr.domain.dto.MediatedRequestProxy;
import org.folio.mr.domain.dto.MediatedRequestProxyPatronGroup;
import org.folio.mr.domain.dto.MediatedRequestRequester;
import org.folio.mr.domain.dto.MediatedRequestRequesterPatronGroup;
import org.folio.mr.domain.dto.MediatedRequestSearchIndex;
import org.folio.mr.domain.dto.MediatedRequestSearchIndexCallNumberComponents;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserGroup;
import org.folio.mr.domain.dto.UserPersonal;
import org.folio.mr.service.MediatedRequestDetailsService;
import org.folio.mr.service.MetadataService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class MediatedRequestDetailsServiceImpl implements MediatedRequestDetailsService {

  private static final MediatedRequest.StatusEnum DEFAULT_STATUS = NEW_AWAITING_CONFIRMATION;
  private static final String DEFAULT_WORKFLOW = "Private request";

  private final ItemClient itemClient;
  private final InstanceClient instanceClient;
  private final ServicePointClient servicePointClient;
  private final UserClient userClient;
  private final UserGroupClient userGroupClient;
  private final LocationClient locationClient;
  private final LocationUnitClient locationUnitClient;
  private final MetadataService metadataService;

  @Override
  public MediatedRequest populateRequestDetailsForCreate(MediatedRequest request) {
    removeExistingRequestDetails(request);
    setStatusBasedOnMediatedRequestStatusAndStep(request);
    MediatedRequestContext context = buildRequestContext(request);

    createRequester(context);
    populateRequester(context);
    createRequesterPatronGroup(context);
    createProxy(context);
    populateProxy(context);
    createProxyPatronGroup(context);
    createInstance(context);
    populateInstance(context);
    createItem(context);
    populateItem(context);
    createPickupServicePoint(context);
    createSearchIndex(context);

    metadataService.updateMetadata(request);

    return request;
  }

  @Override
  public MediatedRequest populateRequestDetailsForUpdate(MediatedRequest request) {
    removeExistingRequestDetails(request);
    setStatusBasedOnMediatedRequestStatusAndStep(request);
    MediatedRequestContext context = buildRequestContext(request);

    createRequester(context);
    createRequesterPatronGroup(context);
    createProxy(context);
    createProxyPatronGroup(context);
    createInstance(context);
    createItem(context);
    createSearchIndex(context);

    metadataService.updateMetadata(request);

    return request;
  }

  @Override
  public MediatedRequest populateRequestDetailsForGet(MediatedRequest request) {
    MediatedRequestContext context = buildRequestContext(request);

    populateRequester(context);
    createRequesterPatronGroup(context);
    populateProxy(context);
    createProxyPatronGroup(context);
    populateInstance(context);
    populateItem(context);
    createPickupServicePoint(context);

    removeSearchIndex(request);

    return request;
  }

  private MediatedRequestContext buildRequestContext(MediatedRequest request) {
    MediatedRequestContext context = new MediatedRequestContext(request);

    Instance instance = instanceClient.get(request.getInstanceId());
    context.setInstance(instance);

    User requester = userClient.get(request.getRequesterId());
    context.setRequester(requester);

    UserGroup requesterGroup = userGroupClient.get(requester.getPatronGroup());
    context.setRequesterGroup(requesterGroup);

    if (request.getProxyUserId() != null) {
      User proxy = userClient.get(request.getProxyUserId());
      context.setProxy(proxy);

      UserGroup proxyGroup = userGroupClient.get(proxy.getPatronGroup());
      context.setProxyGroup(proxyGroup);
    }

    if (request.getItemId() != null) {
      Item item = itemClient.get(request.getItemId());
      context.setItem(item);

      Location itemLocation = locationClient.get(item.getEffectiveLocationId());
      context.setLocation(itemLocation);

      Library library = locationUnitClient.getLibrary(itemLocation.getLibraryId());
      context.setLibrary(library);
    }

    if (request.getPickupServicePointId() != null) {
      ServicePoint pickupServicePoint = servicePointClient.get(request.getPickupServicePointId());
      context.setPickupServicePoint(pickupServicePoint);
    }

    return context;
  }

//  public MediatedRequest fetchRequestDetailsForRetrieval(MediatedRequest request) {
//    fetchRequester(request);
////    fetchProxyUser(request);
//    fetchInstance(request);
//    fetchItem(request);
//    fetchPickupServicePoint(request);
//
//    removeSearchIndex(request);
//
//    return request;
//  }

//  public MediatedRequest fetchRequestDetailsForCreation(MediatedRequest request) {
//    removeExistingRequestDetails(request);
//    initObjects(request);
//
//    fetchRequester(request);
////    fetchProxyUser(request);
//    fetchInstance(request);
//    fetchItem(request);
//    fetchPickupServicePoint(request);
//
//    return request;
//  }

  private static void createRequester(MediatedRequestContext context) {
    User requester = context.getRequester();
    MediatedRequestRequester newRequester = new MediatedRequestRequester()
      .barcode(requester.getBarcode());

    UserPersonal personal = requester.getPersonal();
    if (personal != null) {
      newRequester.firstName(personal.getFirstName())
        .middleName(personal.getMiddleName())
        .lastName(personal.getLastName());
    }
    context.getRequest().requester(newRequester);
  }

  private static void populateRequester(MediatedRequestContext context) {
    context.getRequest()
      .getRequester()
      .patronGroupId(context.getRequester().getPatronGroup());
  }

  private static void createRequesterPatronGroup(MediatedRequestContext context) {
    UserGroup userGroup = context.getRequesterGroup();
    context.getRequest()
      .getRequester()
      .patronGroup(new MediatedRequestRequesterPatronGroup()
        .id(userGroup.getId())
        .group(userGroup.getGroup())
        .desc(userGroup.getDesc()));
  }

  private static void createProxy(MediatedRequestContext context) {
    User proxy = context.getProxy();
    if (proxy == null) {
      context.getRequest().proxy(null);
      return;
    }

    MediatedRequestProxy newProxy = new MediatedRequestProxy()
      .barcode(proxy.getBarcode());

    UserPersonal personal = proxy.getPersonal();
    if (personal != null) {
      newProxy.firstName(personal.getFirstName())
        .middleName(personal.getMiddleName())
        .lastName(personal.getLastName());
    }
    context.getRequest().proxy(newProxy);
  }

  private static void populateProxy(MediatedRequestContext context) {
    if (context.getProxy() == null) {
      return;
    }

    context.getRequest()
      .getProxy()
      .patronGroupId(context.getProxy().getPatronGroup());
  }

  private static void createProxyPatronGroup(MediatedRequestContext context) {
    if (context.getProxyGroup() == null) {
      return;
    }

    UserGroup userGroup = context.getProxyGroup();
    context.getRequest()
      .getProxy()
      .patronGroup(new MediatedRequestProxyPatronGroup()
        .id(userGroup.getId())
        .group(userGroup.getGroup())
        .desc(userGroup.getDesc()));
  }

  private static void createInstance(MediatedRequestContext context) {
    Instance instance = context.getInstance();
    var identifiers = instance.getIdentifiers()
      .stream()
      .map(i -> new MediatedRequestInstanceIdentifiersInner()
        .value(i.getValue())
        .identifierTypeId(i.getIdentifierTypeId()))
      .toList();

    MediatedRequestInstance newInstance = new MediatedRequestInstance()
      .title(instance.getTitle())
      .identifiers(identifiers);

    context.getRequest().instance(newInstance);
  }

  private static void populateInstance(MediatedRequestContext context) {
    Instance instance = context.getInstance();

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

    context.getRequest()
      .getInstance()
      .contributorNames(contributors)
      .publication(publications)
      .editions(new ArrayList<>(instance.getEditions()));
  }

  private static void createItem(MediatedRequestContext context) {
    if (context.getItem() == null) {
      context.getRequest().item(null);
      return;
    }
    context.getRequest()
      .item(new MediatedRequestItem().barcode(context.getItem().getBarcode()));
  }

  private static void populateItem(MediatedRequestContext context) {
    Item item = context.getItem();
    if (item == null) {
      return;
    }

    context.getRequest().getItem()
      .enumeration(item.getEnumeration())
      .volume(item.getVolume())
      .chronology(item.getChronology())
      .displaySummary(item.getDisplaySummary())
      .status(item.getStatus().getName().getValue())
      .copyNumber(item.getCopyNumber())
      .location(new MediatedRequestItemLocation()
        .name(context.getLocation().getName())
        .code(context.getLocation().getCode())
        .libraryName(context.getLibrary().getName()));

    var effectiveCallNumberComponents = item.getEffectiveCallNumberComponents();
    if (effectiveCallNumberComponents != null) {
      context.getRequest().getItem()
        .callNumber(effectiveCallNumberComponents.getCallNumber())
        .callNumberComponents(new MediatedRequestItemCallNumberComponents()
          .callNumber(effectiveCallNumberComponents.getCallNumber())
          .prefix(effectiveCallNumberComponents.getPrefix())
          .suffix(effectiveCallNumberComponents.getSuffix()));
    }
  }

  private static void createPickupServicePoint(MediatedRequestContext context) {
    ServicePoint pickupServicePoint = context.getPickupServicePoint();
    if (pickupServicePoint == null) {
      context.getRequest().pickupServicePoint(null);
      return;
    }
    context.getRequest().pickupServicePoint(new MediatedRequestPickupServicePoint()
      .name(pickupServicePoint.getName())
      .code(pickupServicePoint.getCode())
      .discoveryDisplayName(pickupServicePoint.getDiscoveryDisplayName())
      .description(pickupServicePoint.getDescription())
      .shelvingLagTime(pickupServicePoint.getShelvingLagTime())
      .pickupLocation(pickupServicePoint.getPickupLocation()));
  }

  private static void createSearchIndex(MediatedRequestContext context) {
    MediatedRequestSearchIndex searchIndex = new MediatedRequestSearchIndex();

    Item item = context.getItem();
    if (item != null) {
      String shelvingOrder = item.getEffectiveShelvingOrder();
      ItemEffectiveCallNumberComponents callNumberComponents = item.getEffectiveCallNumberComponents();
      if (shelvingOrder != null) {
        searchIndex.setShelvingOrder(shelvingOrder);
      }
      if (callNumberComponents != null) {
        searchIndex.callNumberComponents(new MediatedRequestSearchIndexCallNumberComponents()
          .callNumber(callNumberComponents.getCallNumber())
          .prefix(callNumberComponents.getPrefix())
          .suffix(callNumberComponents.getSuffix()));
      }
    }

    ServicePoint pickupServicePoint = context.getPickupServicePoint();
    if (pickupServicePoint != null && pickupServicePoint.getName() != null) {
      searchIndex.setPickupServicePointName(pickupServicePoint.getName());
    }

    context.getRequest().searchIndex(searchIndex);
  }


//  private void fetchRequester(MediatedRequest request) {
//    final String requesterId = request.getRequesterId();
//    if (requesterId == null) {
//      log.info("fetchRequester:: requesterId is null, doing nothing");
//      return;
//    }
//
//    log.info("fetchRequester:: fetching requester {}", requesterId);
//    User fetchedRequester = userClient.get(requesterId);
//    if (request.getRequester() == null) {
//      initRequester(request, fetchedRequester);
//    }
//    request.getRequester().patronGroupId(fetchedRequester.getPatronGroup());
//    fetchRequesterUserGroup(request);
//  }
//
//  private void initRequester(MediatedRequest request, User requester) {
//    MediatedRequestRequester newRequester = new MediatedRequestRequester()
//      .barcode(requester.getBarcode());
//
//    UserPersonal personal = requester.getPersonal();
//    if (personal != null) {
//      newRequester
//        .firstName(personal.getFirstName())
//        .middleName(personal.getMiddleName())
//        .lastName(personal.getLastName());
//    }
//    request.requester(newRequester);
//  }
//
//  private void fetchRequesterUserGroup(MediatedRequest request) {
//    final MediatedRequestRequester requester = request.getRequester();
////    if (requester == null) {
////      log.info("fetchRequesterUserGroup:: requester is null, doing nothing");
////      return;
////    }
//    final String userGroupId = requester.getPatronGroupId();
//    if (userGroupId == null) {
//      log.info("fetchRequesterUserGroup:: userGroupId is null, doing nothing");
//      return;
//    }
//
//    log.info("fetchRequesterUserGroup:: fetching requester user group {}", userGroupId);
//    UserGroup userGroup = userGroupClient.get(userGroupId);
//
//    requester.patronGroup(new MediatedRequestRequesterPatronGroup()
//      .id(userGroup.getId())
//      .group(userGroup.getGroup())
//      .desc(userGroup.getDesc()));
//  }
//
////  private void fetchProxyUser(MediatedRequest request) {
////    final String proxyUserId = request.getProxyUserId();
////    if (proxyUserId == null) {
////      log.info("fetchProxyUser:: proxyUserId is null, doing nothing");
////      return;
////    }
////
////    log.info("fetchProxyUser:: fetching proxy user {}", proxyUserId);
////    User fetchedProxyUser = userClient.get(proxyUserId);
////
////    if (request.getProxy() == null) {
////      MediatedRequestProxy newProxyUser = new MediatedRequestProxy()
////        .barcode(fetchedProxyUser.getBarcode());
////
////      UserPersonal personal = fetchedProxyUser.getPersonal();
////      if (personal != null) {
////        newProxyUser
////          .firstName(personal.getFirstName())
////          .middleName(personal.getMiddleName())
////          .lastName(personal.getLastName());
////      }
////      request.proxy(newProxyUser);
////    }
////
////    request.getProxy().patronGroupId(fetchedProxyUser.getPatronGroup());
////    fetchProxyUserGroup(request);
////  }
////
////  private void initProxyUser(MediatedRequest request, User proxyUser) {
////    MediatedRequestRequester newRequester = new MediatedRequestRequester()
////      .barcode(proxyUser.getBarcode());
////
////    UserPersonal personal = proxyUser.getPersonal();
////    if (personal != null) {
////      newRequester
////        .firstName(personal.getFirstName())
////        .middleName(personal.getMiddleName())
////        .lastName(personal.getLastName());
////    }
////    request.requester(newRequester);
////  }
////
////  private void fetchProxyUserGroup(MediatedRequest request) {
////    final MediatedRequestProxy proxy = request.getProxy();
//////    if (proxy == null) {
//////      log.info("fetchProxyUserGroup:: proxy user is null, doing nothing");
//////      return;
//////    }
////    final String userGroupId = proxy.getPatronGroupId();
////    if (userGroupId == null) {
////      log.info("fetchProxyUserGroup:: userGroupId is null, doing nothing");
////      return;
////    }
////
////    log.info("fetchProxyUserGroup:: fetching proxy user group {}", userGroupId);
////    UserGroup userGroup = userGroupClient.get(userGroupId);
////
////    proxy.patronGroup(new MediatedRequestProxyPatronGroup()
////      .id(userGroup.getId())
////      .group(userGroup.getGroup())
////      .desc(userGroup.getDesc()));
////  }
//
//  private void fetchInstance(MediatedRequest request) {
//    final String instanceId = request.getInstanceId();
//    if (instanceId == null) {
//      log.info("fetchInstance:: instanceId is null, doing nothing");
//      return;
//    }
//
//    log.info("fetchInstance:: fetching instance {}", instanceId);
//    Instance fetchedInstance = instanceClient.get(request.getInstanceId());
//
//    if (request.getInstance() == null) {
//      initInstance(request, fetchedInstance);
//    }
//
//    var contributors = fetchedInstance.getContributors()
//      .stream()
//      .map(InstanceContributorsInner::getName)
//      .map(new MediatedRequestInstanceContributorNamesInner()::name)
//      .toList();
//
//    var publications = fetchedInstance.getPublication()
//      .stream()
//      .map(publication -> new MediatedRequestInstancePublicationInner()
//        .publisher(publication.getPublisher())
//        .place(publication.getPlace())
//        .dateOfPublication(publication.getDateOfPublication())
//        .role(publication.getRole()))
//      .toList();
//
//    request.getInstance()
//      .contributorNames(contributors)
//      .publication(publications)
//      .editions(new ArrayList<>(fetchedInstance.getEditions()));
//  }
//
//  private void initInstance(MediatedRequest request, Instance instance) {
//    var identifiers = instance.getIdentifiers()
//      .stream()
//      .map(i -> new MediatedRequestInstanceIdentifiersInner()
//        .value(i.getValue())
//        .identifierTypeId(i.getIdentifierTypeId()))
//      .toList();
//
//    MediatedRequestInstance newInstance = new MediatedRequestInstance()
//      .title(instance.getTitle())
//      .identifiers(identifiers);
//
//    request.instance(newInstance);
//  }
//
//  private void fetchItem(MediatedRequest request) {
//    final String itemId = request.getItemId();
//    if (itemId == null) {
//      log.info("fetchItem:: itemId is null, doing nothing");
//      return;
//    }
//
//    log.info("fetchItem:: fetching item {}", itemId);
//    Item fetchedItem = itemClient.get(itemId);
//
//    if (request.getItem() == null) {
//      initItem(request, fetchedItem);
//    }
//
//    request.getItem()
//      .enumeration(fetchedItem.getEnumeration())
//      .volume(fetchedItem.getVolume())
//      .chronology(fetchedItem.getChronology())
//      .displaySummary(fetchedItem.getDisplaySummary())
//      .status(fetchedItem.getStatus().getName().getValue())
//      .copyNumber(fetchedItem.getCopyNumber());
//
//    request.getSearchIndex()
//      .shelvingOrder(fetchedItem.getEffectiveShelvingOrder());
//
//    var effectiveCallNumberComponents = fetchedItem.getEffectiveCallNumberComponents();
//    if (effectiveCallNumberComponents != null) {
//      request.getItem()
//        .callNumber(effectiveCallNumberComponents.getCallNumber())
//        .callNumberComponents(new MediatedRequestItemCallNumberComponents()
//          .callNumber(effectiveCallNumberComponents.getCallNumber())
//          .prefix(effectiveCallNumberComponents.getPrefix())
//          .suffix(effectiveCallNumberComponents.getSuffix()));
//
//      request.getSearchIndex()
//        .callNumberComponents(new MediatedRequestSearchIndexCallNumberComponents()
//          .callNumber(effectiveCallNumberComponents.getCallNumber())
//          .prefix(effectiveCallNumberComponents.getPrefix())
//          .suffix(effectiveCallNumberComponents.getSuffix()));
//    }
//
//    fetchLocation(request, fetchedItem.getEffectiveLocationId());
//  }
//
//  private void initItem(MediatedRequest request, Item item) {
//    request.item(new MediatedRequestItem().barcode(item.getBarcode()));
//  }
//
//  private void fetchLocation(MediatedRequest request, String locationId) {
//    if (locationId == null) {
//      log.info("fetchLocation:: effectiveLocationId is null, doing nothing");
//      return;
//    }
//
//    log.info("fetchLocation:: fetching location {}", locationId);
//    Location location = locationClient.get(locationId);
//
//    request.getItem()
//      .location(new MediatedRequestItemLocation()
//        .name(location.getName())
//        .code(location.getCode()));
//
//    fetchLibrary(request, location.getLibraryId());
//  }
//
//  private void fetchLibrary(MediatedRequest request, String libraryId) {
//    if (libraryId == null) {
//      log.info("fetchLibrary:: libraryId is null, doing nothing");
//      return;
//    }
//
//    log.info("fetchLibrary:: fetching library {}", libraryId);
//    Library library = locationUnitClient.getLibrary(libraryId);
//    request.getItem().getLocation().setLibraryName(library.getName());
//  }
//
//  private void fetchPickupServicePoint(MediatedRequest request) {
//    final String pickupServicePointId = request.getPickupServicePointId();
//    if (pickupServicePointId == null) {
//      log.info("fetchPickupServicePoint:: pickupServicePointId is null, doing nothing");
//      return;
//    }
//
//    log.info("fetchPickupServicePoint:: fetching service point {}", pickupServicePointId);
//    ServicePoint pickupServicePoint = servicePointClient.get(request.getPickupServicePointId());
//
//    request.getPickupServicePoint()
//      .name(pickupServicePoint.getName())
//      .code(pickupServicePoint.getCode())
//      .discoveryDisplayName(pickupServicePoint.getDiscoveryDisplayName())
//      .description(pickupServicePoint.getDescription())
//      .shelvingLagTime(pickupServicePoint.getShelvingLagTime())
//      .pickupLocation(pickupServicePoint.getPickupLocation());
//
//    request.getSearchIndex()
//      .pickupServicePointName(pickupServicePoint.getName());
//  }

  private static void removeExistingRequestDetails(MediatedRequest request) {
    request.item(null)
      .requester(null)
      .proxy(null)
      .instance(null)
      .pickupServicePoint(null)
      .searchIndex(null);
  }

  private static void removeSearchIndex(MediatedRequest request) {
    request.searchIndex(null);
  }

  private static void setStatusBasedOnMediatedRequestStatusAndStep(
    MediatedRequest mediatedRequest) {

    if (mediatedRequest.getStatus() == null) {
      mediatedRequest.setStatus(DEFAULT_STATUS);
    }
    if (mediatedRequest.getMediatedWorkflow() == null) {
      mediatedRequest.mediatedWorkflow(DEFAULT_WORKFLOW);
    }

    var statusElements = mediatedRequest.getStatus().toString().split(" - ");
    if (statusElements.length == 2) {
      mediatedRequest.setMediatedRequestStatus(
        MediatedRequest.MediatedRequestStatusEnum.fromValue(statusElements[0]));
      mediatedRequest.setMediatedRequestStep(statusElements[1]);
      return;
    }

    log.warn("setStatusBasedOnMediatedRequestStatusAndStep:: Invalid status: {}",
      mediatedRequest.getStatus());
  }

}
