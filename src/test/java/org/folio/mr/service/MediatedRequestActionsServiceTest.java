package org.folio.mr.service;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.NEW_AWAITING_CONFIRMATION;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_FOR_APPROVAL;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_ITEM_ARRIVED;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequest;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequestEntity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.EcsTlr;
import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.domain.mapper.MediatedRequestMapper;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.impl.MediatedRequestActionsServiceImpl;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class MediatedRequestActionsServiceTest {

  @Mock
  private MediatedRequestsRepository mediatedRequestsRepository;

  @Mock
  private MediatedRequestMapper mediatedRequestMapper;

  @Mock
  private InventoryService inventoryService;

  @Mock
  private CirculationRequestService circulationRequestService;

  @Mock
  private EcsRequestService ecsRequestService;

  @Mock
  private FolioExecutionContext folioExecutionContext;

  @Mock
  private SearchService searchService;

  @InjectMocks
  private MediatedRequestActionsServiceImpl mediatedRequestActionsService;

  @Test
  void successfulItemArrivalConfirmation() {
    // given
    UUID mediatedRequestId = UUID.randomUUID();
    MediatedRequestEntity initialRequest = buildMediatedRequestEntity(OPEN_IN_TRANSIT_FOR_APPROVAL)
      .withId(mediatedRequestId);
    MediatedRequestEntity updatedRequest = buildMediatedRequestEntity(OPEN_ITEM_ARRIVED)
      .withId(mediatedRequestId);
    MediatedRequest mappedRequest = buildMediatedRequest(OPEN_ITEM_ARRIVED);
    String itemBarcode = initialRequest.getItemBarcode();

    when(mediatedRequestsRepository.findRequestForItemArrivalConfirmation(itemBarcode))
      .thenReturn(Optional.of(initialRequest));
    when(mediatedRequestsRepository.save(any(MediatedRequestEntity.class)))
      .thenReturn(updatedRequest);
    when(inventoryService.fetchItem(initialRequest.getItemId().toString()))
      .thenReturn(new Item());
    when(mediatedRequestMapper.mapEntityToDto(any(MediatedRequestEntity.class)))
      .thenReturn(mappedRequest);

    // when
    MediatedRequest result = mediatedRequestActionsService.confirmItemArrival(itemBarcode);

    // then
    verify(mediatedRequestsRepository).save(any(MediatedRequestEntity.class));
    assertThat(result.getStatus().getValue(), is("Open - Item arrived"));
    assertThat(result.getMediatedRequestStep(), is("Item arrived"));
  }

  @Test
  void confirmItemArrivalRequestNotFound() {
    // given
    when(mediatedRequestsRepository.findRequestForItemArrivalConfirmation(any(String.class)))
      .thenReturn(Optional.empty());

    // when-then
    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
      () -> mediatedRequestActionsService.confirmItemArrival("item-barcode"));
    assertThat(exception.getMessage(),
      is("Mediated request for arrival confirmation of item with barcode 'item-barcode' was not found"));
  }

  @Test
  void sendItemInTransitSuccess() {
    // given
    UUID mediatedRequestId = UUID.randomUUID();
    MediatedRequestEntity initialRequest = buildMediatedRequestEntity(OPEN_ITEM_ARRIVED)
      .withId(mediatedRequestId);
    MediatedRequestEntity updatedRequest = buildMediatedRequestEntity(OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT)
      .withId(mediatedRequestId);
    MediatedRequest mappedRequest = buildMediatedRequest(OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT);
    String itemBarcode = initialRequest.getItemBarcode();

    when(mediatedRequestsRepository.findRequestForSendingInTransit(itemBarcode))
      .thenReturn(Optional.of(initialRequest));
    when(mediatedRequestsRepository.save(any(MediatedRequestEntity.class)))
      .thenReturn(updatedRequest);
    when(inventoryService.fetchItem(initialRequest.getItemId().toString()))
      .thenReturn(new Item());
    when(mediatedRequestMapper.mapEntityToDto(any(MediatedRequestEntity.class)))
      .thenReturn(mappedRequest);

    // when
    MediatedRequest result = mediatedRequestActionsService.sendItemInTransit(itemBarcode);

    // then
    verify(mediatedRequestsRepository).save(any(MediatedRequestEntity.class));
    assertThat(result.getStatus().getValue(), is("Open - In transit to be checked out"));
    assertThat(result.getMediatedRequestStep(), is("In transit to be checked out"));
  }

  @Test
  void sendItemInTransitRequestNotFound() {
    // given
    when(mediatedRequestsRepository.findRequestForSendingInTransit(any(String.class)))
      .thenReturn(Optional.empty());

    // when-then
    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
      () -> mediatedRequestActionsService.sendItemInTransit("item-barcode"));
    assertThat(exception.getMessage(),
      is("Mediated request for in transit sending of item with barcode 'item-barcode' was not found"));
  }

  @Test
  void mediatedRequestConfirmationForLocalInstanceAndItem() {
    final UUID mediatedRequestId = randomUUID();
    final UUID instanceId = randomUUID();
    final UUID circulationRequestId = randomUUID();

    MediatedRequestEntity mediatedRequest = buildMediatedRequestEntity(NEW_AWAITING_CONFIRMATION)
      .withId(mediatedRequestId)
      .withInstanceId(instanceId);

    ConsortiumItem requestedItem = new ConsortiumItem().id(mediatedRequest.getItemId().toString());
    Request circulationRequest = new Request().id(circulationRequestId.toString());

    when(mediatedRequestsRepository.findById(mediatedRequestId))
      .thenReturn(Optional.of(mediatedRequest));
    when(inventoryService.fetchInstance(instanceId.toString()))
      .thenReturn(new Instance());
    when(folioExecutionContext.getTenantId())
      .thenReturn("consortium");
    when(searchService.searchItems(instanceId.toString(), "consortium"))
      .thenReturn(List.of(requestedItem));
    when(circulationRequestService.create(mediatedRequest))
      .thenReturn(circulationRequest);

    mediatedRequestActionsService.confirm(mediatedRequestId);

    verify(mediatedRequestsRepository).save(mediatedRequest.withConfirmedRequestId(circulationRequestId));
    verifyNoInteractions(ecsRequestService);
  }

  @Test
  void mediatedRequestConfirmationForLocalInstanceAndRemoteItem() {
    final UUID mediatedRequestId = randomUUID();
    final UUID instanceId = randomUUID();
    final UUID ecsTlrId = randomUUID();
    final UUID primaryRequestId = randomUUID();

    MediatedRequestEntity mediatedRequest = buildMediatedRequestEntity(NEW_AWAITING_CONFIRMATION)
      .withId(mediatedRequestId)
      .withInstanceId(instanceId);

    EcsTlr ecsTlr = new EcsTlr()
      .id(ecsTlrId.toString())
      .primaryRequestId(primaryRequestId.toString());

    when(mediatedRequestsRepository.findById(mediatedRequestId))
      .thenReturn(Optional.of(mediatedRequest));
    when(inventoryService.fetchInstance(instanceId.toString()))
      .thenReturn(new Instance());
    when(folioExecutionContext.getTenantId())
      .thenReturn("consortium");
    when(searchService.searchItems(instanceId.toString(), "consortium"))
      .thenReturn(emptyList());
    when(ecsRequestService.create(mediatedRequest))
      .thenReturn(ecsTlr);

    mediatedRequestActionsService.confirm(mediatedRequestId);

    verify(mediatedRequestsRepository).save(mediatedRequest.withConfirmedRequestId(primaryRequestId));
    verifyNoInteractions(circulationRequestService);
  }

  @Test
  void mediatedRequestConfirmationForRemoteInstanceAndItem() {
    final UUID mediatedRequestId = randomUUID();
    final UUID instanceId = randomUUID();
    final UUID ecsTlrId = randomUUID();
    final UUID primaryRequestId = randomUUID();

    MediatedRequestEntity mediatedRequest = buildMediatedRequestEntity(NEW_AWAITING_CONFIRMATION)
      .withId(mediatedRequestId)
      .withInstanceId(instanceId);

    EcsTlr ecsTlr = new EcsTlr()
      .id(ecsTlrId.toString())
      .primaryRequestId(primaryRequestId.toString());

    when(mediatedRequestsRepository.findById(mediatedRequestId))
      .thenReturn(Optional.of(mediatedRequest));
    when(inventoryService.fetchInstance(instanceId.toString()))
      .thenThrow(FeignException.NotFound.class);
    when(ecsRequestService.create(mediatedRequest))
      .thenReturn(ecsTlr);

    mediatedRequestActionsService.confirm(mediatedRequestId);

    verify(mediatedRequestsRepository).save(mediatedRequest.withConfirmedRequestId(primaryRequestId));
    verifyNoInteractions(circulationRequestService);
    verify(inventoryService).fetchInstance(instanceId.toString());
    verifyNoMoreInteractions(inventoryService);
  }

}
