package org.folio.mr.domain.entity;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.mr.domain.converter.UUIDConverter;
import java.util.UUID;

@Entity
@Table(name = "secure_request")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SecureRequestEntity {

  @Id
  @Convert(converter = UUIDConverter.class)
  private UUID id;

}
