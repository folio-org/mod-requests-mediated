package org.folio.mr.domain.entity;

import java.util.Date;
import java.util.UUID;

import org.folio.mr.domain.converter.MediatedRequestStatusJdbcType;
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
  private String mediatedRequestStatus;
  private String mediatedRequestStep;
  private Date actionDate;
  // Metadata
  @Column(name = "created_date")
  private Date createdDate;
  @Column(name = "created_by")
  private UUID createdBy;
  @Column(name = "updated_date")
  private Date updatedDate;
  @Column(name = "updated_by")
  private UUID updatedBy;
}
