package it.gov.pagopa.gpd.ingestion.manager.events.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.enumeration.PaymentOptionStatus;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentOption {
  private int id;

  @JsonProperty("payment_position_id")
  private int paymentPositionId;

  private int amount;
  private String description;

  @JsonProperty("due_date")
  private Long dueDate;

  private int fee;

  @JsonProperty("flow_reporting_id")
  private String flowReportingId;

  @JsonProperty("receipt_id")
  private String receiptId;

  @JsonProperty("inserted_date")
  private Long insertedDate;

  @JsonProperty("is_partial_payment")
  private boolean isPartialPayment;

  private String iuv;

  @JsonProperty("last_update_date")
  private Long lastUpdateDate;

  @JsonProperty("organization_fiscal_code")
  private String organizationFiscalCode;

  private PaymentOptionStatus status;

  @JsonProperty("payment_date")
  private Long paymentDate;

  @JsonProperty("payment_method")
  private String paymentMethod;

  @JsonProperty("psp_company")
  private String pspCompany;

  @JsonProperty("reporting_date")
  private Long reportingDate;

  @JsonProperty("retention_date")
  private Long retentionDate;

  @JsonProperty("notification_fee")
  private int notificationFee;

  @JsonProperty("last_updated_date_notification_fee")
  private Long lastUpdatedDateNotificationFee;

  @JsonProperty("payment_plan_id")
  private String paymentPlanId;

  @JsonProperty("switch_to_expired")
  private boolean switchToExpired;

  @JsonProperty("validity_date")
  private Long validityDate;

  // Debtor info
  @JsonProperty("fiscal_code")
  private String fiscalCode;

  @JsonProperty("postal_code")
  private String postalCode;

  private String province;
  private String region;
  private String type;
}
