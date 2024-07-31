package org.folio.mr.domain.entity;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.folio.mr.domain.FulfillmentPreference;
import org.folio.mr.domain.MediatedRequestStatus;
import org.folio.mr.domain.RequestLevel;
import org.folio.mr.domain.RequestType;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "mediated_request")
@Entity
public class MediatedRequestEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "request_level", columnDefinition = "RequestLevel")
  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  private RequestLevel requestLevel;

  @Column(name = "request_type", columnDefinition = "RequestType")
  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  private RequestType requestType;

  private Date requestDate;

  private String patronComments;

  private UUID requesterId;

  private String requesterFirstName;

  private String requesterLastName;

  private String requesterMiddleName;

  private String requesterBarcode;

  private UUID proxyUserId;

  private String proxyFirstName;

  private String proxyLastName;

  private String proxyMiddleName;

  private String proxyBarcode;

  private UUID instanceId;

  private String instanceTitle;

  @OneToMany(mappedBy="mediatedRequest", cascade = CascadeType.ALL)
  private Set<MediatedRequestInstanceIdentifier> instanceIdentifiers;

  private UUID holdingsRecordId;

  private UUID itemId;

  private String itemBarcode;

  private String mediatedWorkflow;

  @Column(name = "mediated_request_status", columnDefinition = "MediatedRequestStatus")
  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  private MediatedRequestStatus mediatedRequestStatus;

  private String mediatedRequestStep;

  private String status;

  private UUID cancellationReasonId;

  private UUID cancelledByUserId;

  private String cancellationAdditionalInformation;

  private Date cancelledDate;

  private Integer position;

  @Column(name = "fulfillment_preference", columnDefinition = "FulfillmentPreference")
  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  private FulfillmentPreference fulfillmentPreference;

  private UUID deliveryAddressTypeId;

  private UUID pickupServicePointId;

  // Search index

  private String callNumber;

  private String callNumberPrefix;

  private String callNumberSuffix;

  private String shelvingOrder;

  private String pickupServicePointName;

  // Metadata

  @Column(name = "created_date")
  private Timestamp createdDate;

  @Column(name = "created_by_user_id")
  private UUID createdByUserId;

  @Column(name = "created_by_username")
  private String createdByUsername;

  @Column(name = "updated_date")
  private Timestamp updatedDate;

  @Column(name = "updated_by_user_id")
  private UUID updatedByUserId;

  @Column(name = "updated_by_username")
  private String updatedByUsername;
}
