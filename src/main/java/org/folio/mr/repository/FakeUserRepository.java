package org.folio.mr.repository;

import org.folio.mr.domain.entity.FakeUser;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FakeUserRepository extends JpaCqlRepository<FakeUser, UUID> {

}
