package it.gov.pagopa.gpd.ingestion.manager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.gpd.ingestion.manager.client.AnonymizerClient;
import it.gov.pagopa.gpd.ingestion.manager.exception.AnonymizerException;
import it.gov.pagopa.gpd.ingestion.manager.model.AnonymizerModel;

/**
 * Service that handle the input and output for the {@link AnonymizerClient}
 */
public interface AnonymizerService {

    /**
     * Anonymize a text for the specified string by calling {@link AnonymizerClient#anonymize(AnonymizerModel)}
     *
     * @param body the String to anonymize
     * @return the generated anonymized text
     * @throws JsonProcessingException if an error occur when parsing input or output
     * @throws AnonymizerException   if an error occur when invoking the Anonymizer
     */
    String anonymize(String body) throws AnonymizerException, JsonProcessingException;
}
