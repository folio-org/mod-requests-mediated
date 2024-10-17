package org.folio.mr.service.impl;

import static org.folio.mr.domain.dto.MediatedRequest.FulfillmentPreferenceEnum.DELIVERY;
import static org.folio.mr.domain.dto.MediatedRequest.FulfillmentPreferenceEnum.HOLD_SHELF;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.NEW_AWAITING_CONFIRMATION;

import java.util.ArrayList;

import org.folio.mr.client.SearchClient;
import org.folio.mr.domain.dto.InstanceContributorsInner;
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
import org.folio.mr.domain.dto.SearchInstance;
import org.folio.mr.domain.dto.SearchItem;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserGroup;
import org.folio.mr.domain.dto.UserPersonal;
import org.folio.mr.service.InventoryService;
import org.folio.mr.service.MediatedRequestDetailsService;
import org.folio.mr.service.MetadataService;
import org.folio.mr.service.UserService;
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
  private final MetadataService metadataService;
  private final SystemUserScopedExecutionService executionService;
  private final SearchClient searchClient;

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

    metadataService.updateMetadata(request);

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

    metadataService.updateMetadata(request);

    return request;
  }

  @Override
  public MediatedRequest addRequestDetailsForGet(MediatedRequest request) {
    MediatedRequestContext context = buildRequestContext(request);

    extendRequester(context);
    addRequesterGroup(context);
    extendProxy(context);
    addProxyGroup(context);
    extendInstance(context);
    extendItem(context);
    addFulfillmentDetails(context);

    return request;
  }

  private MediatedRequestContext buildRequestContext(MediatedRequest request) {
    log.info("buildRequestContext:: building request context");
    var contextBuilder = MediatedRequestContext.builder().request(request);
    var searchInstance = searchClient.searchInstance(request.getInstanceId())
      .getInstances().get(0);

    User requester = userService.fetchUser(request.getRequesterId());
    UserGroup requesterGroup = userService.fetchUserGroup(requester.getPatronGroup());

    contextBuilder.instance(searchInstance)
      .requester(requester)
      .requesterGroup(requesterGroup);

    if (request.getProxyUserId() != null) {
      User proxy = userService.fetchUser(request.getProxyUserId());
      UserGroup proxyGroup = userService.fetchUserGroup(proxy.getPatronGroup());
      contextBuilder.proxy(proxy)
        .proxyGroup(proxyGroup);
    }

    if (request.getItemId() != null) {
        searchInstance.getItems().stream()
          .filter(searchItem -> searchItem.getId().equals(request.getItemId()))
          .findFirst()
          .ifPresent(item -> {
            log.info("buildRequestContext:: item found");
            String tenantId = item.getTenantId();
            executionService.executeAsyncSystemUserScoped(tenantId, () -> {
              Location location = inventoryService.fetchLocation(item.getEffectiveLocationId());
              Library library = inventoryService.fetchLibrary(location.getLibraryId());
              contextBuilder.item(item)
                .location(location)
                .library(library);
            });
        });

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

  private static void addRequesterGroup(MediatedRequestContext context) {
    log.info("addRequesterGroup:: adding requester user group data");
    UserGroup userGroup = context.requesterGroup();
    context.request()
      .getRequester()
      .patronGroup(new MediatedRequestRequesterPatronGroup()
        .id(userGroup.getId())
        .group(userGroup.getGroup())
        .desc(userGroup.getDesc()));
  }

  private static void addProxy(MediatedRequestContext context) {
    log.info("addProxy:: adding proxy user data");
    User proxy = context.proxy();
    if (proxy == null) {
      log.info("addProxy:: proxy user is null");
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
    log.info("extendProxy:: extending proxy user data");
    if (context.proxy() == null) {
      log.info("extendProxy:: proxy user is null");
      context.request().proxy(null);
      return;
    }

    context.request()
      .getProxy()
      .patronGroupId(context.proxy().getPatronGroup());
  }

  private static void addProxyGroup(MediatedRequestContext context) {
    log.info("addProxyGroup:: adding proxy user group data");
    if (context.proxyGroup() == null) {
      log.info("addProxyGroup:: proxy user group is null");
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
    log.info("addInstance:: adding instance data");
    var searchInstance = context.instance();
    if (searchInstance == null) {
      log.info("addInstance:: instance is null");
      context.request().instance(null);
      return;
    }
    var identifiers = searchInstance.getIdentifiers()
      .stream()
      .map(i -> new MediatedRequestInstanceIdentifiersInner()
        .value(i.getValue())
        .identifierTypeId(i.getIdentifierTypeId()))
      .toList();

    MediatedRequestInstance newInstance = new MediatedRequestInstance()
      .title(searchInstance.getTitle())
      .identifiers(identifiers);

    context.request().instance(newInstance);
  }

  private void extendInstance(MediatedRequestContext context) {
    log.info("extendInstance:: extending instance data");
    var searchInstance = context.instance();
    if (searchInstance == null) {
      log.info("extendInstance:: instance is null");
      context.request().instance(null);
      return;
    }
    executionService.executeAsyncSystemUserScoped(searchInstance.getTenantId(),
      () -> extendInstanceFromInventory(context, searchInstance));
  }

  private void extendInstanceFromInventory(MediatedRequestContext context,
    SearchInstance searchInstance) {

    var inventoryInstance = inventoryService.fetchInstance(searchInstance.getId());
    var contributors = inventoryInstance.getContributors()
      .stream()
      .map(InstanceContributorsInner::getName)
      .map(name -> new MediatedRequestInstanceContributorNamesInner().name(name))
      .toList();

    var publications = inventoryInstance.getPublication()
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
      .editions(new ArrayList<>(inventoryInstance.getEditions()));
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

  private void extendItem(MediatedRequestContext context) {
    log.info("extendItem:: extending item data");
    var searchItem = context.item();
    if (searchItem == null) {
      log.info("extendItem:: item is null");
      context.request().item(null);
      return;
    }
    executionService.executeAsyncSystemUserScoped(searchItem.getTenantId(),
      () -> extendItemFromInventory(context, searchItem));
  }

  private void extendItemFromInventory(MediatedRequestContext context, SearchItem searchItem) {
    var inventoryItem = inventoryService.fetchItem(searchItem.getId());
    context.request().getItem()
      .enumeration(inventoryItem.getEnumeration())
      .volume(inventoryItem.getVolume())
      .chronology(inventoryItem.getChronology())
      .displaySummary(inventoryItem.getDisplaySummary())
      .status(inventoryItem.getStatus().getName().getValue())
      .copyNumber(inventoryItem.getCopyNumber())
      .location(new MediatedRequestItemLocation()
        .name(context.location().getName())
        .code(context.location().getCode())
        .libraryName(context.library().getName()));

    var effectiveCallNumberComponents = searchItem.getEffectiveCallNumberComponents();
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
      log.info("addFulfillmentDetails:: fulfillment preference is null");
      return;
    }

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

    var searchItem = context.item();
    if (searchItem != null) {
      log.info("addSearchIndex:: adding item data to search index");
      String shelvingOrder = searchItem.getEffectiveShelvingOrder();
      var callNumberComponents = searchItem.getEffectiveCallNumberComponents();
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

  @Builder
  private record MediatedRequestContext(MediatedRequest request, User requester,
//    UserGroup requesterGroup, User proxy, UserGroup proxyGroup, Item item,
    UserGroup requesterGroup, User proxy, UserGroup proxyGroup, SearchItem item,
//    Instance instance, ServicePoint pickupServicePoint, Location location, Library library) {
    SearchInstance instance, ServicePoint pickupServicePoint, Location location, Library library) {
  }

}
