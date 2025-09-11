package org.folio.mr.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class MetadataEntity {

  @CreatedDate
  @Column(name = "created_date", nullable = false, updatable = false)
  private Timestamp createdDate;

  @LastModifiedDate
  @Column(name = "updated_date", nullable = false)
  private Timestamp updatedDate;

  @CreatedBy
  @Column(name = "created_by_user_id", nullable = false, updatable = false)
  private UUID createdByUserId;

  @LastModifiedBy
  @Column(name = "updated_by_user_id", nullable = false)
  private UUID updatedByUserId;

  @Column(name = "created_by_username", length = 100)
  private String createdByUsername;

  @Column(name = "updated_by_username", length = 100)
  private String updatedByUsername;
}
