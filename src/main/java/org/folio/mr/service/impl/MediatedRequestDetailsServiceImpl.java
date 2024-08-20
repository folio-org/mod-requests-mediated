package org.folio.mr.service.impl;

import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.NEW_AWAITING_CONFIRMATION;

import java.util.ArrayList;

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
import org.folio.mr.service.InventoryService;
import org.folio.mr.service.MediatedRequestDetailsService;
import org.folio.mr.service.MetadataService;
import org.folio.mr.service.UserService;
import org.springframework.stereotype.Service;

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
    addRequesterGroup(context);
    addProxy(context);
    extendProxy(context);
    addProxyGroup(context);
    addInstance(context);
    extendInstance(context);
    addItem(context);
    extendItem(context);
    addPickupServicePoint(context);
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
    addPickupServicePoint(context);

    return request;
  }

  private MediatedRequestContext buildRequestContext(MediatedRequest request) {
    log.info("buildRequestContext:: building request context");
    var contextBuilder = MediatedRequestContext.builder().request(request);
    Instance instance = inventoryService.fetchInstance(request.getInstanceId());
    User requester = userService.fetchUser(request.getRequesterId());
    UserGroup requesterGroup = userService.fetchUserGroup(requester.getPatronGroup());

    contextBuilder.instance(instance)
      .requester(requester)
      .requesterGroup(requesterGroup);

    if (request.getProxyUserId() != null) {
      User proxy = userService.fetchUser(request.getProxyUserId());
      UserGroup proxyGroup = userService.fetchUserGroup(proxy.getPatronGroup());
      contextBuilder.proxy(proxy)
        .proxyGroup(proxyGroup);
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

  private static void extendRequester(MediatedRequestContext context) {
    log.info("extendRequester:: extending requester data");
    context.getRequest()
      .getRequester()
      .patronGroupId(context.getRequester().getPatronGroup());
  }

  private static void addRequesterGroup(MediatedRequestContext context) {
    log.info("addRequesterGroup:: adding requester user group data");
    UserGroup userGroup = context.getRequesterGroup();
    context.getRequest()
      .getRequester()
      .patronGroup(new MediatedRequestRequesterPatronGroup()
        .id(userGroup.getId())
        .group(userGroup.getGroup())
        .desc(userGroup.getDesc()));
  }

  private static void addProxy(MediatedRequestContext context) {
    log.info("addProxy:: adding proxy user data");
    User proxy = context.getProxy();
    if (proxy == null) {
      log.info("addProxy:: proxy user is null");
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

  private static void extendProxy(MediatedRequestContext context) {
    log.info("extendProxy:: extending proxy user data");
    if (context.getProxy() == null) {
      log.info("extendProxy:: proxy user is null");
      context.getRequest().proxy(null);
      return;
    }

    context.getRequest()
      .getProxy()
      .patronGroupId(context.getProxy().getPatronGroup());
  }

  private static void addProxyGroup(MediatedRequestContext context) {
    log.info("addProxyGroup:: adding proxy user group data");
    if (context.getProxyGroup() == null) {
      log.info("addProxyGroup:: proxy user group is null");
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

  private static void addInstance(MediatedRequestContext context) {
    log.info("addInstance:: adding instance data");
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

  private static void extendInstance(MediatedRequestContext context) {
    log.info("extendInstance:: extending instance data");
    Instance instance = context.getInstance();

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

    context.getRequest()
      .getInstance()
      .contributorNames(contributors)
      .publication(publications)
      .editions(new ArrayList<>(instance.getEditions()));
  }

  private static void addItem(MediatedRequestContext context) {
    log.info("addItem:: adding item data");
    if (context.getItem() == null) {
      log.info("addItem:: item is null");
      context.getRequest().item(null);
      return;
    }
    context.getRequest()
      .item(new MediatedRequestItem().barcode(context.getItem().getBarcode()));
  }

  private static void extendItem(MediatedRequestContext context) {
    log.info("extendItem:: extending item data");
    Item item = context.getItem();
    if (item == null) {
      log.info("extendItem:: item is null");
      context.getRequest().item(null);
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

  private static void addPickupServicePoint(MediatedRequestContext context) {
    log.info("addPickupServicePoint:: adding pickup service point data");
    ServicePoint pickupServicePoint = context.getPickupServicePoint();
    if (pickupServicePoint == null) {
      log.info("addPickupServicePoint:: pickup service point is null");
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

  private static void addSearchIndex(MediatedRequestContext context) {
    log.info("addSearchIndex:: adding search index");
    MediatedRequestSearchIndex searchIndex = new MediatedRequestSearchIndex();

    Item item = context.getItem();
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

    ServicePoint pickupServicePoint = context.getPickupServicePoint();
    if (pickupServicePoint != null && pickupServicePoint.getName() != null) {
      log.info("addSearchIndex:: adding pickup service point data to search index");
      searchIndex.setPickupServicePointName(pickupServicePoint.getName());
    }

    context.getRequest().searchIndex(searchIndex);
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

}