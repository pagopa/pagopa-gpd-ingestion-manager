package it.gov.pagopa.gpd.ingestion.manager.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.gpd.ingestion.manager.entity.enumeration.PaymentOptionStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentOption {
    private int id;
    @JsonProperty("payment_position_id")
    private int paymentPositionId;
    private int amount;
    private String description;
    @NonNull
    @JsonProperty("due_date")
    private Long dueDate;
    private int fee;
    @JsonProperty("flow_reporting_id")
    private String flowReportingId;
    @JsonProperty("receipt_id")
    private String receiptId;
    @NonNull
    @JsonProperty("inserted_date")
    private Long insertedDate;
    @JsonProperty("is_partial_payment")
    private boolean isPartialPayment;
    @NonNull
    private String iuv;
    @NonNull
    @JsonProperty("last_update_date")
    private Long lastUpdateDate;
    @NonNull
    @JsonProperty("organization_fiscal_code")
    private String organizationFiscalCode;
    @NonNull
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
}
