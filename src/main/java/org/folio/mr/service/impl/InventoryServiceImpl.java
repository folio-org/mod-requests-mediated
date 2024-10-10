package org.folio.mr.service.impl;

import java.util.Collection;

import org.folio.mr.client.HoldingClient;
import org.folio.mr.client.InstanceClient;
import org.folio.mr.client.ItemClient;
import org.folio.mr.client.LoanTypeClient;
import org.folio.mr.client.LocationClient;
import org.folio.mr.client.LocationUnitClient;
import org.folio.mr.client.MaterialTypeClient;
import org.folio.mr.client.ServicePointClient;
import org.folio.mr.domain.dto.Campus;
import org.folio.mr.domain.dto.HoldingsRecord;
import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.Institution;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.Library;
import org.folio.mr.domain.dto.LoanType;
import org.folio.mr.domain.dto.Location;
import org.folio.mr.domain.dto.MaterialType;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.service.InventoryService;
import org.folio.mr.support.CqlQuery;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class InventoryServiceImpl implements InventoryService {
  private final ItemClient itemClient;
  private final InstanceClient instanceClient;
  private final HoldingClient holdingClient;
  private final ServicePointClient servicePointClient;
  private final LocationClient locationClient;
  private final LocationUnitClient locationUnitClient;
  private final MaterialTypeClient materialTypeClient;
  private final LoanTypeClient loanTypeClient;

  @Override
  public Item fetchItem(String id) {
    log.info("fetchItem:: fetching item {}", id);
    return itemClient.get(id).orElse(null);
  }

  @Override
  public Collection<Item> fetchItems(CqlQuery query) {
    log.info("fetchItems:: fetching items by query: {}", query);
    return itemClient.get(query).getItems();
  }

  @Override
  public HoldingsRecord fetchHolding(String id) {
    log.info("fetchHolding:: fetching holding {}", id);
    return holdingClient.get(id).orElse(null);
  }

  @Override
  public Instance fetchInstance(String id) {
    log.info("fetchInstance:: fetching instance {}", id);
    return instanceClient.get(id).orElse(null);
  }

  @Override
  public Location fetchLocation(String id) {
    log.info("fetchLocation:: fetching location {}", id);
    return locationClient.get(id).orElse(null);
  }

  @Override
  public Library fetchLibrary(String id) {
    log.info("fetchLibrary:: fetching library {}", id);
    return locationUnitClient.getLibrary(id).orElse(null);
  }

  @Override
  public Campus fetchCampus(String id) {
    log.info("fetchCampus:: fetching campus {}", id);
    return locationUnitClient.getCampus(id).orElse(null);
  }

  @Override
  public Institution fetchInstitution(String id) {
    log.info("fetchInstitution:: fetching institution {}", id);
    return locationUnitClient.getInstitution(id).orElse(null);
  }

  @Override
  public ServicePoint fetchServicePoint(String id) {
    log.info("fetchServicePoint:: fetching library {}", id);
    return servicePointClient.get(id).orElse(null);
  }

  @Override
  public MaterialType fetchMaterialType(String id) {
    log.info("fetchMaterialType:: fetching material type {}", id);
    return materialTypeClient.get(id).orElse(null);
  }

  @Override
  public LoanType fetchLoanType(String id) {
    log.info("fetchLoanType:: fetching loan type {}", id);
    return loanTypeClient.get(id).orElse(null);
  }

}
