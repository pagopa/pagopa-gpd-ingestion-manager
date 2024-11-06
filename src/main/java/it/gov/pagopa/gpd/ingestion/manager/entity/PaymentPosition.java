package it.gov.pagopa.gpd.ingestion.manager.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.gpd.ingestion.manager.entity.enumeration.PaymentPositionStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentPosition {
    private int id;
    @NonNull
    private String iupd;
    @NonNull
    @JsonProperty("fiscal_code")
    private String fiscalCode;
    @JsonProperty("postal_code")
    private String postalCode;
    private String province;
    @NonNull
    @JsonProperty("max_due_date")
    private Long maxDueDate;
    @NonNull
    @JsonProperty("min_due_date")
    private Long minDueDate;
    @NonNull
    @JsonProperty("organizationFiscalCode")
    private String organizationFiscalCode;
    @NonNull
    @JsonProperty("company_name")
    private String companyName;
    @JsonProperty("publish_date")
    private Long publishDate;
    private String region;
    @NonNull
    private PaymentPositionStatus status;
    @NonNull
    private String type;
    @JsonProperty("validity_date")
    private Long validityDate;
    @JsonProperty("switchToExpired")
    private boolean switchToExpired;
    @JsonProperty("payment_date")
    private Long paymentDate;
    @NonNull
    @JsonProperty("last_update_date")
    private Long lastUpdateDate;
    @NonNull
    @JsonProperty("inserted_date")
    private Long insertedDate;
}
