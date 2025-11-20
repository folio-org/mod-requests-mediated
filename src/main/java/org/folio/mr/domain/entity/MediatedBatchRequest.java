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
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(name = "batch_request")
public class MediatedBatchRequest extends MetadataEntity implements Persistable<UUID>, Identifiable<UUID> {

  @Transient
  private boolean isNew = true;

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

  // copy column of status for easier querying
  private String mediatedRequestStatus;

  @PostLoad
  @PrePersist
  void markNotNew() {
    this.isNew = false;
  }

  public static BatchRequestBuilder builder() {
    return new BatchRequestBuilder();
  }

  public static class BatchRequestBuilder {
    private UUID id;
    private UUID requesterId;
    private BatchRequestStatus status;
    private Timestamp requestDate;
    private String patronComments;
    private String mediatedWorkflow;

    // MetadataEntity fields
    private Timestamp createdDate;
    private UUID createdByUserId;
    private String createdByUsername;
    private Timestamp updatedDate;
    private UUID updatedByUserId;
    private String updatedByUsername;

    public BatchRequestBuilder id(UUID id) {
      this.id = id;
      return this;
    }

    public BatchRequestBuilder requesterId(UUID requesterId) {
      this.requesterId = requesterId;
      return this;
    }

    public BatchRequestBuilder status(BatchRequestStatus status) {
      this.status = status;
      return this;
    }

    public BatchRequestBuilder requestDate(Timestamp requestDate) {
      this.requestDate = requestDate;
      return this;
    }

    public BatchRequestBuilder patronComments(String patronComments) {
      this.patronComments = patronComments;
      return this;
    }

    public BatchRequestBuilder mediatedWorkflow(String mediatedWorkflow) {
      this.mediatedWorkflow = mediatedWorkflow;
      return this;
    }

    public BatchRequestBuilder createdDate(Timestamp createdDate) {
      this.createdDate = createdDate;
      return this;
    }

    public BatchRequestBuilder createdByUserId(UUID createdByUserId) {
      this.createdByUserId = createdByUserId;
      return this;
    }

    public BatchRequestBuilder createdByUsername(String createdByUsername) {
      this.createdByUsername = createdByUsername;
      return this;
    }

    public BatchRequestBuilder updatedDate(Timestamp updatedDate) {
      this.updatedDate = updatedDate;
      return this;
    }

    public BatchRequestBuilder updatedByUserId(UUID updatedByUserId) {
      this.updatedByUserId = updatedByUserId;
      return this;
    }

    public BatchRequestBuilder updatedByUsername(String updatedByUsername) {
      this.updatedByUsername = updatedByUsername;
      return this;
    }

    public MediatedBatchRequest build() {
      var entity = new MediatedBatchRequest();
      entity.setId(id);
      entity.setRequesterId(requesterId);
      entity.setStatus(status);
      entity.setRequestDate(requestDate);
      entity.setPatronComments(patronComments);
      entity.setMediatedWorkflow(mediatedWorkflow);
      entity.setCreatedDate(createdDate);
      entity.setCreatedByUserId(createdByUserId);
      entity.setCreatedByUsername(createdByUsername);
      entity.setUpdatedDate(updatedDate);
      entity.setUpdatedByUserId(updatedByUserId);
      entity.setUpdatedByUsername(updatedByUsername);
      return entity;
    }
  }
}
