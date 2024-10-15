package org.folio.mr.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Data
@Table(name = "fake_user")
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class FakeUser {

  @Id
  @GeneratedValue
  private UUID id;

  private UUID userId;

  // Metadata

  private Date createdDate;

  private UUID createdBy;

  private Date updatedDate;

  private UUID updatedBy;

}
