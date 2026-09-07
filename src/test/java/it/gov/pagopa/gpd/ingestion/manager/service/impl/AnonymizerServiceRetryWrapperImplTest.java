package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.resilience4j.retry.Retry;
import it.gov.pagopa.gpd.ingestion.manager.exception.AnonymizerException;
import it.gov.pagopa.gpd.ingestion.manager.exception.AnonymizerUnexpectedException;
import it.gov.pagopa.gpd.ingestion.manager.service.AnonymizerService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {AnonymizerServiceRetryWrapperImpl.class})
class AnonymizerServiceRetryWrapperImplTest {

    private static final String TEXT = "Targa XXXXX";
    private static final String ANONYMIZED_TEXT = "Targa";
    private static final int MAX_ATTEMPTS = 3;

    @MockBean
    private AnonymizerService anonymizerServiceMock;
    @MockBean
    private Retry retry;

    @Autowired
    private AnonymizerServiceRetryWrapperImpl sut;

    @Test
    void anonymizeRetryForAnonymizerExceptionWithStatus429()
            throws AnonymizerException, JsonProcessingException {
        String errMsg = "Error";
        doThrow(new AnonymizerException(errMsg, 429))
                .when(anonymizerServiceMock)
                .anonymize(anyString());

        AnonymizerException e =
                assertThrows(
                        AnonymizerException.class,
                        () -> sut.anonymizeWithRetry(TEXT));

        assertNotNull(e);
        assertEquals(429, e.getStatusCode());
        assertEquals(errMsg, e.getMessage());

        verify(anonymizerServiceMock, times(MAX_ATTEMPTS)).anonymize(anyString());
    }

    @Test
    void anonymizeNotRetryForAnonymizerExceptionWithoutStatus429()
            throws AnonymizerException, JsonProcessingException {
        String errMsg = "Error";
        doThrow(new AnonymizerException(errMsg, HttpStatus.SC_INTERNAL_SERVER_ERROR))
                .when(anonymizerServiceMock)
                .anonymize(anyString());

        AnonymizerException e =
                assertThrows(
                        AnonymizerException.class,
                        () -> sut.anonymizeWithRetry(TEXT));

        assertNotNull(e);
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getStatusCode());
        assertEquals(errMsg, e.getMessage());

        verify(anonymizerServiceMock).anonymize(anyString());
    }

    @Test
    void anonymizeNotRetryForJsonProcessingException()
            throws AnonymizerException, JsonProcessingException {
        doThrow(JsonProcessingException.class)
                .when(anonymizerServiceMock)
                .anonymize(anyString());

        JsonProcessingException e =
                assertThrows(
                        JsonProcessingException.class,
                        () -> sut.anonymizeWithRetry(TEXT));

        assertNotNull(e);
        verify(anonymizerServiceMock).anonymize(anyString());
    }

    @Test
    void anonymizeNotRetryForAnonymizerUnexpectedException()
            throws AnonymizerException, JsonProcessingException {
        doThrow(RuntimeException.class)
                .when(anonymizerServiceMock)
                .anonymize(anyString());

        AnonymizerUnexpectedException e =
                assertThrows(
                        AnonymizerUnexpectedException.class,
                        () -> sut.anonymizeWithRetry(TEXT));

        assertNotNull(e);
        verify(anonymizerServiceMock).anonymize(anyString());
    }

    @Test
    void anonymizeSuccessNotRetry()
            throws AnonymizerException, JsonProcessingException {
        doReturn(ANONYMIZED_TEXT).when(anonymizerServiceMock).anonymize(anyString());

        String token = sut.anonymizeWithRetry(TEXT);

        assertEquals(ANONYMIZED_TEXT, token);
        verify(anonymizerServiceMock).anonymize(anyString());
    }
}
