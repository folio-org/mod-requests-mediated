package org.folio.mr.repository;

import java.util.UUID;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MediatedBatchRequestSplitRepository extends JpaCqlRepository<MediatedBatchRequestSplit, UUID> {

  @Query("select a from MediatedBatchRequestSplit a where a.mediatedBatchRequest.id = ?1")
  Page<MediatedBatchRequestSplit> findAllByBatchId(UUID batchId, Pageable pageable);
}
