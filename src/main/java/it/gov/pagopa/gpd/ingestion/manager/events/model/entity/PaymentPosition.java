package it.gov.pagopa.gpd.ingestion.manager.events.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.enumeration.PaymentPositionStatus;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentPosition {
    private int id;

    private String iupd;

    @JsonProperty("fiscal_code")
    private String fiscalCode;

    @JsonProperty("postal_code")
    private String postalCode;

    private String province;

    @JsonProperty("max_due_date")
    private Long maxDueDate;

    @JsonProperty("min_due_date")
    private Long minDueDate;

    @JsonProperty("organization_fiscal_code")
    private String organizationFiscalCode;

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("publish_date")
    private Long publishDate;

    private String region;

    private PaymentPositionStatus status;

    private String type;

    @JsonProperty("payment_date")
    private Long paymentDate;

    @JsonProperty("last_updated_date")
    private Long lastUpdatedDate;

    @JsonProperty("inserted_date")
    private Long insertedDate;

    @JsonProperty("service_type")
    private String serviceType;

    @JsonProperty("pay_stand_in")
    private String payStandIn;

    private boolean pull;
}
