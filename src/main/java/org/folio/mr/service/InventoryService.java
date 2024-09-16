package org.folio.mr.service;

import java.util.Collection;

import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.Library;
import org.folio.mr.domain.dto.Location;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.support.CqlQuery;

public interface InventoryService {
  Item fetchItem(String id);
  Collection<Item> fetchItems(CqlQuery query);
  Instance fetchInstance(String id);
  Location fetchLocation(String id);
  Library fetchLibrary(String id);
  ServicePoint fetchServicePoint(String id);
}
