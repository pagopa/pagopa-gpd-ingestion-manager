package it.gov.pagopa.gpd.ingestion.manager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;

/**
 * Service that wrap the {@link PDVTokenizerService} for adding retry logic for tokenizer responses with 429 status code
 */
public interface PDVTokenizerServiceRetryWrapper {

    /**
     * Call {@link PDVTokenizerService#generateTokenForFiscalCode(String)} with retry on failure
     *
     * @param fiscalCode the fiscal code
     * @return the generated token
     * @throws JsonProcessingException if an error occur when parsing input or output
     * @throws PDVTokenizerException   if an error occur when invoking the PDV Tokenizer
     */
    String generateTokenForFiscalCodeWithRetry(String fiscalCode) throws PDVTokenizerException, JsonProcessingException;
}
