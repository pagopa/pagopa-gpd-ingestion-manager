package it.gov.pagopa.gpd.ingestion.manager.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.gpd.ingestion.manager.entity.PaymentPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataCapturePaymentPosition {

    private PaymentPosition before;
    private PaymentPosition after;
    private String op;
    @JsonProperty("ts_ms")
    private Long tsMs;
    @JsonProperty("ts_us")
    private Long tsUs;
    @JsonProperty("ts_ns")
    private Long tsNs;
}
