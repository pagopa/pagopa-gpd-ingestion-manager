package it.gov.pagopa.gpd.ingestion.manager.events.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("inserted_date")
    private Long insertedDate;

    @JsonProperty("is_partial_payment")
    private boolean isPartialPayment;

    private String iuv;
    private String nav;

    @JsonProperty("last_update_date")
    private Long lastUpdateDate;

    @JsonProperty("organization_fiscal_code")
    private String organizationFiscalCode;

    private String status;

    @JsonProperty("retention_date")
    private Long retentionDate;

    @JsonProperty("notification_fee")
    private int notificationFee;

    @JsonProperty("last_updated_date_notification_fee")
    private Long lastUpdatedDateNotificationFee;

    // Debtor info
    @JsonProperty("fiscal_code")
    private String fiscalCode;

    private String type;

    private String region;

    @JsonProperty("send_sync")
    private boolean sendSync;

    @JsonProperty("psp_code")
    private String pspCode;

    @JsonProperty("switch_to_expired")
    private boolean switchToExpired;

    @JsonProperty("validity_date")
    private Long validityDate;

    @JsonProperty("payment_plan_id")
    private String paymentPlanId;

    @JsonProperty("payment_option_description")
    private String paymentOptionDescription;
}
