package org.folio.mr.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.InstanceContributorsInner;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.SendItemInTransitResponseStaffSlipContext;
import org.folio.mr.domain.dto.SendItemInTransitResponseStaffSlipContextItem;
import org.folio.mr.service.InventoryService;
import org.folio.mr.support.DateFormatUtil;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class StaffSlipContextService {

  private final InventoryService inventoryService;

  public SendItemInTransitResponseStaffSlipContext createStaffSlipContext(MediatedRequest request) {
    Item item = inventoryService.fetchItem(request.getItemId());

    log.debug("createStaffSlipContext:: parameters item: {}", item);

    var holding = inventoryService.fetchHolding(item.getHoldingsRecordId());
    var instance = inventoryService.fetchInstance(holding.getInstanceId());
    var materialType = inventoryService.fetchMaterialType(item.getMaterialTypeId());
    var loanType = inventoryService.fetchLoanType(item.getPermanentLoanTypeId());
    var toServicePoint = inventoryService.fetchServicePoint(item.getInTransitDestinationServicePointId());

    var staffSlipContextItem = new SendItemInTransitResponseStaffSlipContextItem()
      .title(instance.getTitle())
      .primaryContributor(getPrimaryContributorName(instance))
      .allContributors(getAllContributorNames(instance))
      .barcode(item.getBarcode())
      .status(item.getStatus().getName().getValue())
      .enumeration(item.getEnumeration())
      .volume(item.getVolume())
      .chronology(item.getChronology())
      .copy(Objects.requireNonNullElse(item.getCopyNumber(), ""))
      .displaySummary(item.getDisplaySummary())
      .yearCaption(String.join("; ", item.getYearCaption()))
      .materialType(materialType.getName())
      .loanType(loanType.getName())
      .numberOfPieces(item.getNumberOfPieces())
      .descriptionOfPieces(item.getDescriptionOfPieces())
      .toServicePoint(toServicePoint.getName());

    if (item.getLastCheckIn() != null) {
      var fromServicePoint = inventoryService.fetchServicePoint(
        item.getLastCheckIn().getServicePointId());
      staffSlipContextItem
        .fromServicePoint(fromServicePoint.getName())
        .lastCheckedInDateTime(DateFormatUtil.formatUtcDate(item.getLastCheckIn().getDateTime()));
    }

    var location = inventoryService.fetchLocation(item.getEffectiveLocationId());
    if (location != null) {
      var library = inventoryService.fetchLibrary(location.getLibraryId());
      var campus = inventoryService.fetchCampus(location.getCampusId());
      var institution = inventoryService.fetchInstitution(location.getInstitutionId());

      staffSlipContextItem
        .effectiveLocationSpecific(location.getName())
        .effectiveLocationLibrary(library.getName())
        .effectiveLocationCampus(campus.getName())
        .effectiveLocationInstitution(institution.getName())
        .effectiveLocationDiscoveryDisplayName(location.getDiscoveryDisplayName());

      var primaryServicePoint = inventoryService.fetchServicePoint(location.getPrimaryServicePoint().toString());
      if (primaryServicePoint != null) {
        log.info("createStaffSlipContext:: primaryServicePoint is not null");
        staffSlipContextItem.effectiveLocationPrimaryServicePointName(
          primaryServicePoint.getName());
      }
    }

    var effectiveCallNumberComponents = item.getEffectiveCallNumberComponents();
    if (effectiveCallNumberComponents != null) {
      log.info("createStaffSlipContext:: effectiveCallNumberComponents is not null");
      staffSlipContextItem
        .callNumber(effectiveCallNumberComponents.getCallNumber())
        .callNumberPrefix(effectiveCallNumberComponents.getPrefix())
        .callNumberSuffix(effectiveCallNumberComponents.getSuffix());
    }

    log.info("createStaffSlipContext:: staffSlipContextItem: {}", staffSlipContextItem);
    return new SendItemInTransitResponseStaffSlipContext().item(staffSlipContextItem);
  }

  private static String getPrimaryContributorName(Instance instance) {
    return instance.getContributors().stream()
      .filter(InstanceContributorsInner::getPrimary)
      .findFirst()
      .map(InstanceContributorsInner::getName)
      .orElse(null);
  }

  private static String getAllContributorNames(Instance instance) {
    return instance.getContributors()
      .stream()
      .map(InstanceContributorsInner::getName)
      .collect(Collectors.joining("; "));
  }

}
