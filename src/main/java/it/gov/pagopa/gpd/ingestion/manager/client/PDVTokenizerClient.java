package it.gov.pagopa.gpd.ingestion.manager.client;

import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
import it.gov.pagopa.gpd.ingestion.manager.model.tokenizer.PiiResource;

import java.net.http.HttpResponse;

/**
 * Client for invoking PDV Tokenizer service
 */
public interface PDVTokenizerClient {

    /**
     * Create a new token for the specified PII
     *
     * @param piiBody the {@link PiiResource} serialized as String
     * @return the {@link HttpResponse} of the PDV Tokenizer service
     * @throws PDVTokenizerException if an error occur when invoking the PDV Tokenizer service
     */
    HttpResponse<String> createToken(String piiBody) throws PDVTokenizerException;
}
