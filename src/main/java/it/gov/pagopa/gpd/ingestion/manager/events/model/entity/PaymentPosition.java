package it.gov.pagopa.gpd.ingestion.manager.events.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentPosition {
    private long id;

    private String iupd;

    @JsonProperty("max_due_date")
    private LocalDateTime maxDueDate;
    @JsonProperty("min_due_date")
    private LocalDateTime minDueDate;

    @JsonProperty("organization_fiscal_code")
    private String organizationFiscalCode;
    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("publish_date")
    private LocalDateTime publishDate;

    private String status;

    @JsonProperty("payment_date")
    private LocalDateTime paymentDate;

    @JsonProperty("last_updated_date")
    private LocalDateTime lastUpdatedDate;
    @JsonProperty("inserted_date")
    private LocalDateTime insertedDate;

    private boolean pull;
    @JsonProperty("pay_stand_in")
    private boolean payStandIn;

    @JsonProperty("service_type")
    private String serviceType;
}
