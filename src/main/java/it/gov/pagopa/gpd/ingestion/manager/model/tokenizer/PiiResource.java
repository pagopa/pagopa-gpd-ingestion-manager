package it.gov.pagopa.gpd.ingestion.manager.model.tokenizer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model class that hold Personal Identifiable Information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PiiResource {

    private String pii;
}
