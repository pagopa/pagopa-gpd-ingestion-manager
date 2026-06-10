package it.gov.pagopa.gpd.ingestion.manager.client.impl;

import it.gov.pagopa.gpd.ingestion.manager.client.AnonymizerClient;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.enumeration.ReasonErrorCode;
import it.gov.pagopa.gpd.ingestion.manager.exception.AnonymizerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
public class AnonymizerClientImpl implements AnonymizerClient {

    private final Logger logger = LoggerFactory.getLogger(AnonymizerClientImpl.class);

    private final HttpClient client;
    private final String subscriptionKey;
    private final String subscriptionKeyHeader;
    private final String anonymizerEndpoint;
    private final String requestIdHeader;

    @Autowired
    public AnonymizerClientImpl(
            HttpClient client,
            @Value("${anonymizer.endpoint}") String anonymizerEndpoint,
            @Value("${anonymizer.sub-key}") String subscriptionKey,
            @Value("${anonymizer.sub-key-header}") String subscriptionKeyHeader,
            @Value("${anonymizer.request-id-header") String requestIdHeader
    ) {
        this.client = client;
        this.anonymizerEndpoint = anonymizerEndpoint;
        this.subscriptionKey = subscriptionKey;
        this.subscriptionKeyHeader = subscriptionKeyHeader;
        this.requestIdHeader = requestIdHeader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpResponse<String> anonymize(String body) throws AnonymizerException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(anonymizerEndpoint))
                .version(HttpClient.Version.HTTP_2)
                .headers(
                        subscriptionKeyHeader, subscriptionKey,
                        requestIdHeader, MDC.get("requestId")
                )
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return makeCall(request);
    }

    private HttpResponse<String> makeCall(HttpRequest request) throws AnonymizerException {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new AnonymizerException("I/O error when invoking Anonymizer", ReasonErrorCode.ERROR_ANONYMIZER_IO.getCode(), e);
        } catch (InterruptedException e) {
            logger.warn("This thread was interrupted, restoring the state");
            Thread.currentThread().interrupt();
            throw new AnonymizerException("Unexpected error when invoking Anonymizer, the thread was interrupted", ReasonErrorCode.ERROR_ANONYMIZER_UNEXPECTED.getCode(), e);
        }
    }
}
