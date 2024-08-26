package org.folio.mr.service.impl;

import static java.util.stream.Collectors.toSet;
import static org.folio.mr.domain.dto.MediatedRequest.FulfillmentPreferenceEnum.DELIVERY;
import static org.folio.mr.domain.dto.MediatedRequest.FulfillmentPreferenceEnum.HOLD_SHELF;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.NEW_AWAITING_CONFIRMATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.InstanceContributorsInner;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.ItemEffectiveCallNumberComponents;
import org.folio.mr.domain.dto.Library;
import org.folio.mr.domain.dto.Location;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequestDeliveryAddress;
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
import org.folio.mr.service.InventoryService;
import org.folio.mr.service.MediatedRequestDetailsService;
import org.folio.mr.service.MetadataService;
import org.folio.mr.service.UserService;
import org.springframework.stereotype.Service;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class MediatedRequestDetailsServiceImpl implements MediatedRequestDetailsService {

  private static final MediatedRequest.StatusEnum DEFAULT_STATUS = NEW_AWAITING_CONFIRMATION;
  private static final String DEFAULT_WORKFLOW = "Private request";

  private final InventoryService inventoryService;
  private final UserService userService;
  private final MetadataService metadataService;

  @Override
  public MediatedRequest addRequestDetailsForCreate(MediatedRequest request) {
    removeExistingRequestDetails(request);
    processRequestStatus(request);
    MediatedRequestContext context = buildRequestContext(request);

    addRequester(context);
    extendRequester(context);
    addRequesterUserGroup(context);
    addProxyUser(context);
    extendProxyUser(context);
    addProxyUserGroup(context);
    addInstance(context);
    extendInstance(context);
    addItem(context);
    extendItem(context);
    addFulfillmentDetails(context);
    addSearchIndex(context);

    metadataService.updateMetadata(request);

    return request;
  }

  @Override
  public MediatedRequest addRequestDetailsForUpdate(MediatedRequest request) {
    removeExistingRequestDetails(request);
    processRequestStatus(request);
    MediatedRequestContext context = buildRequestContext(request);

    addRequester(context);
    addRequesterUserGroup(context);
    addProxyUser(context);
    addProxyUserGroup(context);
    addInstance(context);
    addItem(context);
    addSearchIndex(context);

    metadataService.updateMetadata(request);

    return request;
  }

  @Override
  public Collection<MediatedRequest> addRequestDetailsForGet(Collection<MediatedRequest> requests) {
    buildRequestContexts(requests)
      .forEach(this::addRequestDetailsForGet);

    return requests;
  }

  @Override
  public MediatedRequest addRequestDetailsForGet(MediatedRequest request) {
    return addRequestDetailsForGet(buildRequestContext(request));
  }

  private MediatedRequest addRequestDetailsForGet(MediatedRequestContext context) {
    extendRequester(context);
    addRequesterUserGroup(context);
    extendProxyUser(context);
    addProxyUserGroup(context);
    extendInstance(context);
    extendItem(context);
    addFulfillmentDetails(context);

    return context.request();
  }

  private Collection<MediatedRequestContext> buildRequestContexts(Collection<MediatedRequest> requests) {
    Set<String> instanceIds = new HashSet<>();
    Set<String> itemIds = new HashSet<>();
    Set<String> servicePointIds = new HashSet<>();
    Set<String> userIds = new HashSet<>();

    requests.forEach(request -> {
      addIfNotNull(instanceIds, request.getInstanceId());
      addIfNotNull(itemIds, request.getItemId());
      addIfNotNull(servicePointIds, request.getPickupServicePointId());
      addIfNotNull(userIds, request.getRequesterId());
      addIfNotNull(userIds, request.getProxyUserId());
    });

    Map<String, Instance> instances = inventoryService.fetchInstances(instanceIds);
    Map<String, Item> items = inventoryService.fetchItems(itemIds);
    Map<String, ServicePoint> servicePoints = inventoryService.fetchServicePoints(servicePointIds);
    Map<String, User> users = userService.fetchUsers(userIds);
    Map<String, UserGroup> userGroups = userService.fetchUserGroups(
      extractValues(users, User::getPatronGroup));
    Map<String, Location> locations = inventoryService.fetchLocations(
      extractValues(items, Item::getEffectiveLocationId));
    Map<String, Library> libraries = inventoryService.fetchLibraries(
      extractValues(locations, Location::getLibraryId));

    return requests.stream()
      .map(request -> {
        Instance instance = instances.get(request.getInstanceId());
        Item item = items.get(request.getItemId());
        ServicePoint servicePoint = servicePoints.get(request.getPickupServicePointId());
        User requester = users.get(request.getRequesterId());
        UserGroup requesterUserGroup = userGroups.get(requester.getPatronGroup());
        User proxyUser = users.get(request.getProxyUserId());
        Location location = Optional.ofNullable(item)
          .map(Item::getEffectiveLocationId)
          .map(locations::get)
          .orElse(null);
        Library library = Optional.ofNullable(location)
          .map(Location::getLibraryId)
          .map(libraries::get)
          .orElse(null);
        UserGroup proxyUserGroup = Optional.ofNullable(proxyUser)
          .map(User::getPatronGroup)
          .map(userGroups::get)
          .orElse(null);

        return MediatedRequestContext.builder()
          .request(request)
          .instance(instance)
          .item(item)
          .location(location)
          .library(library)
          .pickupServicePoint(servicePoint)
          .requester(requester)
          .requesterUserGroup(requesterUserGroup)
          .proxyUser(proxyUser)
          .proxyUserGroup(proxyUserGroup)
          .build();
      }).toList();
  }

  private MediatedRequestContext buildRequestContext(MediatedRequest request) {
    log.info("buildRequestContext:: building request context");
    var contextBuilder = MediatedRequestContext.builder().request(request);
    Instance instance = inventoryService.fetchInstance(request.getInstanceId());
    User requester = userService.fetchUser(request.getRequesterId());
    UserGroup requesterGroup = userService.fetchUserGroup(requester.getPatronGroup());

    contextBuilder.instance(instance)
      .requester(requester)
      .requesterUserGroup(requesterGroup);

    if (request.getProxyUserId() != null) {
      User proxy = userService.fetchUser(request.getProxyUserId());
      UserGroup proxyGroup = userService.fetchUserGroup(proxy.getPatronGroup());
      contextBuilder.proxyUser(proxy)
        .proxyUserGroup(proxyGroup);
    }

    if (request.getItemId() != null) {
      Item item = inventoryService.fetchItem(request.getItemId());
      Location location = inventoryService.fetchLocation(item.getEffectiveLocationId());
      Library library = inventoryService.fetchLibrary(location.getLibraryId());
      contextBuilder.item(item)
        .location(location)
        .library(library);
    }

    if (request.getPickupServicePointId() != null) {
      ServicePoint servicePoint = inventoryService.fetchServicePoint(request.getPickupServicePointId());
      contextBuilder.pickupServicePoint(servicePoint);
    }
    log.info("buildRequestContext:: request context is built");

    return contextBuilder.build();
  }

  private static void addRequester(MediatedRequestContext context) {
    log.info("addRequester:: adding requester data");
    User requester = context.requester();
    MediatedRequestRequester newRequester = new MediatedRequestRequester()
      .barcode(requester.getBarcode());

    UserPersonal personal = requester.getPersonal();
    if (personal != null) {
      newRequester.firstName(personal.getFirstName())
        .middleName(personal.getMiddleName())
        .lastName(personal.getLastName());
    }
    context.request().requester(newRequester);
  }

  private static void extendRequester(MediatedRequestContext context) {
    log.info("extendRequester:: extending requester data");
    context.request()
      .getRequester()
      .patronGroupId(context.requester().getPatronGroup());
  }

  private static void addRequesterUserGroup(MediatedRequestContext context) {
    log.info("addRequesterUserGroup:: adding requester user group data");
    UserGroup userGroup = context.requesterUserGroup();
    context.request()
      .getRequester()
      .patronGroup(new MediatedRequestRequesterPatronGroup()
        .id(userGroup.getId())
        .group(userGroup.getGroup())
        .desc(userGroup.getDesc()));
  }

  private static void addProxyUser(MediatedRequestContext context) {
    log.info("addProxyUser:: adding proxyUser user data");
    User proxy = context.proxyUser();
    if (proxy == null) {
      log.info("addProxyUser:: proxyUser user is null");
      context.request().proxy(null);
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
    context.request().proxy(newProxy);
  }

  private static void extendProxyUser(MediatedRequestContext context) {
    log.info("extendProxyUser:: extending proxyUser user data");
    if (context.proxyUser() == null) {
      log.info("extendProxyUser:: proxyUser user is null");
      context.request().proxy(null);
      return;
    }

    context.request()
      .getProxy()
      .patronGroupId(context.proxyUser().getPatronGroup());
  }

  private static void addProxyUserGroup(MediatedRequestContext context) {
    log.info("addProxyUserGroup:: adding proxyUser user group data");
    if (context.proxyUserGroup() == null) {
      log.info("addProxyUserGroup:: proxyUser user group is null");
      return;
    }

    UserGroup userGroup = context.proxyUserGroup();
    context.request()
      .getProxy()
      .patronGroup(new MediatedRequestProxyPatronGroup()
        .id(userGroup.getId())
        .group(userGroup.getGroup())
        .desc(userGroup.getDesc()));
  }

  private static void addInstance(MediatedRequestContext context) {
    log.info("addInstance:: adding instance data");
    Instance instance = context.instance();
    var identifiers = instance.getIdentifiers()
      .stream()
      .map(i -> new MediatedRequestInstanceIdentifiersInner()
        .value(i.getValue())
        .identifierTypeId(i.getIdentifierTypeId()))
      .toList();

    MediatedRequestInstance newInstance = new MediatedRequestInstance()
      .title(instance.getTitle())
      .identifiers(identifiers);

    context.request().instance(newInstance);
  }

  private static void extendInstance(MediatedRequestContext context) {
    log.info("extendInstance:: extending instance data");
    Instance instance = context.instance();

    var contributors = instance.getContributors()
      .stream()
      .map(InstanceContributorsInner::getName)
      .map(name -> new MediatedRequestInstanceContributorNamesInner().name(name))
      .toList();

    var publications = instance.getPublication()
      .stream()
      .map(publication -> new MediatedRequestInstancePublicationInner()
        .publisher(publication.getPublisher())
        .place(publication.getPlace())
        .dateOfPublication(publication.getDateOfPublication())
        .role(publication.getRole()))
      .toList();

    context.request()
      .getInstance()
      .contributorNames(contributors)
      .publication(publications)
      .editions(new ArrayList<>(instance.getEditions()));
  }

  private static void addItem(MediatedRequestContext context) {
    log.info("addItem:: adding item data");
    if (context.item() == null) {
      log.info("addItem:: item is null");
      context.request().item(null);
      return;
    }
    context.request()
      .item(new MediatedRequestItem().barcode(context.item().getBarcode()));
  }

  private static void extendItem(MediatedRequestContext context) {
    log.info("extendItem:: extending item data");
    Item item = context.item();
    if (item == null) {
      log.info("extendItem:: item is null");
      context.request().item(null);
      return;
    }

    context.request().getItem()
      .enumeration(item.getEnumeration())
      .volume(item.getVolume())
      .chronology(item.getChronology())
      .displaySummary(item.getDisplaySummary())
      .status(item.getStatus().getName().getValue())
      .copyNumber(item.getCopyNumber())
      .location(new MediatedRequestItemLocation()
        .name(context.location().getName())
        .code(context.location().getCode())
        .libraryName(context.library().getName()));

    var effectiveCallNumberComponents = item.getEffectiveCallNumberComponents();
    if (effectiveCallNumberComponents != null) {
      context.request().getItem()
        .callNumber(effectiveCallNumberComponents.getCallNumber())
        .callNumberComponents(new MediatedRequestItemCallNumberComponents()
          .callNumber(effectiveCallNumberComponents.getCallNumber())
          .prefix(effectiveCallNumberComponents.getPrefix())
          .suffix(effectiveCallNumberComponents.getSuffix()));
    }
  }

  private static void addFulfillmentDetails(MediatedRequestContext context) {
    var fulfillmentPreference = context.request().getFulfillmentPreference();
    log.info("addFulfillmentDetails:: fulfillment preference is '{}'", fulfillmentPreference.getValue());

    if (fulfillmentPreference == DELIVERY) {
      addDeliveryAddress(context);
    } else if (fulfillmentPreference == HOLD_SHELF) {
      addPickupServicePoint(context);
    }
  }

  private static void addPickupServicePoint(MediatedRequestContext context) {
    log.info("addPickupServicePoint:: adding pickup service point data");
    ServicePoint pickupServicePoint = context.pickupServicePoint();
    if (pickupServicePoint == null) {
      log.info("addPickupServicePoint:: pickup service point is null");
      context.request().pickupServicePoint(null);
      return;
    }

    context.request().pickupServicePoint(new MediatedRequestPickupServicePoint()
      .name(pickupServicePoint.getName())
      .code(pickupServicePoint.getCode())
      .discoveryDisplayName(pickupServicePoint.getDiscoveryDisplayName())
      .description(pickupServicePoint.getDescription())
      .shelvingLagTime(pickupServicePoint.getShelvingLagTime())
      .pickupLocation(pickupServicePoint.getPickupLocation()));
  }

  private static void addDeliveryAddress(MediatedRequestContext context) {
    log.info("addDeliveryAddress:: adding delivery address");
    String deliveryAddressTypeId = context.request().getDeliveryAddressTypeId();
    if (deliveryAddressTypeId == null) {
      log.info("addDeliveryAddress:: deliveryAddressTypeId is null");
      context.request().deliveryAddress(null);
      return;
    }

    context.requester()
      .getPersonal()
      .getAddresses()
      .stream()
      .filter(address -> deliveryAddressTypeId.equals(address.getAddressTypeId()))
      .findFirst()
      .ifPresent(address -> context.request().setDeliveryAddress(
        new MediatedRequestDeliveryAddress()
          .addressTypeId(address.getAddressTypeId())
          .addressLine1(address.getAddressLine1())
          .addressLine2(address.getAddressLine2())
          .city(address.getCity())
          .region(address.getRegion())
          .countryId(address.getCountryId())
          .postalCode(address.getPostalCode())
    ));
  }

  private static void addSearchIndex(MediatedRequestContext context) {
    log.info("addSearchIndex:: adding search index");
    MediatedRequestSearchIndex searchIndex = new MediatedRequestSearchIndex();

    Item item = context.item();
    if (item != null) {
      log.info("addSearchIndex:: adding item data to search index");
      String shelvingOrder = item.getEffectiveShelvingOrder();
      ItemEffectiveCallNumberComponents callNumberComponents = item.getEffectiveCallNumberComponents();
      if (shelvingOrder != null) {
        log.info("addSearchIndex:: adding shelving order to search index");
        searchIndex.setShelvingOrder(shelvingOrder);
      }
      if (callNumberComponents != null) {
        log.info("addSearchIndex:: adding call number components to search index");
        searchIndex.callNumberComponents(new MediatedRequestSearchIndexCallNumberComponents()
          .callNumber(callNumberComponents.getCallNumber())
          .prefix(callNumberComponents.getPrefix())
          .suffix(callNumberComponents.getSuffix()));
      }
    }

    ServicePoint pickupServicePoint = context.pickupServicePoint();
    if (pickupServicePoint != null && pickupServicePoint.getName() != null) {
      log.info("addSearchIndex:: adding pickup service point data to search index");
      searchIndex.setPickupServicePointName(pickupServicePoint.getName());
    }

    context.request().searchIndex(searchIndex);
  }

  private static void removeExistingRequestDetails(MediatedRequest request) {
    log.debug("removeExistingRequestDetails:: removing existing request details");
    request.item(null)
      .requester(null)
      .proxy(null)
      .instance(null)
      .pickupServicePoint(null)
      .searchIndex(null);
  }

  private static void processRequestStatus(MediatedRequest mediatedRequest) {
    if (mediatedRequest.getStatus() == null) {
      log.info("processRequestStatus:: using default request status: '{}'", DEFAULT_STATUS.getValue());
      mediatedRequest.setStatus(DEFAULT_STATUS);
    }
    if (mediatedRequest.getMediatedWorkflow() == null) {
      log.info("processRequestStatus:: using default workflow: '{}'", DEFAULT_WORKFLOW);
      mediatedRequest.mediatedWorkflow(DEFAULT_WORKFLOW);
    }

    var statusElements = mediatedRequest.getStatus().toString().split(" - ");
    if (statusElements.length == 2) {
      String status = statusElements[0];
      String step = statusElements[1];
      log.info("processRequestStatus:: status='{}', step='{}'", status, step);
      mediatedRequest.setMediatedRequestStatus(
        MediatedRequest.MediatedRequestStatusEnum.fromValue(status));
      mediatedRequest.setMediatedRequestStep(step);
      return;
    }

    log.warn("processRequestStatus:: invalid status: {}", mediatedRequest.getStatus());
  }

  private static <T> void addIfNotNull(Collection<T> collection, T obj) {
    Optional.ofNullable(obj)
      .ifPresent(collection::add);
  }

  private static <T> Set<String> extractValues(Map<String, T> objects,
    Function<T, String> valueExtractor) {

    return objects.values().stream()
      .map(valueExtractor)
      .filter(Objects::nonNull)
      .collect(toSet());
  }

  @Builder
  private record MediatedRequestContext(MediatedRequest request, User requester,
    UserGroup requesterUserGroup, User proxyUser, UserGroup proxyUserGroup, Item item,
    Instance instance, ServicePoint pickupServicePoint, Location location, Library library) {
  }
}
