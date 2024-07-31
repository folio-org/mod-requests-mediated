package org.folio.mr.domain.entity;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "mediated_request_instance_identifier")
@Entity
public class MediatedRequestInstanceIdentifier {
  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "mediated_request_id")
  private MediatedRequestEntity mediatedRequest;

  @Id
  private UUID identifierTypeId;

  @Id
  private String value;

  @Override
  public int hashCode() {
    return Objects.hash(mediatedRequest == null ? null : mediatedRequest.getId(),
      identifierTypeId, value);
  }
}
