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
@Table(name = "fake_patron_link")
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class FakePatronLink {

  @Id
  @GeneratedValue
  private UUID id;

  private UUID userId;

  private UUID fakeUserId;

  // Metadata

  private Date createdDate;

  private UUID createdBy;

  private Date updatedDate;

  private UUID updatedBy;

}
