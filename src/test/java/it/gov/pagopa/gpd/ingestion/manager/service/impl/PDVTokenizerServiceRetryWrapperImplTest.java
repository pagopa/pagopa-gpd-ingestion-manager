package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerUnexpectedException;
import it.gov.pagopa.gpd.ingestion.manager.service.PDVTokenizerService;
import it.gov.pagopa.gpd.ingestion.manager.service.PDVTokenizerServiceRetryWrapper;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PDVTokenizerServiceRetryWrapperImplTest {

    private static final String FISCAL_CODE = "fiscalCode";
    private static final String TOKEN = "token";
    private static final int MAX_ATTEMPTS = 3;

    private PDVTokenizerService pdvTokenizerServiceMock;

    private PDVTokenizerServiceRetryWrapper sut;

    @BeforeEach
    void setUp() {
        pdvTokenizerServiceMock = mock(PDVTokenizerService.class);

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(MAX_ATTEMPTS)
                .retryOnException(e -> (e instanceof PDVTokenizerException tokenizerException) && tokenizerException.getStatusCode() == 429)
                .build();
        Retry retry = Retry.of("id", config);

        sut = Mockito.spy(new PDVTokenizerServiceRetryWrapperImpl(pdvTokenizerServiceMock, retry));
    }

    @Test
    void generateTokenForFiscalCodeRetryForPDVTokenizerExceptionWithStatus429() throws PDVTokenizerException, JsonProcessingException {
        String errMsg = "Error";
        doThrow(new PDVTokenizerException(errMsg, 429)).when(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.generateTokenForFiscalCodeWithRetry(FISCAL_CODE));

        assertNotNull(e);
        assertEquals(429, e.getStatusCode());
        assertEquals(errMsg, e.getMessage());

        verify(pdvTokenizerServiceMock, times(MAX_ATTEMPTS)).generateTokenForFiscalCode(anyString());
    }

    @Test
    void generateTokenForFiscalCodeNotRetryForPDVTokenizerExceptionWithoutStatus429() throws PDVTokenizerException, JsonProcessingException {
        String errMsg = "Error";
        doThrow(new PDVTokenizerException(errMsg, HttpStatus.SC_INTERNAL_SERVER_ERROR)).when(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.generateTokenForFiscalCodeWithRetry(FISCAL_CODE));

        assertNotNull(e);
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getStatusCode());
        assertEquals(errMsg, e.getMessage());

        verify(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());
    }

    @Test
    void generateTokenForFiscalCodeNotRetryForJsonProcessingException() throws PDVTokenizerException, JsonProcessingException {
        doThrow(JsonProcessingException.class).when(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());

        JsonProcessingException e = assertThrows(JsonProcessingException.class, () -> sut.generateTokenForFiscalCodeWithRetry(FISCAL_CODE));

        assertNotNull(e);
        verify(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());
    }

    @Test
    void generateTokenForFiscalCodeNotRetryForPDVTokenizerUnexpectedException() throws PDVTokenizerException, JsonProcessingException {
        doThrow(RuntimeException.class).when(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());

        PDVTokenizerUnexpectedException e = assertThrows(PDVTokenizerUnexpectedException.class, () -> sut.generateTokenForFiscalCodeWithRetry(FISCAL_CODE));

        assertNotNull(e);
        verify(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());
    }

    @Test
    void generateTokenForFiscalCodeSuccessNotRetry() throws PDVTokenizerException, JsonProcessingException {
        doReturn(TOKEN).when(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());

        String token = sut.generateTokenForFiscalCodeWithRetry(FISCAL_CODE);

        assertEquals(TOKEN, token);
        verify(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());
    }
}