package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import it.gov.pagopa.gpd.ingestion.manager.exception.AnonymizerException;
import it.gov.pagopa.gpd.ingestion.manager.exception.AnonymizerUnexpectedException;
import it.gov.pagopa.gpd.ingestion.manager.service.AnonymizerService;
import it.gov.pagopa.gpd.ingestion.manager.service.AnonymizerServiceRetryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * {@inheritDoc}
 */
@Service
public class AnonymizerServiceRetryWrapperImpl implements AnonymizerServiceRetryWrapper {

    private final AnonymizerService anonymizerService;
    private final Retry retry;

    @Autowired
    public AnonymizerServiceRetryWrapperImpl(
            @Value("${anonymizer.retry.initial-interval}") Long initialIn,
            @Value("${anonymizer.retry.multiplier}") Double multiplier,
            @Value("${anonymizer.retry.randomization-factor}") Double randomizationFactor,
            @Value("${anonymizer.retry.max-retries}") Integer maxRetries,
            AnonymizerService anonymizerService
    ) {
        this.anonymizerService = anonymizerService;

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxRetries)
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(initialIn, multiplier, randomizationFactor))
                .retryOnException(e -> (e instanceof AnonymizerException anonymizerException) && anonymizerException.getStatusCode() == 429)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        this.retry = registry.retry("anonymizerRetry");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String anonymizeWithRetry(String body) throws AnonymizerException, JsonProcessingException {
        CheckedFunction<String, String> function = Retry.decorateCheckedFunction(retry, anonymizerService::anonymize);
        return runFunction(body, function);
    }

    private String runFunction(String body, CheckedFunction<String, String> function) throws AnonymizerException, JsonProcessingException {
        try {
            return function.apply(body);
        } catch (Throwable e) {
            if (e instanceof AnonymizerException anonymizerException) {
                throw anonymizerException;
            }
            if (e instanceof JsonProcessingException jsonProcessingException) {
                throw jsonProcessingException;
            }
            throw new AnonymizerUnexpectedException(e);
        }
    }
}