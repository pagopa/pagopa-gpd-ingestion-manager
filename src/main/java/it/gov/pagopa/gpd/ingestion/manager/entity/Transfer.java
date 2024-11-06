package it.gov.pagopa.gpd.ingestion.manager.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.gpd.ingestion.manager.entity.enumeration.TransferStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Transfer {
    private int amount;
    @NonNull
    private String category;
    @NonNull
    @JsonProperty("transfer_id")
    private String transferId;
    @NonNull
    @JsonProperty("inserted_date")
    private Long insertedDate;
    @NonNull
    private String iuv;
    @NonNull
    @JsonProperty("last_update_date")
    private Long lastUpdateDate;
    @NonNull
    @JsonProperty("organization_fiscal_code")
    private String organizationFiscalCode;
    @NonNull
    private TransferStatus status;
}
