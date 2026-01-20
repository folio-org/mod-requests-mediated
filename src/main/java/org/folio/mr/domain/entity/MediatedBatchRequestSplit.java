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
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.converter.BatchRequestSplitStatusJdbcType;
import org.hibernate.annotations.JdbcType;
import org.springframework.data.domain.Persistable;


@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(name = "batch_request_split")
public class MediatedBatchRequestSplit extends MetadataEntity implements Persistable<UUID>, Identifiable<UUID> {

  @Transient
  private boolean isNew = true;

  @EqualsAndHashCode.Include
  @Id
  @Column(name = "id", nullable = false, unique = true)
  private UUID id;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "batch_id", nullable = false)
  private MediatedBatchRequest mediatedBatchRequest;

  @Column(name = "request_status")
  private String requestStatus;

  @Column(name = "item_id", nullable = false)
  private UUID itemId;

  @Column(name = "requester_id", nullable = false)
  private UUID requesterId;

  @Column(name = "pickup_service_point_id", nullable = false)
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

  // copy column of status for easier querying
  private String mediatedRequestStatus;

  @PostLoad
  @PrePersist
  void markNotNew() {
    this.isNew = false;
  }

  public static BatchRequestSplitBuilder builder() {
    return new BatchRequestSplitBuilder();
  }

  public static class BatchRequestSplitBuilder {
    private UUID id;
    private MediatedBatchRequest mediatedBatchRequest;
    private String requestStatus;
    private UUID itemId;
    private UUID requesterId;
    private UUID pickupServicePointId;
    private BatchRequestSplitStatus status;
    private UUID confirmedRequestId;
    private String patronComments;
    private String errorDetails;

    public BatchRequestSplitBuilder id(UUID id) {
      this.id = id;
      return this;
    }

    public BatchRequestSplitBuilder mediatedBatchRequest(MediatedBatchRequest mediatedBatchRequest) {
      this.mediatedBatchRequest = mediatedBatchRequest;
      return this;
    }

    public BatchRequestSplitBuilder requestStatus(String requestStatus) {
      this.requestStatus = requestStatus;
      return this;
    }

    public BatchRequestSplitBuilder itemId(UUID itemId) {
      this.itemId = itemId;
      return this;
    }

    public BatchRequestSplitBuilder requesterId(UUID requesterId) {
      this.requesterId = requesterId;
      return this;
    }

    public BatchRequestSplitBuilder pickupServicePointId(UUID pickupServicePointId) {
      this.pickupServicePointId = pickupServicePointId;
      return this;
    }

    public BatchRequestSplitBuilder status(BatchRequestSplitStatus status) {
      this.status = status;
      return this;
    }

    public BatchRequestSplitBuilder confirmedRequestId(UUID confirmedRequestId) {
      this.confirmedRequestId = confirmedRequestId;
      return this;
    }

    public BatchRequestSplitBuilder patronComments(String patronComments) {
      this.patronComments = patronComments;
      return this;
    }

    public BatchRequestSplitBuilder errorDetails(String errorDetails) {
      this.errorDetails = errorDetails;
      return this;
    }

    public MediatedBatchRequestSplit build() {
      MediatedBatchRequestSplit split = new MediatedBatchRequestSplit();
      split.id = this.id;
      split.mediatedBatchRequest = this.mediatedBatchRequest;
      split.requestStatus = this.requestStatus;
      split.itemId = this.itemId;
      split.requesterId = this.requesterId;
      split.pickupServicePointId = this.pickupServicePointId;
      split.status = this.status;
      split.confirmedRequestId = this.confirmedRequestId;
      split.patronComments = this.patronComments;
      split.errorDetails = this.errorDetails;
      return split;
    }
  }
}
