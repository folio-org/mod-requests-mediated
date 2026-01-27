package org.folio.mr.service.impl;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;
import static org.folio.mr.domain.dto.MediatedRequest.FulfillmentPreferenceEnum.DELIVERY;
import static org.folio.mr.domain.dto.MediatedRequest.FulfillmentPreferenceEnum.HOLD_SHELF;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.NEW_AWAITING_CONFIRMATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.folio.mr.client.InstanceClient;
import org.folio.mr.client.ItemClient;
import org.folio.mr.client.LibraryClient;
import org.folio.mr.client.LocationClient;
import org.folio.mr.client.SearchClient;
import org.folio.mr.client.SearchInstancesClient;
import org.folio.mr.client.ServicePointClient;
import org.folio.mr.client.UserClient;
import org.folio.mr.client.UserGroupClient;
import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.Instances;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.Items;
import org.folio.mr.domain.dto.Libraries;
import org.folio.mr.domain.dto.Library;
import org.folio.mr.domain.dto.Location;
import org.folio.mr.domain.dto.Locations;
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
import org.folio.mr.domain.dto.SearchInstance;
import org.folio.mr.domain.dto.SearchInstancesResponse;
import org.folio.mr.domain.dto.SearchItem;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.domain.dto.ServicePoints;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserGroup;
import org.folio.mr.domain.dto.UserGroups;
import org.folio.mr.domain.dto.UserPersonal;
import org.folio.mr.domain.dto.Users;
import org.folio.mr.service.BulkFetchingService;
import org.folio.mr.service.InventoryService;
import org.folio.mr.service.MediatedRequestDetailsService;
import org.folio.mr.service.UserService;
import org.folio.mr.service.impl.MediatedRequestDetailsServiceImpl.MediatedRequestContext.MediatedRequestContextBuilder;
import org.folio.spring.service.SystemUserScopedExecutionService;
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
  private final SystemUserScopedExecutionService executionService;
  private final SearchClient searchClient;
  private final SearchInstancesClient searchInstancesClient;
  private final UserClient userClient;
  private final UserGroupClient userGroupClient;
  private final InstanceClient instanceClient;
  private final ItemClient itemClient;
  private final LocationClient locationClient;
  private final LibraryClient libraryClient;
  private final ServicePointClient servicePointClient;
  private final BulkFetchingService fetchingService;

  @Override
  public MediatedRequest addRequestDetailsForCreate(MediatedRequest request) {
    removeExistingRequestDetails(request);
    processRequestStatus(request);
    MediatedRequestContext context = buildRequestContext(request);

    addRequester(context);
    extendRequester(context);
    addRequesterGroup(context);
    addProxy(context);
    extendProxy(context);
    addProxyGroup(context);
    addInstance(context);
    extendInstance(context);
    addItem(context);
    extendItem(context);
    addFulfillmentDetails(context);
    addSearchIndex(context);

    // Metadata is now handled by JPA auditing in MediatedRequestEntity
    // No need to manually update metadata

    return request;
  }

  @Override
  public MediatedRequest addRequestDetailsForUpdate(MediatedRequest request) {
    removeExistingRequestDetails(request);
    processRequestStatus(request);

    MediatedRequestContext context = buildRequestContext(request);
    addRequester(context);
    addRequesterGroup(context);
    addProxy(context);
    addProxyGroup(context);
    addInstance(context);
    addItem(context);
    addSearchIndex(context);

    // Metadata is now handled by JPA auditing in MediatedRequestEntity
    // No need to manually update metadata

    return request;
  }

  @Override
  public MediatedRequest addRequestDetailsForGet(MediatedRequest request) {
    MediatedRequestContext context = buildRequestContext(request);
    addRequestDetailsForGet(context);
    return request;
  }

  @Override
  public List<MediatedRequest> addRequestBatchDetailsForGet(List<MediatedRequest> requests) {
    var batchContext = buildRequestBatchContext(requests);
    return requests.stream()
      .map(request -> addRequestDetailsForGet(request, batchContext))
      .toList();
  }

  private MediatedRequest addRequestDetailsForGet(MediatedRequest request,
    MediatedRequestBatchContext batchContext) {

    var context = extractRequestContextFromBatchContext(request, batchContext);
    addRequestDetailsForGet(context);
    return request;
  }

  private void addRequestDetailsForGet(MediatedRequestContext context) {
    extendRequester(context);
    addRequesterGroup(context);
    extendProxy(context);
    addProxyGroup(context);
    extendInstance(context);
    extendItem(context);
    addFulfillmentDetails(context);
  }

  private MediatedRequestContext buildRequestContext(MediatedRequest request) {
    log.info("buildRequestContext:: building context for mediated request {}", request::getId);
    var contextBuilder = MediatedRequestContext.builder().request(request);

    User requester = fetchRequester(request);
    contextBuilder.requester(requester);
    contextBuilder.requesterGroup(userService.fetchUserGroup(requester.getPatronGroup()));

    var searchInstances = searchClient.searchInstance(request.getInstanceId()).getInstances();
    handleSearchInstances(searchInstances, contextBuilder, request);
    fetchProxyUser(request, contextBuilder);
    fetchPickupServicePoint(request, contextBuilder);

    log.debug("buildRequestContext:: request context is built");
    return contextBuilder.build();
  }

  private User fetchRequester(MediatedRequest request) {
    return Optional.ofNullable(userService.fetchUser(request.getRequesterId()))
      .orElseGet(() -> createFallbackUser(request.getRequester()));
  }

  private void handleSearchInstances(List<SearchInstance> searchInstances,
    MediatedRequestContextBuilder ctxBuilder, MediatedRequest request) {

    if (searchInstances == null || searchInstances.isEmpty()) {
      ctxBuilder.instance(createFallbackInstance(request));
      return;
    }

    fetchInventoryInstance(searchInstances.get(0), ctxBuilder, request);
  }

  private void fetchInventoryInstance(SearchInstance searchInstance,
    MediatedRequestContextBuilder ctxBuilder, MediatedRequest request) {

    executionService.executeAsyncSystemUserScoped(searchInstance.getTenantId(),
      () -> ctxBuilder.instance(inventoryService.fetchInstance(searchInstance.getId())));

    if (request.getItemId() == null) {
      log.debug("fetchInventoryInstance:: itemId is null");
      return;
    }

    searchInstance.getItems().stream()
      .filter(searchItem -> searchItem.getId().equals(request.getItemId()))
      .findFirst()
      .ifPresentOrElse(
        searchItem -> fetchInventoryItem(searchItem, ctxBuilder, request),
        () -> ctxBuilder.item(createFallbackItem(request))
      );
  }

  private void fetchInventoryItem(SearchItem searchItem, MediatedRequestContextBuilder ctxBuilder,
    MediatedRequest request) {

    log.info("fetchInventoryItem:: fetching inventory item {}", searchItem.getId());
    executionService.executeAsyncSystemUserScoped(searchItem.getTenantId(), () -> {
      Item inventoryItem = inventoryService.fetchItem(searchItem.getId());
      if (inventoryItem != null) {
        log.info("fetchInventoryItem:: inventoryItem {} found", searchItem.getId());
        var location = inventoryService.fetchLocation(inventoryItem.getEffectiveLocationId());
        var library = inventoryService.fetchLibrary(location.getLibraryId());
        ctxBuilder.item(inventoryItem).location(location).library(library);
      } else {
        log.info("fetchInventoryItem:: inventoryItem {} not found", searchItem.getId());
        ctxBuilder.item(createFallbackItem(request));
      }
    });
  }

  private void fetchProxyUser(MediatedRequest request, MediatedRequestContextBuilder ctxBuilder) {
    if (request.getProxyUserId() == null) {
      log.debug("fetchProxyUser:: proxyUserId is null");
      return;
    }
    User proxy = userService.fetchUser(request.getProxyUserId());
    if (proxy != null) {
      UserGroup proxyGroup = userService.fetchUserGroup(proxy.getPatronGroup());
      ctxBuilder.proxy(proxy).proxyGroup(proxyGroup);
    } else {
      log.info("fetchProxyUser:: proxy user {} not found", request.getProxyUserId());
      MediatedRequestProxy medReqProxy = request.getProxy();
      ctxBuilder.proxy(new User().barcode(medReqProxy.getBarcode())
        .personal(new UserPersonal()
          .firstName(medReqProxy.getFirstName())
          .lastName(medReqProxy.getLastName())
          .middleName(medReqProxy.getMiddleName())));
    }
  }

  private void fetchPickupServicePoint(MediatedRequest request,
    MediatedRequestContextBuilder ctxBuilder) {

    if (request.getPickupServicePointId() == null) {
      log.debug("fetchPickupServicePoint:: pickupServicePointId is null");
      return;
    }

    ctxBuilder.pickupServicePoint(inventoryService.fetchServicePoint(
      request.getPickupServicePointId()));
  }

  private MediatedRequestBatchContext buildRequestBatchContext(List<MediatedRequest> requests) {
    var contextBuilder = MediatedRequestBatchContext.builder().requests(requests);

    var users = fetchRequestersAndProxies(requests);
    contextBuilder.users(toMap(users, User::getId));
    contextBuilder.userGroups(toMap(fetchUserGroups(users), UserGroup::getId));

    var searchInstances = fetchSearchInstances(requests);

    var inventoryInstances = fetchInventoryInstances(searchInstances);
    contextBuilder.instances(toMap(inventoryInstances, Instance::getId));

    var itemsAndRelatedRecords = fetchItemsAndRelatedRecords(searchInstances);
    contextBuilder.items(toMap(itemsAndRelatedRecords.items, Item::getId));
    contextBuilder.locations(toMap(itemsAndRelatedRecords.locations, Location::getId));
    contextBuilder.libraries(toMap(itemsAndRelatedRecords.libraries, Library::getId));

    var pickupServicePoints = fetchPickupServicePoints(requests);
    contextBuilder.pickupServicePoints(toMap(pickupServicePoints, ServicePoint::getId));

    return contextBuilder.build();
  }

  private Collection<User> fetchRequestersAndProxies(List<MediatedRequest> requests) {
    log.info("fetchRequesters:: fetching requesters and proxies for {} requests", requests::size);

    var userIds = requests.stream()
      .flatMap(request -> Stream.of(request.getRequesterId(), request.getProxyUserId()))
      .filter(Objects::nonNull)
      .distinct()
      .toList();

    if (userIds.isEmpty()) {
      log.info("fetchRequesters:: no user IDs to fetch");
      return Collections.emptyList();
    }

    return fetchingService.fetchByIds(userClient, userIds, Users::getUsers);
  }

  private Collection<UserGroup> fetchUserGroups(Collection<User> requesters) {
    log.info("fetchUserGroups:: fetching user groups for {} requesters", requesters::size);

    var requesterGroupIds = requesters.stream()
      .map(User::getPatronGroup)
      .filter(Objects::nonNull)
      .distinct()
      .toList();

    if (requesterGroupIds.isEmpty()) {
      log.info("fetchUserGroups:: no requesterGroupIds found");
      return Collections.emptyList();
    }

    return fetchingService.fetchByIds(userGroupClient, requesterGroupIds,
      UserGroups::getUsergroups);
  }

  private Collection<SearchInstance> fetchSearchInstances(Collection<MediatedRequest> requests) {
    log.info("fetchSearchInstances:: fetching search instances for {} requests", requests::size);

    var instanceIds = requests.stream()
      .map(MediatedRequest::getInstanceId)
      .filter(Objects::nonNull)
      .distinct()
      .toList();

    if (instanceIds.isEmpty()) {
      log.info("fetchSearchInstances:: no instanceIds found");
      return Collections.emptyList();
    }

    return fetchingService.fetchByUuidIndex(searchInstancesClient, "id", instanceIds,
      Map.of("expandAll", "true"), SearchInstancesResponse::getInstances);
  }

  private Collection<Instance> fetchInventoryInstances(Collection<SearchInstance> searchInstances) {
    log.info("fetchInventoryInstances:: fetching inventory instances for {} instances",
      searchInstances::size);

    var instanceIdsByTenant = searchInstances.stream()
        .collect(groupingBy(SearchInstance::getTenantId, mapping(SearchInstance::getId, toSet())));

    return instanceIdsByTenant.keySet().stream()
      .map(tenantId -> executionService.executeSystemUserScoped(tenantId,
        () -> fetchingService.fetchByIds(
          instanceClient, instanceIdsByTenant.get(tenantId), Instances::getInstances)))
      .flatMap(Collection::stream)
      .toList();
  }

  private ItemsAndRelatedRecords fetchItemsAndRelatedRecords(
    Collection<SearchInstance> searchInstances) {

    log.info("fetchItemsAndRelatedRecords:: fetching items and related records for {} instances",
      searchInstances::size);

    var itemsByTenant = searchInstances.stream()
      .map(SearchInstance::getItems)
      .flatMap(Collection::stream)
      .filter(Objects::nonNull)
      .collect(groupingBy(SearchItem::getTenantId, toSet()));

    return itemsByTenant.keySet().stream()
      .map(tenantId -> executionService.executeSystemUserScoped(tenantId,
        () -> fetchItemsAndRelatedRecordsForTenant(tenantId, itemsByTenant.get(tenantId))))
      .reduce(ItemsAndRelatedRecords::combine)
      .orElseGet(ItemsAndRelatedRecords::empty);
  }

  private ItemsAndRelatedRecords fetchItemsAndRelatedRecordsForTenant(String tenantId,
    Collection<SearchItem> searchItems) {

    log.info("fetchItemsAndRelatedRecordsForTenant:: fetching items and related records for " +
        "tenant {} and {} search items", () -> tenantId, searchItems::size);

    var itemIds = searchItems.stream()
      .map(SearchItem::getId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    var effectiveLocationIds = searchItems.stream()
      .map(SearchItem::getEffectiveLocationId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

    var items = fetchingService.fetchByIds(itemClient, itemIds, Items::getItems);
    var locations = fetchingService.fetchByIds(locationClient, effectiveLocationIds,
      Locations::getLocations);
    var libraries = fetchingService.fetchByIds(libraryClient, locations.stream()
      .map(Location::getLibraryId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet()), Libraries::getLoclibs);

    return ItemsAndRelatedRecords.builder()
      .items(items)
      .locations(locations)
      .libraries(libraries)
      .build();
  }

  private Collection<ServicePoint> fetchPickupServicePoints(Collection<MediatedRequest> requests) {
    log.info("fetchServicePoints:: fetching service points for {} requests", requests::size);

    var pickupServicePointIds = requests.stream()
      .map(MediatedRequest::getPickupServicePointId)
      .filter(Objects::nonNull)
      .distinct()
      .toList();

    if (pickupServicePointIds.isEmpty()) {
      log.info("fetchServicePoints:: no pickupServicePointIds found");
      return Collections.emptyList();
    }

    return fetchingService.fetchByIds(servicePointClient, pickupServicePointIds,
      ServicePoints::getServicepoints);
  }

  private Instance createFallbackInstance(MediatedRequest request) {
    var instance = request.getInstance();
    if (instance == null) {
      log.info("createFallbackInstance:: instance is null");
      return new Instance();
    }

    log.debug("createFallbackInstance:: instance hrid: {}", instance::getHrid);
    return new Instance()
      .hrid(instance.getHrid())
      .title(instance.getTitle());
  }

  private Item createFallbackItem(MediatedRequest request) {
    var item = request.getItem();
    if (item == null) {
      log.info("createFallbackItem:: item is null");
      return new Item();
    }

    log.debug("createFallbackItem:: item barcode: {}", item::getBarcode);
    return new Item().barcode(item.getBarcode());
  }

  private User createFallbackUser(MediatedRequestRequester mediatedRequestRequester) {
    return new User().barcode(mediatedRequestRequester.getBarcode())
      .personal(new UserPersonal()
        .firstName(mediatedRequestRequester.getFirstName())
        .lastName(mediatedRequestRequester.getLastName())
        .middleName(mediatedRequestRequester.getMiddleName()));
  }

  private static void addRequester(MediatedRequestContext context) {
    log.debug("addRequester:: adding requester data");
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
    log.debug("extendRequester:: extending requester data");
    context.request()
      .getRequester()
      .patronGroupId(context.requester().getPatronGroup());
  }

  private static void addRequesterGroup(MediatedRequestContext context) {
    log.debug("addRequesterGroup:: adding requester user group data");
    UserGroup userGroup = context.requesterGroup();
    if (userGroup == null) {
      log.debug("addRequesterGroup:: userGroup is null");
      return;
    }
    context.request()
      .getRequester()
      .patronGroup(new MediatedRequestRequesterPatronGroup()
        .id(userGroup.getId())
        .group(userGroup.getGroup())
        .desc(userGroup.getDesc()));
  }

  private static void addProxy(MediatedRequestContext context) {
    log.debug("addProxy:: adding proxy user data");
    User proxy = context.proxy();
    if (proxy == null) {
      log.debug("addProxy:: proxy user is null");
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

  private static void extendProxy(MediatedRequestContext context) {
    log.debug("extendProxy:: extending proxy user data");
    if (context.proxy() == null) {
      log.debug("extendProxy:: proxy user is null");
      context.request().proxy(null);
      return;
    }

    context.request()
      .getProxy()
      .patronGroupId(context.proxy().getPatronGroup());
  }

  private static void addProxyGroup(MediatedRequestContext context) {
    log.debug("addProxyGroup:: adding proxy user group data");
    if (context.proxyGroup() == null) {
      log.debug("addProxyGroup:: proxy user group is null");
      return;
    }

    UserGroup userGroup = context.proxyGroup();
    context.request()
      .getProxy()
      .patronGroup(new MediatedRequestProxyPatronGroup()
        .id(userGroup.getId())
        .group(userGroup.getGroup())
        .desc(userGroup.getDesc()));
  }

  private static void addInstance(MediatedRequestContext context) {
    log.debug("addInstance:: adding instance data");
    var instance = context.instance();
    if (instance == null) {
      log.debug("addInstance:: instance is null");
      context.request().instance(null);
      return;
    }
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

  private void extendInstance(MediatedRequestContext context) {
    log.debug("extendInstance:: extending instance data");
    var instance = context.instance();

    var contributors = Optional.ofNullable(instance.getContributors())
      .orElse(Collections.emptyList())
      .stream()
      .map(contributor -> Optional.ofNullable(contributor.getName())
        .map(name -> new MediatedRequestInstanceContributorNamesInner().name(name))
        .orElse(null))
      .filter(Objects::nonNull)
      .toList();

    var publications = Optional.ofNullable(instance.getPublication())
      .orElse(Collections.emptyList())
      .stream()
      .map(publication -> new MediatedRequestInstancePublicationInner()
        .publisher(Optional.ofNullable(publication.getPublisher()).orElse(""))
        .place(Optional.ofNullable(publication.getPlace()).orElse(""))
        .dateOfPublication(Optional.ofNullable(publication.getDateOfPublication()).orElse(""))
        .role(Optional.ofNullable(publication.getRole()).orElse("")))
      .toList();

    var editions = new ArrayList<>(Optional.ofNullable(instance.getEditions())
      .orElse(Collections.emptySet()));

    context.request()
      .getInstance()
      .contributorNames(contributors)
      .publication(publications)
      .editions(editions)
      .hrid(instance.getHrid());
  }
  private static void addItem(MediatedRequestContext context) {
    log.debug("addItem:: adding item data");
    if (context.item() == null) {
      log.debug("addItem:: item is null");
      context.request().item(null);
      return;
    }
    context.request()
      .item(new MediatedRequestItem().barcode(context.item().getBarcode()));
  }

  private void extendItem(MediatedRequestContext context) {
    log.debug("extendItem:: extending item data");
    var item = context.item();
    if (item == null) {
      log.debug("extendItem:: item is null");
      context.request().item(null);
      return;
    }
    context.request().getItem()
      .enumeration(item.getEnumeration())
      .volume(item.getVolume())
      .chronology(item.getChronology())
      .displaySummary(item.getDisplaySummary())
      .status(item.getStatus() != null ? item.getStatus().getName().getValue() : null)
      .copyNumber(item.getCopyNumber())
      .location(new MediatedRequestItemLocation()
        .name(context.location() != null ? context.location().getName() : null)
        .code(context.location() != null ? context.location().getCode() : null)
        .libraryName(context.library() != null ? context.library().getName() : null));

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
    if (fulfillmentPreference == null) {
      log.debug("addFulfillmentDetails:: fulfillment preference is null");
      return;
    }

    log.debug("addFulfillmentDetails:: fulfillment preference is '{}'", fulfillmentPreference.getValue());

    if (fulfillmentPreference == DELIVERY) {
      addDeliveryAddress(context);
    } else if (fulfillmentPreference == HOLD_SHELF) {
      addPickupServicePoint(context);
    }
  }

  private static void addPickupServicePoint(MediatedRequestContext context) {
    log.debug("addPickupServicePoint:: adding pickup service point data");
    ServicePoint pickupServicePoint = context.pickupServicePoint();
    if (pickupServicePoint == null) {
      log.debug("addPickupServicePoint:: pickup service point is null");
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
    log.debug("addDeliveryAddress:: adding delivery address");
    String deliveryAddressTypeId = context.request().getDeliveryAddressTypeId();
    if (deliveryAddressTypeId == null) {
      log.debug("addDeliveryAddress:: deliveryAddressTypeId is null");
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
    log.debug("addSearchIndex:: adding search index");
    MediatedRequestSearchIndex searchIndex = new MediatedRequestSearchIndex();

    var searchItem = context.item();
    if (searchItem != null) {
      log.debug("addSearchIndex:: adding item data to search index");
      String shelvingOrder = searchItem.getEffectiveShelvingOrder();
      var callNumberComponents = searchItem.getEffectiveCallNumberComponents();
      if (shelvingOrder != null) {
        log.debug("addSearchIndex:: adding shelving order to search index");
        searchIndex.setShelvingOrder(shelvingOrder);
      }
      if (callNumberComponents != null) {
        log.debug("addSearchIndex:: adding call number components to search index");
        searchIndex.callNumberComponents(new MediatedRequestSearchIndexCallNumberComponents()
          .callNumber(callNumberComponents.getCallNumber())
          .prefix(callNumberComponents.getPrefix())
          .suffix(callNumberComponents.getSuffix()));
      }
    }

    ServicePoint pickupServicePoint = context.pickupServicePoint();
    if (pickupServicePoint != null && pickupServicePoint.getName() != null) {
      log.debug("addSearchIndex:: adding pickup service point data to search index");
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

  private MediatedRequestContext extractRequestContextFromBatchContext(MediatedRequest request,
    MediatedRequestBatchContext batchContext) {

    log.info("extractRequestContextFromBatchContext:: mediated request {}", request::getId);

    var contextBuilder = MediatedRequestContext.builder().request(request);

    var requester = batchContext.users.get(request.getRequesterId());
    if (requester == null) {
      requester = createFallbackUser(request.getRequester());
    }
    contextBuilder.requester(requester);
    contextBuilder.requesterGroup(batchContext.userGroups.get(requester.getPatronGroup()));

    var instance = batchContext.instances.get(request.getInstanceId());
    if (instance == null) {
      contextBuilder.instance(createFallbackInstance(request));
    } else {
      contextBuilder.instance(instance);
    }

    var item = batchContext.items.get(request.getItemId());
    if (item != null) {
      var location = batchContext.locations.get(item.getEffectiveLocationId());
      var library = batchContext.libraries.get(location.getLibraryId());
      contextBuilder.item(item).location(location).library(library);
    } else {
      contextBuilder.item(createFallbackItem(request));
    }

    if (request.getProxyUserId() != null) {
      contextBuilder.proxy(batchContext.users.get(request.getProxyUserId()));
    }

    if (request.getPickupServicePointId() != null) {
      contextBuilder.pickupServicePoint(
        batchContext.pickupServicePoints.get(request.getPickupServicePointId()));
    }

    log.debug("extractRequestContextFromBatchContext:: extracted context for {}", request::getId);

    return contextBuilder.build();
  }

  private <T> Map<String, T> toMap(Collection<T> collection, Function<T, String> keyMapper) {
    return collection.stream().collect(Collectors.toMap(keyMapper, identity(), (existing, replacement) -> existing));
  }

  @Builder
  public record MediatedRequestContext(MediatedRequest request, User requester,
    UserGroup requesterGroup, User proxy, UserGroup proxyGroup, Item item,
    Instance instance, ServicePoint pickupServicePoint, Location location, Library library) {
  }

  @Builder
  public record MediatedRequestBatchContext(
    List<MediatedRequest> requests,
    Map<String, User> users,
    Map<String, UserGroup> userGroups,
    Map<String, Instance> instances,
    Map<String, Item> items,
    Map<String, ServicePoint> pickupServicePoints,
    Map<String, Location> locations,
    Map<String, Library> libraries) {
  }

  @Builder
  public record ItemsAndRelatedRecords(Collection<Item> items, Collection<Location> locations,
    Collection<Library> libraries) {

    public static ItemsAndRelatedRecords empty() {
      return ItemsAndRelatedRecords.builder()
        .items(Collections.emptyList())
        .locations(Collections.emptyList())
        .libraries(Collections.emptyList())
        .build();
    }

    public static ItemsAndRelatedRecords combine(ItemsAndRelatedRecords a, ItemsAndRelatedRecords b) {
      return ItemsAndRelatedRecords.builder()
        .items(Stream.of(a.items, b.items).flatMap(Collection::stream).toList())
        .locations(Stream.of(a.locations, b.locations).flatMap(Collection::stream).toList())
        .libraries(Stream.of(a.libraries, b.libraries).flatMap(Collection::stream).toList())
        .build();
    }
  }

}
