package org.folio.mr.repository;

import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MediatedRequestsExtendedRepository {

  Page<MediatedRequestEntity> findByCql(String cql, Pageable pageable);
  long count(String cql);

}
