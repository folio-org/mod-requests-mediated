package org.folio.mr.domain.entity;

import java.util.Date;
import java.util.UUID;

import org.folio.mr.domain.converter.UUIDConverter;

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

@Entity
@Table(name = "mediated_request")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MediatedRequestEntity {

  @Id
  @Convert(converter = UUIDConverter.class)
  private UUID id;
  private String requestLevel;
  private String requestType;
  private Date requestDate;
  private String patronComments;
  private UUID requesterId;
  private UUID proxyUserId;
  private UUID instanceId;
  private UUID holdingsRecordId;
  private UUID itemId;
  private String status;
  private UUID cancellationReasonId;
  private UUID cancelledByUserId;
  private String cancellationAdditionalInformation;
  private Date cancelledDate;
  private Integer position;
  private String fulfillmentPreference;
  private UUID deliveryAddressTypeId;
  private UUID pickupServicePointId;
}
