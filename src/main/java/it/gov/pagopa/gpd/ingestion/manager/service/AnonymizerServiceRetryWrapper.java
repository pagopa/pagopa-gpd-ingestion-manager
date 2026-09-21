package it.gov.pagopa.gpd.ingestion.manager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.gpd.ingestion.manager.exception.AnonymizerException;

/**
 * Service that wrap the {@link AnonymizerService} for adding retry logic for anonymized responses with 429 status code
 */
public interface AnonymizerServiceRetryWrapper {

    /**
     * Call {@link AnonymizerService#anonymize(String)} with retry on failure
     *
     * @param body the string to anonymize
     * @return the anonymized string
     * @throws JsonProcessingException if an error occur when parsing input or output
     * @throws AnonymizerException     if an error occur when invoking the Anonymizer
     */
    String anonymizeWithRetry(String body) throws AnonymizerException, JsonProcessingException;
}
