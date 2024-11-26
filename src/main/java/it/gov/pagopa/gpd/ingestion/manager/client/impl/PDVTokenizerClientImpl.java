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
    private final String basePath;
    private final String subscriptionKey;
    private final String subscriptionKeyHeader;
    private final String createTokenEndpoint;

    @Autowired
    public PDVTokenizerClientImpl(
            HttpClient client,
            @Value("${pdv.tokenizer.base-path}") String basePath,
            @Value("${pdv.tokenizer.sub-key}") String subscriptionKey,
            @Value("${pdv.tokenizer.sub-key-header}") String subscriptionKeyHeader,
            @Value("${pdv.tokenizer.create-token.endpoint}") String createTokenEndpoint
    ) {
        this.client = client;
        this.basePath = basePath;
        this.subscriptionKey = subscriptionKey;
        this.subscriptionKeyHeader = subscriptionKeyHeader;
        this.createTokenEndpoint = createTokenEndpoint;
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
