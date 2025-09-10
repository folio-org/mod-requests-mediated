package org.folio.mr.repository;

import java.util.UUID;
import org.folio.mr.domain.entity.BatchRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BatchRequestRepository extends JpaRepository<BatchRequest, UUID> {
}
