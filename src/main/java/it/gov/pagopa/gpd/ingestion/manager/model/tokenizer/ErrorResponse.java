package it.gov.pagopa.gpd.ingestion.manager.model.tokenizer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Model class for the error response of the PDV Tokenizer for status 400, 500
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private String detail;
    private String instance;
    private List<InvalidParam> invalidParams;
    private int status;
    private String title;
    private String type;

}
