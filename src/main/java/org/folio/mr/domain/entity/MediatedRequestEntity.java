package org.folio.mr.domain.entity;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import lombok.Builder;
import org.folio.mr.domain.FulfillmentPreference;
import org.folio.mr.domain.MediatedRequestStatus;
import org.folio.mr.domain.RequestLevel;
import org.folio.mr.domain.RequestType;
import org.folio.mr.domain.converter.FulfillmentPreferenceJdbcType;
import org.folio.mr.domain.converter.MediatedRequestStatusJdbcType;
import org.folio.mr.domain.converter.RequestLevelJdbcType;
import org.folio.mr.domain.converter.RequestTypeJdbcType;
import org.folio.spring.cql.Cql2JpaCriteria;
import org.folio.spring.cql.IgnoreCase;
import org.hibernate.annotations.JdbcType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.With;

@Data
@Table(name = "mediated_request")
@Entity
@IgnoreCase
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MediatedRequestEntity {
  static {  // delete for Trillium, this is only needed for Ramsons and Sunflower
    Cql2JpaCriteria.setCaseAccentsHandlingEnabled(true);
  }

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "request_level", columnDefinition = "RequestLevel")
  @JdbcType(RequestLevelJdbcType.class)
  private RequestLevel requestLevel;

  @Column(name = "request_type", columnDefinition = "RequestType")
  @JdbcType(RequestTypeJdbcType.class)
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
  private String instanceHrid;

  @OneToMany(mappedBy="mediatedRequest", cascade = CascadeType.ALL)
  private Set<MediatedRequestInstanceIdentifier> instanceIdentifiers;

  private UUID holdingsRecordId;

  private UUID itemId;

  private String itemBarcode;

  private String mediatedWorkflow;

  @Column(name = "mediated_request_status", columnDefinition = "MediatedRequestStatus")
  @JdbcType(MediatedRequestStatusJdbcType.class)
  private MediatedRequestStatus mediatedRequestStatus;

  private String mediatedRequestStep;

  private String status;

  private UUID cancellationReasonId;

  private UUID cancelledByUserId;

  private String cancellationAdditionalInformation;

  private Date cancelledDate;

  private Integer position;

  @Column(name = "fulfillment_preference", columnDefinition = "FulfillmentPreference")
  @JdbcType(FulfillmentPreferenceJdbcType.class)
  private FulfillmentPreference fulfillmentPreference;

  private UUID deliveryAddressTypeId;

  private UUID pickupServicePointId;

  // Search index

  private String callNumber;

  private String callNumberPrefix;

  private String callNumberSuffix;

  private String fullCallNumber;

  private String shelvingOrder;

  private String pickupServicePointName;

  private UUID confirmedRequestId;

  private String idText;

  private String requestLevelText;

  private String requestTypeText;

  private String mediatedRequestStatusText;

  private String fulfillmentPreferenceText;

  // Metadata

  @Column(name = "created_date")
  private Date createdDate;

  @Column(name = "created_by_user_id")
  private UUID createdByUserId;

  @Column(name = "created_by_username")
  private String createdByUsername;

  @Column(name = "updated_date")
  private Date updatedDate;

  @Column(name = "updated_by_user_id")
  private UUID updatedByUserId;

  @Column(name = "updated_by_username")
  private String updatedByUsername;

}
