package org.folio.mr.service;

import static java.util.UUID.randomUUID;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.NEW_AWAITING_CONFIRMATION;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_FOR_APPROVAL;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_ITEM_ARRIVED;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequest;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequestEntity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.folio.mr.client.SearchClient;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.SearchInstance;
import org.folio.mr.domain.dto.SearchInstancesResponse;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.domain.mapper.MediatedRequestMapper;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.impl.MediatedRequestActionsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
  private SearchClient searchClient;

  @Mock
  private CirculationRequestService circulationRequestService;

  @Mock
  private EcsRequestService ecsRequestService;

  @InjectMocks
  private MediatedRequestActionsServiceImpl mediatedRequestActionsService;

  @Test
  void successfulItemArrivalConfirmation() {
    UUID mediatedRequestId = randomUUID();
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
//    when(inventoryService.fetchItem(initialRequest.getItemId().toString()))
//      .thenReturn(new Item());
    when(mediatedRequestMapper.mapEntityToDto(any(MediatedRequestEntity.class)))
      .thenReturn(mappedRequest);

    MediatedRequest result = mediatedRequestActionsService.confirmItemArrival(itemBarcode);

    verify(mediatedRequestsRepository).save(any(MediatedRequestEntity.class));
    assertThat(result.getStatus().getValue(), is("Open - Item arrived"));
    assertThat(result.getMediatedRequestStep(), is("Item arrived"));
  }

  @Test
  void mediatedRequestIsNotFoundDuringItemArrivalConfirmation() {
    when(mediatedRequestsRepository.findRequestForItemArrivalConfirmation(any(String.class)))
      .thenReturn(Optional.empty());
    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
      () -> mediatedRequestActionsService.confirmItemArrival("item-barcode"));
    assertThat(exception.getMessage(),
      is("Mediated request for arrival confirmation of item with barcode 'item-barcode' was not found"));
  }

  @Test
  void successfulMediatedRequestConfirmationForNonSecureTenant() {

  }

  @Test
  void successfulMediatedRequestConfirmationForInstanceInSecureTenant() {
    final String secureTenantId = "secure-tenant";
    final UUID mediatedRequestId = randomUUID();
    final UUID instanceId = randomUUID();

    MediatedRequestEntity requestEntity = buildMediatedRequestEntity(NEW_AWAITING_CONFIRMATION)
      .withId(mediatedRequestId)
      .withInstanceId(instanceId);

    SearchInstancesResponse searchInstancesResponse = new SearchInstancesResponse()
      .instances(List.of(new SearchInstance()
        .id(instanceId.toString())
        .tenantId(secureTenantId)));

    when(mediatedRequestsRepository.findById(mediatedRequestId))
      .thenReturn(Optional.of(requestEntity));

//    when(searchClient.findInstance(instanceId.toString()))
//      .thenReturn(searchInstancesResponse);

    doNothing().when(circulationRequestService).create(requestEntity);

    mediatedRequestActionsService.confirm(mediatedRequestId);

    verify(circulationRequestService).create(requestEntity);
  }

}