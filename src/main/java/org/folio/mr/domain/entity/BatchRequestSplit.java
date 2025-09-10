package org.folio.mr.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.converter.BatchRequestSplitStatusJdbcType;
import org.hibernate.Hibernate;
import org.hibernate.annotations.JdbcType;
import org.springframework.data.domain.Persistable;

@Getter
@Setter
@Builder
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "batch_request_split")
public class BatchRequestSplit extends MetadataEntity implements Persistable<UUID>, Identifiable<UUID> {

  @Id
  @Column(name = "id", nullable = false, unique = true)
  private UUID id;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "batch_id", nullable = false)
  private BatchRequest batchRequest;

  @Column(name = "item_id")
  private UUID itemId;

  @Column(name = "requester_id", nullable = false)
  private UUID requesterId;

  @Column(name = "pickup_service_point_id")
  private UUID pickupServicePointId;

  @Enumerated(EnumType.STRING)
  @JdbcType(BatchRequestSplitStatusJdbcType.class)
  @Column(name = "status", nullable = false, columnDefinition = "BatchRequestSplitStatus")
  private BatchRequestSplitStatus status;

  @Column(name = "confirmed_request_id")
  private UUID confirmedRequestId;

  @Column(name = "patron_comments")
  private String patronComments;

  @Column(name = "error_details")
  private String errorDetails;

  @Column(name = "created_by_username", length = 100)
  private String createdByUsername;

  @Column(name = "updated_by_username", length = 100)
  private String updatedByUsername;

  @Transient
  private boolean isNew = true;

  @Override
  public int hashCode() {
    return Objects.hashCode(getId());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    BatchRequestSplit that = (BatchRequestSplit) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @PostLoad
  @PrePersist
  void markNotNew() {
    this.isNew = false;
  }
}
