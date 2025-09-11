package org.folio.mr.repository;

import java.util.UUID;
import org.folio.mr.domain.entity.BatchRequest;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BatchRequestRepository extends JpaCqlRepository<BatchRequest, UUID> {
}
