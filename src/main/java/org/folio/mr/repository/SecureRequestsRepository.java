package org.folio.mr.repository;

import org.folio.mr.domain.entity.SecureRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SecureRequestsRepository extends JpaRepository<SecureRequestEntity, UUID> {
}
