package it.gov.pagopa.gpd.ingestion.manager.model.tokenizer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model class for the details of invalid param error
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvalidParam {

    private String name;
    private String reason;
}
