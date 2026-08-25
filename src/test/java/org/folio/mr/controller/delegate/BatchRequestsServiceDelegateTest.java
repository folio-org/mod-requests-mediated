package org.folio.mr.controller.delegate;

import static org.folio.mr.controller.delegate.BatchRequestsServiceDelegate.BATCH_REQUEST_ITEMS_VALIDATION_SETTING_FETCH_QUERY;
import static org.folio.mr.controller.delegate.BatchRequestsServiceDelegate.SETTING_KEY;
import static org.folio.mr.controller.delegate.BatchRequestsServiceDelegate.SETTING_SCOPE;
import static org.folio.mr.domain.type.ErrorCode.BATCH_REQUEST_ITEM_IDS_COUNT_EXCEEDS_MAX_LIMIT;
import static org.folio.mr.domain.type.ErrorCode.DUPLICATE_BATCH_REQUEST_ITEM_IDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.flow.api.Flow;
import org.folio.flow.api.FlowEngine;
import org.folio.mr.client.SettingsClient;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDtoItemRequestsInner;
import org.folio.mr.domain.entity.projection.BatchRequestStatsImpl;
import org.folio.mr.exception.MediatedBatchRequestValidationException;
import org.folio.mr.service.MediatedBatchRequestFlowProvider;
import org.folio.mr.service.MediatedBatchRequestSplitService;
import org.folio.mr.service.MediatedBatchRequestsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BatchRequestsServiceDelegateTest {

  @InjectMocks private BatchRequestsServiceDelegate delegate;
  @Mock private MediatedBatchRequestsService batchRequestsService;
  @Mock private MediatedBatchRequestSplitService requestSplitService;
  @Mock private FlowEngine flowEngine;
  @Mock private MediatedBatchRequestFlowProvider flowProvider;
  @Mock private SettingsClient settingsClient;

  @Test
  void retrieveBatchRequestsCollection_positive_shouldReturnDtoCollection() {
    var query = "test";
    var offset = 0;
    var limit = 10;
    var batchRequestDto = new MediatedBatchRequestDto().batchId(UUID.randomUUID().toString());
    var entitiesPage = new PageImpl<>(List.of(batchRequestDto));
    when(batchRequestsService.getAll(query, offset, limit)).thenReturn(entitiesPage);

    var result = delegate.retrieveBatchRequestsCollection(query, offset, limit);

    assertEquals(1, result.getTotalRecords());
    assertEquals(batchRequestDto, result.getMediatedBatchRequests().get(0));
  }

  @Test
  void createBatchRequest_positive_shouldCreateAndReturnDto() {
    var postDto = new MediatedBatchRequestPostDto()
      .itemRequests(List.of(itemRequest(UUID.randomUUID())));
    var createdBatchRequest = new MediatedBatchRequestDto().batchId(UUID.randomUUID().toString());
    var flow = mock(Flow.class);
    when(batchRequestsService.create(postDto)).thenReturn(createdBatchRequest);
    when(flowProvider.createFlow(any())).thenReturn(flow);
    when(settingsClient.getSettingsEntries(BATCH_REQUEST_ITEMS_VALIDATION_SETTING_FETCH_QUERY, 1))
      .thenReturn(new SettingsClient.SettingsEntries(List.of(), null));
    ReflectionTestUtils.setField(delegate, "batchRequestMaxAllowedItemsCount", 100);

    var result = delegate.createBatchRequest(postDto);

    assertEquals(createdBatchRequest, result);
    verify(flowProvider).createFlow(UUID.fromString(createdBatchRequest.getBatchId()));
    verify(flowEngine).executeAsync(flow);
  }

  @Test
  void createBatchRequest_negative_shouldFailWithDuplicateItemsError() {
    var duplicateItemId = UUID.randomUUID();
    var postDto = new MediatedBatchRequestPostDto()
      .itemRequests(List.of(itemRequest(duplicateItemId), itemRequest(duplicateItemId)));

    var ex = assertThrows(MediatedBatchRequestValidationException.class, () -> delegate.createBatchRequest(postDto));
    assertTrue(ex.getMessage().contains(DUPLICATE_BATCH_REQUEST_ITEM_IDS.getMessage()));
  }

  @Test
  void createBatchRequest_negative_shouldFailWithItemsCountExceedLimitError() {
    var postDto = new MediatedBatchRequestPostDto()
      .itemRequests(List.of(itemRequest(UUID.randomUUID()), itemRequest(UUID.randomUUID())));
    var itemsLimitSetting = new SettingsClient.SettingEntry(UUID.randomUUID(), SETTING_SCOPE, SETTING_KEY,
      new SettingsClient.BatchRequestItemsValidationValue(1));
    when(settingsClient.getSettingsEntries(BATCH_REQUEST_ITEMS_VALIDATION_SETTING_FETCH_QUERY, 1))
      .thenReturn(new SettingsClient.SettingsEntries(List.of(itemsLimitSetting), null));

    var ex = assertThrows(MediatedBatchRequestValidationException.class, () -> delegate.createBatchRequest(postDto));
    assertTrue(ex.getMessage().contains(BATCH_REQUEST_ITEM_IDS_COUNT_EXCEEDS_MAX_LIMIT.getMessage()));
  }

  @Test
  void getBatchRequestById_positive_shouldReturnDto() {
    var id = UUID.randomUUID();
    var dto = new MediatedBatchRequestDto().batchId(id.toString());
    var stats = new BatchRequestStatsImpl();
    stats.setTotal(10);
    stats.setPending(2);
    stats.setInProgress(1);
    stats.setCompleted(5);
    stats.setFailed(5);
    when(batchRequestsService.getById(id)).thenReturn(dto);
    when(requestSplitService.getBatchRequestStats(id)).thenReturn(stats);

    var result = delegate.getBatchRequestById(id);

    assertEquals(10, result.getItemRequestsStats().getTotal());
    assertEquals(2, result.getItemRequestsStats().getPending());
    assertEquals(1, result.getItemRequestsStats().getInProgress());
    assertEquals(5, result.getItemRequestsStats().getCompleted());
    assertEquals(5, result.getItemRequestsStats().getFailed());
  }

  @Test
  void getBatchRequestDetailsByBatchId_positive_shouldReturnDetailsDto() {
    var batchId = UUID.randomUUID();
    var offset = 0;
    var limit = 10;
    var detailDto = new MediatedBatchRequestDetailDto().batchId(batchId.toString());
    var batchSplitEntities = new PageImpl<>(List.of(detailDto));
    when(requestSplitService.getPageByBatchId(batchId, offset, limit)).thenReturn(batchSplitEntities);

    var result = delegate.getBatchRequestDetailsByBatchId(batchId, offset, limit);

    assertEquals(1, result.getTotalRecords());
    assertEquals(detailDto, result.getMediatedBatchRequestDetails().get(0));
  }

  @Test
  void getBatchRequestDetailsByQuery_positive_shouldReturnDetailsDto() {
    var query = "query";
    var offset = 0;
    var limit = 10;
    var detailDto = new MediatedBatchRequestDetailDto().batchId(UUID.randomUUID().toString());
    var batchSplitEntities = new PageImpl<>(List.of(detailDto));
    when(requestSplitService.getAll(query, offset, limit)).thenReturn(batchSplitEntities);

    var result = delegate.retrieveBatchRequestDetailsCollection(query, offset, limit);

    assertEquals(1, result.getTotalRecords());
    assertEquals(detailDto, result.getMediatedBatchRequestDetails().get(0));
  }

  @Test
  void recoverStaleBatchRequests_positive() {
    var batchId = UUID.randomUUID();
    var flow = mock(Flow.class);
    var staleRequest = Mockito.mock(MediatedBatchRequestDto.class);
    when(staleRequest.getBatchId()).thenReturn(batchId.toString());
    when(batchRequestsService.getStaleBatchRequests()).thenReturn(List.of(staleRequest));
    when(flowProvider.createFlow(batchId)).thenReturn(flow);

    delegate.recoverStaleBatchRequests();

    verify(flowEngine).executeAsync(flow);
  }

  private static MediatedBatchRequestPostDtoItemRequestsInner itemRequest(UUID itemId) {
    return new MediatedBatchRequestPostDtoItemRequestsInner()
      .itemId(itemId.toString())
      .pickupServicePointId(UUID.randomUUID().toString());
  }
}
