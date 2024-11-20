package it.gov.pagopa.gpd.ingestion.manager.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.gpd.ingestion.manager.entity.enumeration.TransferStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transfer {

    private int id;
    
    private int amount;

    private String category;

    @JsonProperty("transfer_id")
    private String transferId;

    @JsonProperty("inserted_date")
    private Long insertedDate;

    private String iuv;

    @JsonProperty("last_update_date")
    private Long lastUpdateDate;

    @JsonProperty("organization_fiscal_code")
    private String organizationFiscalCode;

    private TransferStatus status;

    @JsonProperty("payment_option_id")
    private int paymentOptionId;
}
