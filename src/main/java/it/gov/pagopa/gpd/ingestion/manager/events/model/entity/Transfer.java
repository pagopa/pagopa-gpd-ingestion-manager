package it.gov.pagopa.gpd.ingestion.manager.events.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transfer {

    private long id;

    @JsonProperty("payment_option_id")
    private long paymentOptionId;

    private long amount;

    private String category;

    @JsonProperty("transfer_id")
    private String transferId;

    @JsonProperty("inserted_date")
    private LocalDateTime insertedDate;

    private String iuv;

    @JsonProperty("last_updated_date")
    private LocalDateTime lastUpdatedDate;

    @JsonProperty("organization_fiscal_code")
    private String organizationFiscalCode;

    @JsonProperty("remittance_information")
    private String remittanceInformation;

    private String status;
}
