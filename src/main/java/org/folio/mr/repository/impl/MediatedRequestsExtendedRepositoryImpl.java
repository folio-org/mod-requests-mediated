package org.folio.mr.repository.impl;

import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.repository.MediatedRequestsExtendedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import jakarta.persistence.EntityManager;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class MediatedRequestsExtendedRepositoryImpl implements MediatedRequestsExtendedRepository {

  private final EntityManager em;
  private final Cql2JpaCriteriaAltern<MediatedRequestEntity> cql2JpaCriteria;


  public MediatedRequestsExtendedRepositoryImpl(
    EntityManager entityManager) {
    this.em = entityManager;
    this.cql2JpaCriteria = new Cql2JpaCriteriaAltern<>(MediatedRequestEntity.class, entityManager);
  }

  @Override
  public Page<MediatedRequestEntity> findByCql(String cql, Pageable pageable) {
    var criteria = cql2JpaCriteria.toCollectCriteria(cql);
    var result = em.createQuery(criteria).setFirstResult((int)pageable.getOffset())
      .setMaxResults(pageable.getPageSize()).getResultList();

    return PageableExecutionUtils.getPage(result, pageable, () -> count(cql));
  }

  @Override
  public long count(String cql) {
    var criteria = cql2JpaCriteria.toCountCriteria(cql);
    return em.createQuery(criteria).getSingleResult();
  }
}
