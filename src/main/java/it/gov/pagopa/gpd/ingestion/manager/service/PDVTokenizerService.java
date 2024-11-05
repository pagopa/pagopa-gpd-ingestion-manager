package it.gov.pagopa.gpd.ingestion.manager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.gpd.ingestion.manager.client.PDVTokenizerClient;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;

/**
 * Service that handle the input and output for the {@link PDVTokenizerClient}
 */
public interface PDVTokenizerService {

    /**
     * Generate a token for the specified fiscal code by calling {@link PDVTokenizerClient#createToken(String)}
     *
     * @param fiscalCode the fiscal code
     * @return the generated token
     * @throws JsonProcessingException if an error occur when parsing input or output
     * @throws PDVTokenizerException if an error occur when invoking the PDV Tokenizer
     */
    String generateTokenForFiscalCode(String fiscalCode) throws PDVTokenizerException, JsonProcessingException;
}
