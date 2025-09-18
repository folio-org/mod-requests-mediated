package org.folio.mr.repository;

import java.util.UUID;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediatedBatchRequestRepository extends JpaCqlRepository<MediatedBatchRequest, UUID> {
}
