package org.folio.mr.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.folio.mr.client.SearchClient;
import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequestInstance;
import org.folio.mr.domain.dto.MediatedRequestItem;
import org.folio.mr.domain.dto.MediatedRequestRequester;
import org.folio.mr.domain.dto.SearchInstance;
import org.folio.mr.domain.dto.SearchInstancesResponse;
import org.folio.mr.service.impl.MediatedRequestDetailsServiceImpl;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
  }
}
