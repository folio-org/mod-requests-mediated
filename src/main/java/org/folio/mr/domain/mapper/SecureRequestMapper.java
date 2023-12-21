package org.folio.mr.domain.mapper;

import org.folio.mr.domain.dto.SecureRequest;
import org.folio.mr.domain.entity.SecureRequestEntity;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface SecureRequestMapper {
  SecureRequest mapEntityToDto(SecureRequestEntity circulationItem);
  SecureRequestEntity mapDtoToEntity(SecureRequest request);
}
