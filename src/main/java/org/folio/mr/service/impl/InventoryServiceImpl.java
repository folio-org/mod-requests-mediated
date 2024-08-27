package org.folio.mr.service.impl;

import org.folio.mr.client.InstanceClient;
import org.folio.mr.client.ItemClient;
import org.folio.mr.client.LocationClient;
import org.folio.mr.client.LocationUnitClient;
import org.folio.mr.client.ServicePointClient;
import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.Library;
import org.folio.mr.domain.dto.Location;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.service.InventoryService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class InventoryServiceImpl implements InventoryService {
  private final ItemClient itemClient;
  private final InstanceClient instanceClient;
  private final ServicePointClient servicePointClient;
  private final LocationClient locationClient;
  private final LocationUnitClient locationUnitClient;

  @Override
  public Item fetchItem(String id) {
    log.info("fetchItem:: fetching item {}", id);
    return itemClient.get(id);
  }

  @Override
  public Instance fetchInstance(String id) {
    log.info("fetchInstance:: fetching instance {}", id);
    return instanceClient.get(id);
  }

  @Override
  public Location fetchLocation(String id) {
    log.info("fetchLocation:: fetching location {}", id);
    return locationClient.get(id);
  }

  @Override
  public Library fetchLibrary(String id) {
    log.info("fetchLibrary:: fetching library {}", id);
    return locationUnitClient.getLibrary(id);
  }

  @Override
  public ServicePoint fetchServicePoint(String id) {
    log.info("fetchServicePoint:: fetching library {}", id);
    return servicePointClient.get(id);
  }
}
