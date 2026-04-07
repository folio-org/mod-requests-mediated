package org.folio.mr.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.spring.cql.JpaCqlRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MediatedBatchRequestRepository extends JpaCqlRepository<MediatedBatchRequest, UUID> {

  @Modifying
  @Query(nativeQuery = true, value = """
    UPDATE batch_request br
      SET last_processed_date = NOW()
      WHERE br.id in (
        SELECT batch.id FROM batch_request batch
        WHERE batch.status IN ('Pending', 'In progress')
          AND batch.created_date < :threshold
          AND (batch.last_processed_date IS NULL OR batch.last_processed_date < :threshold)
        LIMIT :limit
        FOR UPDATE OF batch SKIP LOCKED
      )
    RETURNING *;""")
  List<MediatedBatchRequest> getStaleRequests(@Param("threshold") Timestamp threshold, @Param("limit") Integer limit);
}
