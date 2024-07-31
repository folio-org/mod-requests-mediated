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
  public boolean equals(Object obj) {
    if (obj instanceof MediatedRequestInstanceIdentifier identifier) {
      var mediatedRequestEquals =
        (mediatedRequest == null && identifier.mediatedRequest == null) ||
          (mediatedRequest != null && mediatedRequest.equals(identifier.mediatedRequest));
      var identifierTypeIdEquals =
        (identifierTypeId == null && identifier.identifierTypeId == null) ||
          (identifierTypeId != null && identifierTypeId.equals(identifier.identifierTypeId));
      var valueEquals = (value == null && identifier.value == null) ||
        (value != null && value.equals(identifier.value));

      return mediatedRequestEquals && identifierTypeIdEquals && valueEquals;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mediatedRequest == null ? null : mediatedRequest.getId(),
      identifierTypeId, value);
  }
}
