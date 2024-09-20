package org.folio.mr.service;

import java.util.Collection;

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
import org.folio.mr.support.CqlQuery;

public interface InventoryService {
  Item fetchItem(String id);
  Collection<Item> fetchItems(CqlQuery query);
  HoldingsRecord fetchHolding(String id);
  Instance fetchInstance(String id);
  Location fetchLocation(String id);
  Library fetchLibrary(String id);
  Campus fetchCampus(String id);
  Institution fetchInstitution(String id);
  ServicePoint fetchServicePoint(String id);
  MaterialType fetchMaterialType(String id);
  LoanType fetchLoanType(String id);
}
