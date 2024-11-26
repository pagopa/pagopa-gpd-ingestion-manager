package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerUnexpectedException;
import it.gov.pagopa.gpd.ingestion.manager.service.PDVTokenizerService;
import it.gov.pagopa.gpd.ingestion.manager.service.PDVTokenizerServiceRetryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * {@inheritDoc}
 */
@Service
public class PDVTokenizerServiceRetryWrapperImpl implements PDVTokenizerServiceRetryWrapper {

    private static final Long INITIAL_INTERVAL = Long.parseLong(System.getenv().getOrDefault("PDV_TOKENIZER_INITIAL_INTERVAL", "200"));
    private static final Double MULTIPLIER = Double.parseDouble(System.getenv().getOrDefault("PDV_TOKENIZER_MULTIPLIER", "2.0"));
    private static final Double RANDOMIZATION_FACTOR = Double.parseDouble(System.getenv().getOrDefault("PDV_TOKENIZER_RANDOMIZATION_FACTOR", "0.6"));
    private static final Integer MAX_RETRIES = Integer.parseInt(System.getenv().getOrDefault("PDV_TOKENIZER_MAX_RETRIES", "3"));

    private final PDVTokenizerService pdvTokenizerService;
    private final Retry retry;

    @Autowired
    public PDVTokenizerServiceRetryWrapperImpl(PDVTokenizerService pdvTokenizerService) {
        this.pdvTokenizerService = pdvTokenizerService;

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(MAX_RETRIES)
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(INITIAL_INTERVAL, MULTIPLIER, RANDOMIZATION_FACTOR))
                .retryOnException(e -> (e instanceof PDVTokenizerException tokenizerException) && tokenizerException.getStatusCode() == 429)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        this.retry = registry.retry("tokenizerRetry");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateTokenForFiscalCodeWithRetry(String fiscalCode) throws PDVTokenizerException, JsonProcessingException {
        CheckedFunction<String, String> function = Retry.decorateCheckedFunction(retry, pdvTokenizerService::generateTokenForFiscalCode);
        return runFunction(fiscalCode, function);
    }

    private String runFunction(String fiscalCode, CheckedFunction<String, String> function) throws PDVTokenizerException, JsonProcessingException {
        try {
            return function.apply(fiscalCode);
        } catch (Throwable e) {
            if (e instanceof PDVTokenizerException tokenizerException) {
                throw tokenizerException;
            }
            if (e instanceof JsonProcessingException jsonProcessingException) {
                throw jsonProcessingException;
            }
            throw new PDVTokenizerUnexpectedException(e);
        }
    }
}