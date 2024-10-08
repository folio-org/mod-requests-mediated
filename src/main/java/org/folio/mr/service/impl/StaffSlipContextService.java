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
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class StaffSlipContextService {

  private final InventoryService inventoryService;

  public SendItemInTransitResponseStaffSlipContext createStaffSlipContext(MediatedRequest request) {
    log.debug("createStaffSlipContext:: parameters request: {}", request);

    var staffSlipContextItem = new SendItemInTransitResponseStaffSlipContextItem();

    Item item = inventoryService.fetchItem(request.getItemId());
    if (item != null) {
      staffSlipContextItem
        .barcode(item.getBarcode())
        .status(item.getStatus().getName().getValue())
        .enumeration(item.getEnumeration())
        .volume(item.getVolume())
        .chronology(item.getChronology())
        .copy(Objects.requireNonNullElse(item.getCopyNumber(), ""))
        .displaySummary(item.getDisplaySummary())
        .yearCaption(String.join("; ", item.getYearCaption()))
        .numberOfPieces(item.getNumberOfPieces())
        .descriptionOfPieces(item.getDescriptionOfPieces());

      if (item.getHoldingsRecordId() != null) {
        var holding = inventoryService.fetchHolding(item.getHoldingsRecordId());
        if (holding != null) {
          if (holding.getInstanceId() != null) {
            var instance = inventoryService.fetchInstance(holding.getInstanceId());
            if (instance != null) {
              staffSlipContextItem
                .title(instance.getTitle())
                .primaryContributor(getPrimaryContributorName(instance))
                .allContributors(getAllContributorNames(instance));
            }
          }
        }
      }

      if (item.getMaterialTypeId() != null) {
        var materialType = inventoryService.fetchMaterialType(item.getMaterialTypeId());
        if (materialType != null) {
          staffSlipContextItem.materialType(materialType.getName());
        }
      }

      if (item.getPermanentLoanTypeId() != null) {
        var loanType = inventoryService.fetchLoanType(item.getPermanentLoanTypeId());
        if (loanType != null) {
          staffSlipContextItem.loanType(loanType.getName());
        }
      }

      if (item.getInTransitDestinationServicePointId() != null) {
        var toServicePoint = inventoryService.fetchServicePoint(
          item.getInTransitDestinationServicePointId());
        if (toServicePoint != null) {
          staffSlipContextItem.toServicePoint(toServicePoint.getName());
        }
      }

      if (item.getLastCheckIn() != null) {
        staffSlipContextItem.lastCheckedInDateTime(item.getLastCheckIn().getDateTime());

        var fromServicePoint = inventoryService.fetchServicePoint(
          item.getLastCheckIn().getServicePointId());
        if (fromServicePoint != null) {
          staffSlipContextItem.fromServicePoint(fromServicePoint.getName());
        }
      }

      if (item.getEffectiveLocationId() != null) {
        var location = inventoryService.fetchLocation(item.getEffectiveLocationId());
        if (location != null) {
          staffSlipContextItem
            .effectiveLocationSpecific(location.getName())
            .effectiveLocationDiscoveryDisplayName(location.getDiscoveryDisplayName());

          if (location.getLibraryId() != null) {
            var library = inventoryService.fetchLibrary(location.getLibraryId());
            if (library != null) {
              staffSlipContextItem.effectiveLocationLibrary(library.getName());
            }
          }

          if (location.getCampusId() != null) {
            var campus = inventoryService.fetchCampus(location.getCampusId());
            if (campus != null) {
              staffSlipContextItem.effectiveLocationCampus(campus.getName());
            }
          }

          if (location.getInstitutionId() != null) {
            var institution = inventoryService.fetchInstitution(location.getInstitutionId());
            if (institution != null) {
              staffSlipContextItem.effectiveLocationInstitution(institution.getName());
            }
          }

          if (location.getPrimaryServicePoint() != null) {
            var primaryServicePoint = inventoryService.fetchServicePoint(
              location.getPrimaryServicePoint().toString());
            if (primaryServicePoint != null) {
              staffSlipContextItem.effectiveLocationPrimaryServicePointName(
                primaryServicePoint.getName());
            }
          }
        }
      }

      if (item.getEffectiveCallNumberComponents() != null) {
        var effectiveCallNumberComponents = item.getEffectiveCallNumberComponents();
        if (effectiveCallNumberComponents != null) {
          staffSlipContextItem
            .callNumber(effectiveCallNumberComponents.getCallNumber())
            .callNumberPrefix(effectiveCallNumberComponents.getPrefix())
            .callNumberSuffix(effectiveCallNumberComponents.getSuffix());
        }
      }
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
