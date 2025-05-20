package org.folio.mr.domain.mapper;

import org.folio.mr.domain.dto.CheckOutDryRunRequest;
import org.folio.mr.domain.dto.CheckOutRequest;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface CirculationMapper {
  CheckOutDryRunRequest toDryRunRequest(CheckOutRequest checkOutRequest);
}
