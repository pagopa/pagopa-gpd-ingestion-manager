package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.resilience4j.retry.annotation.Retry;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
import it.gov.pagopa.gpd.ingestion.manager.service.PDVTokenizerService;
import it.gov.pagopa.gpd.ingestion.manager.service.PDVTokenizerServiceRetryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * {@inheritDoc}
 */
@Service
public class PDVTokenizerServiceRetryWrapperImpl implements PDVTokenizerServiceRetryWrapper {

    private final PDVTokenizerService pdvTokenizerService;

    @Autowired
    PDVTokenizerServiceRetryWrapperImpl(PDVTokenizerService pdvTokenizerService) {
        this.pdvTokenizerService = pdvTokenizerService;
    }

    public PDVTokenizerServiceRetryWrapperImpl() {
        this.pdvTokenizerService = new PDVTokenizerServiceImpl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Retry(name = "tokenizerRetry")
    public String generateTokenForFiscalCodeWithRetry(String fiscalCode) throws PDVTokenizerException, JsonProcessingException {
        return pdvTokenizerService.generateTokenForFiscalCode(fiscalCode);
    }
}
