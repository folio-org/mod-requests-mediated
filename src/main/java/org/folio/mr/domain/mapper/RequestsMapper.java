package org.folio.mr.domain.mapper;

import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.RequestEntity;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface RequestsMapper {
  Request mapEntityToDto(RequestEntity circulationItem);
  RequestEntity mapDtoToEntity(Request request);
}
