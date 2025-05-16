package org.folio.mr.repository;

import org.folio.mr.domain.entity.FakePatronLink;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface FakePatronLinkRepository extends JpaCqlRepository<FakePatronLink, UUID> {
  Collection<FakePatronLink> findByUserId(UUID realUserId);
}
