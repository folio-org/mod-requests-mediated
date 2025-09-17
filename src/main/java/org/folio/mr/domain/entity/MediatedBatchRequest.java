package org.folio.mr.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.mr.domain.BatchRequestStatus;
import org.folio.mr.domain.converter.BatchRequestStatusJdbcType;
import org.hibernate.annotations.JdbcType;
import org.springframework.data.domain.Persistable;

@Getter
@Setter
@Builder
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(name = "batch_request")
public class MediatedBatchRequest extends MetadataEntity implements Persistable<UUID>, Identifiable<UUID> {

  @EqualsAndHashCode.Include
  @Id
  @Column(name = "id", nullable = false, unique = true)
  private UUID id;

  @Column(name = "requester_id", nullable = false)
  private UUID requesterId;

  @Enumerated(EnumType.STRING)
  @JdbcType(BatchRequestStatusJdbcType.class)
  @Column(name = "status", nullable = false, columnDefinition = "BatchRequestStatus")
  private BatchRequestStatus status;

  @Column(name = "request_date", nullable = false)
  private Timestamp requestDate;

  @Column(name = "patron_comments")
  private String patronComments;

  @Column(name = "mediated_workflow", length = 255)
  private String mediatedWorkflow;

  @Transient
  private boolean isNew = true;

  @PostLoad
  @PrePersist
  void markNotNew() {
    this.isNew = false;
  }
}
