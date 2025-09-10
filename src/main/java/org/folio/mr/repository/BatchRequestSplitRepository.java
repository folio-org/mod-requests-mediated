package org.folio.mr.repository;

import java.util.UUID;
import org.folio.mr.domain.entity.BatchRequestSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BatchRequestSplitRepository extends JpaRepository<BatchRequestSplit, UUID> {
}
