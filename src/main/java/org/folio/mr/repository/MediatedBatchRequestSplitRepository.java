package org.folio.mr.repository;

import java.util.List;
import java.util.UUID;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.domain.entity.projection.BatchRequestStats;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MediatedBatchRequestSplitRepository extends JpaCqlRepository<MediatedBatchRequestSplit, UUID> {

  @Query("select a from MediatedBatchRequestSplit a where a.mediatedBatchRequest.id = ?1")
  Page<MediatedBatchRequestSplit> findAllByBatchId(UUID batchId, Pageable pageable);

  @Query("select a from MediatedBatchRequestSplit a where a.mediatedBatchRequest.id = ?1")
  List<MediatedBatchRequestSplit> findAllByBatchId(UUID batchId);

  @Query("""
    select
      br.id,
      count(brs.id) as total,
      count(case when brs.status = 'Pending' then 1 end) as pending,
      count(case when brs.status = 'Completed' then 1 end) as completed,
      count(case when brs.status = 'Failed' then 1 end) as failed,
      count(case when brs.status = 'In progress' then 1 end) as inProgress
    from MediatedBatchRequest br
    left join MediatedBatchRequestSplit brs on br.id = brs.mediatedBatchRequest.id
    where br.id = :batchId
    group by br.id
    """)
  BatchRequestStats findMediatedBatchRequestStats(@Param("batchId") UUID batchId);
}
