package it.gov.pagopa.gpd.ingestion.manager.client;

import it.gov.pagopa.gpd.ingestion.manager.exception.AnonymizerException;
import it.gov.pagopa.gpd.ingestion.manager.model.AnonymizerModel;

import java.net.http.HttpResponse;

/**
 * Client for invoking Anonymizer service
 */
public interface AnonymizerClient {

    /**
     * Return an anonymized string without PII
     *
     * @param body the {@link AnonymizerModel} serialized as String
     * @return the {@link HttpResponse} of the Anonymizer service
     * @throws AnonymizerException if an error occur when invoking the Anonymizer service
     */
    HttpResponse<String> anonymize(String body) throws AnonymizerException;
}