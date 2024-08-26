package org.folio.mr.service;

import java.util.Map;
import java.util.Set;

import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.Library;
import org.folio.mr.domain.dto.Location;
import org.folio.mr.domain.dto.ServicePoint;

public interface InventoryService {
  Item fetchItem(String id);
  Map<String, Item> fetchItems(Set<String> ids);
  Instance fetchInstance(String id);
  Map<String, Instance> fetchInstances(Set<String> ids);
  Location fetchLocation(String id);
  Map<String, Location> fetchLocations(Set<String> ids);
  Library fetchLibrary(String id);
  Map<String, Library> fetchLibraries(Set<String> ids);
  ServicePoint fetchServicePoint(String id);
  Map<String, ServicePoint> fetchServicePoints(Set<String> ids);
}
