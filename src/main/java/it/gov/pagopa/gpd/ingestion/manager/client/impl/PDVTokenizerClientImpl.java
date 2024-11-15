package it.gov.pagopa.gpd.ingestion.manager.client.impl;


import it.gov.pagopa.gpd.ingestion.manager.client.PDVTokenizerClient;
import it.gov.pagopa.gpd.ingestion.manager.entity.enumeration.ReasonErrorCode;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * {@inheritDoc}
 */
public class PDVTokenizerClientImpl implements PDVTokenizerClient {

    private static final String BASE_PATH = System.getenv().getOrDefault("PDV_TOKENIZER_BASE_PATH", "https://api.uat.tokenizer.pdv.pagopa.it/tokenizer/v1");
    private static final String SUBSCRIPTION_KEY = System.getenv().getOrDefault("PDV_TOKENIZER_SUBSCRIPTION_KEY", "");
    private static final String SUBSCRIPTION_KEY_HEADER = System.getenv().getOrDefault("TOKENIZER_APIM_HEADER_KEY", "x-api-key");
    private static final String CREATE_TOKEN_ENDPOINT = System.getenv().getOrDefault("PDV_TOKENIZER_CREATE_TOKEN_ENDPOINT", "/tokens");
    private static PDVTokenizerClientImpl instance;
    private final Logger logger = LoggerFactory.getLogger(PDVTokenizerClientImpl.class);
    private final HttpClient client;

    private PDVTokenizerClientImpl() {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    public PDVTokenizerClientImpl(HttpClient client) {
        this.client = client;
    }

    public static PDVTokenizerClientImpl getInstance() {
        if (instance == null) {
            instance = new PDVTokenizerClientImpl();
        }
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpResponse<String> createToken(String piiBody) throws PDVTokenizerException {
        String uri = String.format("%s%s", BASE_PATH, CREATE_TOKEN_ENDPOINT);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .version(HttpClient.Version.HTTP_2)
                .header(SUBSCRIPTION_KEY_HEADER, SUBSCRIPTION_KEY)
                .PUT(HttpRequest.BodyPublishers.ofString(piiBody))
                .build();

        return makeCall(request);
    }

    private HttpResponse<String> makeCall(HttpRequest request) throws PDVTokenizerException {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new PDVTokenizerException("I/O error when invoking PDV Tokenizer", ReasonErrorCode.ERROR_PDV_IO.getCode(), e);
        } catch (InterruptedException e) {
            logger.warn("This thread was interrupted, restoring the state");
            Thread.currentThread().interrupt();
            throw new PDVTokenizerException("Unexpected error when invoking PDV Tokenizer, the thread was interrupted", ReasonErrorCode.ERROR_PDV_UNEXPECTED.getCode(), e);
        }
    }
}
