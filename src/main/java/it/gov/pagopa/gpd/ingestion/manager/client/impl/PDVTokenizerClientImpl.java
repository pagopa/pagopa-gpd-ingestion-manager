package it.gov.pagopa.gpd.ingestion.manager.client.impl;


import it.gov.pagopa.gpd.ingestion.manager.client.PDVTokenizerClient;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.enumeration.ReasonErrorCode;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * {@inheritDoc}
 */
@Component
public class PDVTokenizerClientImpl implements PDVTokenizerClient {

    private final Logger logger = LoggerFactory.getLogger(PDVTokenizerClientImpl.class);
    private final HttpClient client;
    @Value("${pdv.tokenizer.base-path}")
    private String basePath;
    @Value("${pdv.tokenizer.sub-key}")
    private String subscriptionKey;
    @Value("${pdv.tokenizer.sub-key-header}")
    private String subscriptionKeyHeader;
    @Value("${pdv.tokenizer.create-token.endpoint}")
    private String createTokenEndpoint;

    @Autowired
    public PDVTokenizerClientImpl(HttpClient httpClient) {
        this.client = httpClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpResponse<String> createToken(String piiBody) throws PDVTokenizerException {
        String uri = String.format("%s%s", basePath, createTokenEndpoint);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .version(HttpClient.Version.HTTP_2)
                .header(subscriptionKeyHeader, subscriptionKey)
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
