package org.folio.mr.service;

import org.folio.mr.domain.dto.Campus;
import org.folio.mr.domain.dto.HoldingsRecord;
import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.Institution;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.ItemStatus;
import org.folio.mr.domain.dto.Library;
import org.folio.mr.domain.dto.LoanType;
import org.folio.mr.domain.dto.Location;
import org.folio.mr.domain.dto.MaterialType;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.service.impl.StaffSlipContextService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_FOR_APPROVAL;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffSlipContextServiceTest {

  @Mock
  private InventoryService inventoryService;

  @InjectMocks
  private StaffSlipContextService staffSlipContextService;

  @Test
  void testCreateStaffSlipContext() {
    // given
    String itemId = "itemId";
    String holdingId = "holdingId";
    String instanceId = "instanceId";
    String materialTypeId = "materialTypeId";
    String loanTypeId = "loanTypeId";
    String inTransitServicePointId = "inTransitServicePointId";

    String locationId = "locationId";
    String libraryId = "libraryId";
    String campusId = "campusId";
    String institutionId = "institutionId";
    String primaryServicePointId = UUID.randomUUID().toString();

    Item item = new Item()
      .holdingsRecordId(holdingId)
      .materialTypeId(materialTypeId)
      .permanentLoanTypeId(loanTypeId)
      .inTransitDestinationServicePointId(inTransitServicePointId)
      .effectiveLocationId(locationId)
      .status(new ItemStatus().name(ItemStatus.NameEnum.AVAILABLE))
      .yearCaption(Set.of());
    when(inventoryService.fetchItem(itemId))
      .thenReturn(item
      );
    when(inventoryService.fetchHolding(holdingId))
      .thenReturn(new HoldingsRecord().instanceId(instanceId));
    when(inventoryService.fetchInstance(instanceId))
      .thenReturn(new Instance().contributors(List.of()));
    when(inventoryService.fetchMaterialType(materialTypeId))
      .thenReturn(new MaterialType());
    when(inventoryService.fetchLoanType(loanTypeId))
      .thenReturn(new LoanType());
    when(inventoryService.fetchServicePoint(inTransitServicePointId))
      .thenReturn(new ServicePoint());

    when(inventoryService.fetchLocation(locationId))
      .thenReturn(new Location()
        .libraryId(libraryId)
        .campusId(campusId)
        .institutionId(institutionId)
        .primaryServicePoint(UUID.fromString(primaryServicePointId))
      );
    when(inventoryService.fetchLibrary(libraryId))
      .thenReturn(new Library());
    when(inventoryService.fetchCampus(campusId))
      .thenReturn(new Campus());
    when(inventoryService.fetchInstitution(institutionId))
      .thenReturn(new Institution());
    when(inventoryService.fetchServicePoint(primaryServicePointId))
      .thenReturn(new ServicePoint());

    var request = buildMediatedRequest(OPEN_IN_TRANSIT_FOR_APPROVAL).itemId(itemId);

    // when
    var result = staffSlipContextService.createStaffSlipContext(request);

    // then
    assertEquals("Available", result.getItem().getStatus());

    verify(inventoryService).fetchItem(itemId);
    verify(inventoryService).fetchHolding(holdingId);
    verify(inventoryService).fetchInstance(instanceId);
    verify(inventoryService).fetchServicePoint(inTransitServicePointId);

    verify(inventoryService).fetchLocation(locationId);
    verify(inventoryService).fetchLibrary(libraryId);
    verify(inventoryService).fetchCampus(campusId);
    verify(inventoryService).fetchInstitution(institutionId);
    verify(inventoryService).fetchServicePoint(primaryServicePointId);

    verifyNoMoreInteractions(inventoryService);
  }

}
