package org.folio.mr.service.impl;

import java.util.Map;
import java.util.Set;

import org.folio.mr.client.BulkFetcher;
import org.folio.mr.client.InstanceClient;
import org.folio.mr.client.ItemClient;
import org.folio.mr.client.LocationClient;
import org.folio.mr.client.LibraryClient;
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
  private final LibraryClient libraryClient;
  private final BulkFetcher bulkFetcher;

  @Override
  public Item fetchItem(String id) {
    log.info("fetchItem:: fetching item {}", id);
    return itemClient.get(id);
  }

  @Override
  public Map<String, Item> fetchItems(Set<String> ids) {
    log.info("fetchItems:: fetching {} items by IDs", ids::size);
    return bulkFetcher.getMapped(itemClient, ids, Item::getId);
  }

  @Override
  public Instance fetchInstance(String id) {
    log.info("fetchInstance:: fetching instance {}", id);
    return instanceClient.get(id);
  }

  @Override
  public Map<String, Instance> fetchInstances(Set<String> ids) {
    log.info("fetchInstances:: fetching {} instances by IDs", ids::size);
    return bulkFetcher.getMapped(instanceClient, ids, Instance::getId);
  }

  @Override
  public Location fetchLocation(String id) {
    log.info("fetchLocation:: fetching location {}", id);
    return locationClient.get(id);
  }

  @Override
  public Map<String, Location> fetchLocations(Set<String> ids) {
    log.info("fetchLocations:: fetching {} locations by IDs", ids::size);
    return bulkFetcher.getMapped(locationClient, ids, Location::getId);
  }

  @Override
  public Library fetchLibrary(String id) {
    log.info("fetchLibrary:: fetching library {}", id);
    return libraryClient.getLibrary(id);
  }

  @Override
  public Map<String, Library> fetchLibraries(Set<String> ids) {
    log.info("fetchLibraries:: fetching {} libraries by IDs", ids::size);
    return bulkFetcher.getMapped(libraryClient, ids, Library::getId);
  }

  @Override
  public ServicePoint fetchServicePoint(String id) {
    log.info("fetchServicePoint:: fetching library {}", id);
    return servicePointClient.get(id);
  }

  @Override
  public Map<String, ServicePoint> fetchServicePoints(Set<String> ids) {
    log.info("fetchServicePoints:: fetching {} service points by IDs", ids::size);
    return bulkFetcher.getMapped(servicePointClient, ids, ServicePoint::getId);
  }
}