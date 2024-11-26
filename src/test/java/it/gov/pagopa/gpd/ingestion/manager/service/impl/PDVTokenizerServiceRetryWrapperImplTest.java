package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
import it.gov.pagopa.gpd.ingestion.manager.service.PDVTokenizerService;
import it.gov.pagopa.gpd.ingestion.manager.service.PDVTokenizerServiceRetryWrapper;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {PDVTokenizerServiceRetryWrapperImpl.class})
class PDVTokenizerServiceRetryWrapperImplTest {

    private static final String FISCAL_CODE = "fiscalCode";
    private static final String TOKEN = "token";
    private static final int MAX_ATTEMPTS = 3;

    @MockBean
    private PDVTokenizerService pdvTokenizerServiceMock;

    @Autowired
    @InjectMocks
    private PDVTokenizerServiceRetryWrapper sut;

    @BeforeEach
    void setUp() {
        pdvTokenizerServiceMock = mock(PDVTokenizerService.class);

        sut = Mockito.spy(new PDVTokenizerServiceRetryWrapperImpl(pdvTokenizerServiceMock));
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
    void generateTokenForFiscalCodeSuccessNotRetry() throws PDVTokenizerException, JsonProcessingException {
        doReturn(TOKEN).when(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());

        String token = sut.generateTokenForFiscalCodeWithRetry(FISCAL_CODE);

        assertEquals(TOKEN, token);
        verify(pdvTokenizerServiceMock).generateTokenForFiscalCode(anyString());
    }
}