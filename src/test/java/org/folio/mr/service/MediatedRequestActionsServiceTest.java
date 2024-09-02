package org.folio.mr.service;

import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_FOR_APPROVAL;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_ITEM_ARRIVED;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequest;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequestEntity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.MediatedRequest;
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

  @InjectMocks
  private MediatedRequestActionsServiceImpl mediatedRequestActionsService;

  @Test
  void mediatedRequestIsFoundAndUpdated() {
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

    MediatedRequest result = mediatedRequestActionsService.confirmItemArrival(itemBarcode);

    verify(mediatedRequestsRepository).save(any(MediatedRequestEntity.class));
    assertThat(result.getStatus().getValue(), is("Open - Item arrived"));
    assertThat(result.getMediatedRequestStep(), is("Item arrived"));
  }

  @Test
  void mediatedRequestIsNotFound() {
    when(mediatedRequestsRepository.findRequestForItemArrivalConfirmation(any(String.class)))
      .thenReturn(Optional.empty());
    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
      () -> mediatedRequestActionsService.confirmItemArrival("item-barcode"));
    assertThat(exception.getMessage(),
      is("Mediated request for arrival confirmation of item with barcode 'item-barcode' was not found"));
  }

}