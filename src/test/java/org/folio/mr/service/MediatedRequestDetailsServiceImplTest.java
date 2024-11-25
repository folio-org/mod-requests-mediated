package org.folio.mr.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

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

@ExtendWith(MockitoExtension.class)
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
}
