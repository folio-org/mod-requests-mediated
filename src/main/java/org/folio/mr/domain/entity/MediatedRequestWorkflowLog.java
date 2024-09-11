package org.folio.mr.domain.entity;

import java.util.Date;
import java.util.UUID;

import org.folio.mr.domain.MediatedRequestStatus;
import org.folio.mr.domain.converter.MediatedRequestStatusJdbcType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Table(name = "mediated_request_workflow_log")
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class MediatedRequestWorkflowLog {

  @Id
  @GeneratedValue
  private UUID id;

  private UUID mediatedRequestId;

  private String mediatedWorkflow;

  @Column(name = "mediated_request_status", columnDefinition = "MediatedRequestStatus")
  @JdbcType(MediatedRequestStatusJdbcType.class)
  private MediatedRequestStatus mediatedRequestStatus;

  private String mediatedRequestStep;

  @CreationTimestamp
  private Date actionDate;

  // Metadata

  private Date createdDate;

  private UUID createdBy;

  private Date updatedDate;

  private UUID updatedBy;

}
