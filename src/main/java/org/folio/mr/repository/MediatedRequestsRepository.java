package org.folio.mr.repository;

import java.util.Optional;
import java.util.UUID;

import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MediatedRequestsRepository extends JpaCqlRepository<MediatedRequestEntity, UUID> {

  @Query("""
    SELECT mr from MediatedRequestEntity mr
    WHERE mr.itemBarcode = ?1
    AND mr.mediatedWorkflow = 'Private request'
    AND mr.mediatedRequestStatus = 'Open'
    AND mr.mediatedRequestStep = 'In transit for approval'
    """)
  Optional<MediatedRequestEntity> findRequestForItemArrivalConfirmation(String itemBarcode);

  @Query("""
    SELECT mr from MediatedRequestEntity mr
    WHERE mr.itemBarcode = ?1
    AND mr.mediatedWorkflow = 'Private request'
    AND mr.mediatedRequestStatus = 'Open'
    AND mr.mediatedRequestStep = 'Item arrived'
    """)
  Optional<MediatedRequestEntity> findRequestForSendingInTransit(String itemBarcode);

  Optional<MediatedRequestEntity> findByConfirmedRequestId(UUID confirmedRequestId);
}
