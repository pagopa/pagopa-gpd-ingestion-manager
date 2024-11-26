package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gpd.ingestion.manager.client.PDVTokenizerClient;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
import it.gov.pagopa.gpd.ingestion.manager.model.tokenizer.ErrorMessage;
import it.gov.pagopa.gpd.ingestion.manager.model.tokenizer.ErrorResponse;
import it.gov.pagopa.gpd.ingestion.manager.model.tokenizer.InvalidParam;
import it.gov.pagopa.gpd.ingestion.manager.model.tokenizer.TokenResource;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.net.http.HttpResponse;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {PDVTokenizerServiceImpl.class, ObjectMapper.class})
class PDVTokenizerServiceImplTest {

    private static final String TOKEN = "token";
    private static final String FISCAL_CODE = "fiscalCode";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    private HttpResponse<String> httpResponseMock;
    @MockBean
    private PDVTokenizerClient pdvTokenizerClientMock;
    @Autowired
    @InjectMocks
    private PDVTokenizerServiceImpl sut;

    @BeforeEach
    void setUp() {
        httpResponseMock = mock(HttpResponse.class);
        pdvTokenizerClientMock = mock(PDVTokenizerClient.class);
        sut = Mockito.spy(new PDVTokenizerServiceImpl(pdvTokenizerClientMock, objectMapper));
    }

    @Test
    void generateTokenForFiscalCodeSuccess() throws JsonProcessingException, PDVTokenizerException {
        TokenResource tokenResource = TokenResource.builder().token(TOKEN).build();
        String responseBody = objectMapper.writeValueAsString(tokenResource);

        doReturn(HttpStatus.SC_OK).when(httpResponseMock).statusCode();
        doReturn(responseBody).when(httpResponseMock).body();
        doReturn(httpResponseMock).when(pdvTokenizerClientMock).createToken(anyString());

        String token = sut.generateTokenForFiscalCode(FISCAL_CODE);

        assertNotNull(token);
        assertEquals(TOKEN, token);

        verify(pdvTokenizerClientMock).createToken(anyString());
    }

    @Test
    void generateTokenForFiscalCodeFailClientThrowsPDVTokenizerException() throws PDVTokenizerException {
        doThrow(PDVTokenizerException.class).when(pdvTokenizerClientMock).createToken(anyString());

        assertThrows(PDVTokenizerException.class, () -> sut.generateTokenForFiscalCode(FISCAL_CODE));

        verify(pdvTokenizerClientMock).createToken(anyString());
    }

    @Test
    void generateTokenForFiscalCodeFailResponse400() throws PDVTokenizerException, JsonProcessingException {
        ErrorResponse errorResponse = buildErrorResponse();
        String responseBody = objectMapper.writeValueAsString(errorResponse);

        doReturn(HttpStatus.SC_BAD_REQUEST).when(httpResponseMock).statusCode();
        doReturn(responseBody).when(httpResponseMock).body();
        doReturn(httpResponseMock).when(pdvTokenizerClientMock).createToken(anyString());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.generateTokenForFiscalCode(FISCAL_CODE));

        assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());

        verify(pdvTokenizerClientMock).createToken(anyString());
    }

    @Test
    void generateTokenForFiscalCodeFailResponse429() throws PDVTokenizerException, JsonProcessingException {
        ErrorMessage errorResponse = ErrorMessage.builder().message("Too Many Requests").build();
        String responseBody = objectMapper.writeValueAsString(errorResponse);

        doReturn(429).when(httpResponseMock).statusCode();
        doReturn(responseBody).when(httpResponseMock).body();
        doReturn(httpResponseMock).when(pdvTokenizerClientMock).createToken(anyString());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.generateTokenForFiscalCode(FISCAL_CODE));

        assertEquals(429, e.getStatusCode());

        verify(pdvTokenizerClientMock).createToken(anyString());
    }

    private ErrorResponse buildErrorResponse() {
        return ErrorResponse.builder()
                .title("Error")
                .detail("Error detail")
                .status(HttpStatus.SC_BAD_REQUEST)
                .invalidParams(Collections.singletonList(InvalidParam.builder()
                        .name("param name")
                        .reason("reason")
                        .build()))
                .instance("instance")
                .type("type")
                .build();
    }
}