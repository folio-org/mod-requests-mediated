package org.folio.mr.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

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
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.Library;
import org.folio.mr.domain.dto.Location;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequestInstance;
import org.folio.mr.domain.dto.MediatedRequestItem;
import org.folio.mr.domain.dto.MediatedRequestRequester;
import org.folio.mr.domain.dto.Metadata;
import org.folio.mr.domain.dto.SearchInstance;
import org.folio.mr.domain.dto.SearchInstancesResponse;
import org.folio.mr.domain.dto.SearchItem;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserGroup;
import org.folio.mr.service.impl.MediatedRequestDetailsServiceImpl;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import lombok.SneakyThrows;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MediatedRequestDetailsServiceImplTest {

  @Mock
  private UserService userService;

  @Mock
  private SearchClient searchClient;

  @Mock
  private SystemUserScopedExecutionService executionService;

  @Mock
  private InventoryService inventoryService;

  @Mock
  private MetadataService metadataService;

  @Mock
  private SearchInstancesClient searchInstancesClient;

  @Mock
  private InstanceClient instanceClient;

  @Mock
  private UserClient userClient;

  @Mock
  private UserGroupClient userGroupClient;

  @Mock
  private ItemClient itemClient;

  @Mock
  private LocationClient locationClient;

  @Mock
  private LibraryClient libraryClient;

  @Mock
  private ServicePointClient servicePointClient;

  @Mock
  private BulkFetchingService fetchingService;

  @InjectMocks
  private MediatedRequestDetailsServiceImpl service;

  @Test
  void shouldFallbackToInstanceAndRequesterDetailsWhenSearchInstanceFindsNothing() {
    var originalRequest = new MediatedRequest()
      .requester(new MediatedRequestRequester()
        .barcode("111")
        .firstName("Test firstName")
        .lastName("Test lastName"))
      .instance(new MediatedRequestInstance()
        .title("Test instance title")
        .hrid("Test hrid"))
      .item(new MediatedRequestItem()
        .barcode("1"));
    var searchInstanceResponse = new SearchInstancesResponse().instances(
      Collections.emptyList());

    when(searchClient.searchInstance(any())).thenReturn(searchInstanceResponse);

    var returnedRequest = service.addRequestDetailsForGet(originalRequest);
    assertThat(returnedRequest.getRequester().getBarcode(), is(
      originalRequest.getRequester().getBarcode()));
    assertThat(returnedRequest.getRequester().getFirstName(), is(
      originalRequest.getRequester().getFirstName()));
    assertThat(returnedRequest.getRequester().getLastName(), is(
      originalRequest.getRequester().getLastName()));

    assertThat(returnedRequest.getInstance().getTitle(), is(
      originalRequest.getInstance().getTitle()));
    assertThat(returnedRequest.getInstance().getHrid(), is(
      originalRequest.getInstance().getHrid()));
  }

  @Test
  void shouldFallbackToItemAndRequesterDetailsWhenSearchInstanceFindsNoItem() {
    var originalRequest = new MediatedRequest()
      .requester(new MediatedRequestRequester()
        .barcode("111")
        .firstName("Test firstName")
        .lastName("Test lastName"))
      .instance(new MediatedRequestInstance())
      .itemId(UUID.randomUUID().toString())
      .item(new MediatedRequestItem()
        .barcode("1"));
    var searchInstanceResponse = new SearchInstancesResponse()
      .instances(List.of(new SearchInstance()
        .items(Collections.emptyList())));

    doAnswer(invocation -> {
      ((Runnable) invocation.getArguments()[1]).run();
      return null;
    }).when(executionService).executeAsyncSystemUserScoped(anyString(), any(Runnable.class));
    when(searchClient.searchInstance(any())).thenReturn(searchInstanceResponse
      .instances(List.of(new SearchInstance()
        .tenantId("test tenant")
        .items(Collections.emptyList()))));
    when(inventoryService.fetchInstance(any())).thenReturn(new Instance());

    var returnedRequest = service.addRequestDetailsForGet(originalRequest);
    assertThat(returnedRequest.getItem().getBarcode(), is(originalRequest.getItem().getBarcode()));

    assertThat(returnedRequest.getRequester().getBarcode(), is(originalRequest.getRequester().getBarcode()));
    assertThat(returnedRequest.getRequester().getFirstName(), is(originalRequest.getRequester().getFirstName()));
    assertThat(returnedRequest.getRequester().getLastName(), is(originalRequest.getRequester().getLastName()));

    // Same test for batch execution
    var returnedRequests = service.addRequestBatchDetailsForGet(List.of(originalRequest));
    assertThat(returnedRequests.get(0).getItem().getBarcode(), is(originalRequest.getItem().getBarcode()));

    assertThat(returnedRequests.get(0).getRequester().getBarcode(), is(originalRequest.getRequester().getBarcode()));
    assertThat(returnedRequests.get(0).getRequester().getFirstName(), is(originalRequest.getRequester().getFirstName()));
    assertThat(returnedRequests.get(0).getRequester().getLastName(), is(originalRequest.getRequester().getLastName()));
  }

  @SneakyThrows
  @Test
  void shouldHandleDuplicateLocationsInBatchRequestProcessing() {
    // Scenario: Two items reference the same location ID
    String locationId = "b241764c-1466-4e1d-a028-1a3684a5da87";
    String itemId1 = UUID.randomUUID().toString();
    String itemId2 = UUID.randomUUID().toString();
    String instanceId1 = UUID.randomUUID().toString();
    String instanceId2 = UUID.randomUUID().toString();
    String userId1 = UUID.randomUUID().toString();
    String userId2 = UUID.randomUUID().toString();
    String userGroupId = UUID.randomUUID().toString();
    String libraryId = UUID.randomUUID().toString();
    String servicePointId = UUID.randomUUID().toString();

    var request1 = new MediatedRequest()
      .id(UUID.randomUUID().toString())
      .instanceId(instanceId1)
      .itemId(itemId1)
      .requesterId(userId1)
      .pickupServicePointId(servicePointId)
      .requester(new MediatedRequestRequester().barcode("user1"))
      .instance(new MediatedRequestInstance().title("Instance 1"))
      .item(new MediatedRequestItem().barcode("item1"));

    var request2 = new MediatedRequest()
      .id(UUID.randomUUID().toString())
      .instanceId(instanceId2)
      .itemId(itemId2)
      .requesterId(userId2)
      .pickupServicePointId(servicePointId)
      .requester(new MediatedRequestRequester().barcode("user2"))
      .instance(new MediatedRequestInstance().title("Instance 2"))
      .item(new MediatedRequestItem().barcode("item2"));

    var searchItem1 = new SearchItem()
      .id(itemId1)
      .tenantId("tenant1")
      .effectiveLocationId(locationId);

    var searchItem2 = new SearchItem()
      .id(itemId2)
      .tenantId("tenant2")
      .effectiveLocationId(locationId);

    var searchInstance1 = new SearchInstance()
      .id(instanceId1)
      .tenantId("tenant1")
      .items(List.of(searchItem1));

    var searchInstance2 = new SearchInstance()
      .id(instanceId2)
      .tenantId("tenant2")
      .items(List.of(searchItem2));

    var searchResponse = new SearchInstancesResponse()
      .instances(List.of(searchInstance1, searchInstance2));

    when(searchClient.searchInstance(any())).thenReturn(searchResponse);

    doAnswer(invocation -> {
      var supplier = (Supplier<?>) invocation.getArguments()[1];
      return supplier.get();
    }).when(executionService).executeSystemUserScoped(anyString(), any());

    var user1 = new User().id(userId1).barcode("user1").patronGroup(userGroupId);
    var user2 = new User().id(userId2).barcode("user2").patronGroup(userGroupId);
    when(fetchingService.fetchByIds(eq(userClient), any(Collection.class), any(Function.class)))
      .thenReturn(List.of(user1, user2));
    var userGroup = new UserGroup().id(userGroupId).group("Students");
    when(fetchingService.fetchByIds(eq(userGroupClient), any(Collection.class), any(Function.class)))
      .thenReturn(List.of(userGroup));
    var instance1 = new Instance().id(instanceId1).title("Test Instance 1");
    var instance2 = new Instance().id(instanceId2).title("Test Instance 2");
    when(inventoryService.fetchInstance(instanceId1)).thenReturn(instance1);
    when(inventoryService.fetchInstance(instanceId2)).thenReturn(instance2);
    var item1 = new Item().id(itemId1).barcode("item1").effectiveLocationId(locationId);
    var item2 = new Item().id(itemId2).barcode("item2").effectiveLocationId(locationId);
    when(fetchingService.fetchByIds(eq(itemClient), any(Collection.class), any(Function.class)))
      .thenReturn(List.of(item1, item2));

    var dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    var location1 = new Location()
      .id(locationId)
      .name("Popular Reading Collection")
      .code("KU/CC/DI/P")
      .libraryId(libraryId)
      .metadata(new Metadata()
        .createdDate(dateFormat.parse("2025-02-28T12:07:31Z"))
        .updatedDate(dateFormat.parse("2025-03-20T11:18:54Z")));

    var location2 = new Location()
      .id(locationId)
      .name("Popular Reading Collection")
      .code("KU/CC/DI/P")
      .libraryId(libraryId)
      .metadata(new Metadata()
        .createdDate(dateFormat.parse("2025-05-15T15:55:08Z"))
        .updatedDate(dateFormat.parse("2025-05-15T15:55:08Z")));

    when(fetchingService.fetchByIds(eq(locationClient), any(Collection.class), any(Function.class)))
      .thenReturn(List.of(location1))
      .thenReturn(List.of(location2));
    var library = new Library().id(libraryId).name("Main Library");
    when(fetchingService.fetchByIds(eq(libraryClient), any(Collection.class), any(Function.class)))
      .thenReturn(List.of(library));
    var servicePoint = new ServicePoint().id(servicePointId).name("Circulation Desk");
    when(fetchingService.fetchByIds(eq(servicePointClient), any(Collection.class), any(Function.class)))
      .thenReturn(List.of(servicePoint));

    var result = service.addRequestBatchDetailsForGet(List.of(request1, request2));
    assertThat(result, is(notNullValue()));
    assertThat(result, hasSize(2));
    assertThat(result.get(0).getItem(), is(notNullValue()));
    assertThat(result.get(1).getItem(), is(notNullValue()));
    assertThat(result.get(0).getItem().getLocation(), is(notNullValue()));
    assertThat(result.get(1).getItem().getLocation(), is(notNullValue()));
  }
}
