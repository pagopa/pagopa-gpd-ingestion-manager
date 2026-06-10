package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gpd.ingestion.manager.client.impl.AnonymizerClientImpl;
import it.gov.pagopa.gpd.ingestion.manager.exception.AnonymizerException;
import it.gov.pagopa.gpd.ingestion.manager.model.AnonymizerModel;
import it.gov.pagopa.gpd.ingestion.manager.model.tokenizer.ErrorMessage;
import it.gov.pagopa.gpd.ingestion.manager.model.tokenizer.ErrorResponse;
import it.gov.pagopa.gpd.ingestion.manager.model.tokenizer.InvalidParam;
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

@SpringBootTest(classes = {AnonymizerServiceImpl.class, ObjectMapper.class})
class AnonymizerServiceImplTest {

    private static final String ANONYMIZED_TEXT = "Targa";
    private static final String TEXT = "Targa XXXXX";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    private HttpResponse<String> httpResponseMock;
    @MockBean
    private AnonymizerClientImpl anonymizerClientMock;
    @Autowired
    @InjectMocks
    private AnonymizerServiceImpl sut;

    @BeforeEach
    void setUp() {
        httpResponseMock = mock(HttpResponse.class);
        anonymizerClientMock = mock(AnonymizerClientImpl.class);
        sut = Mockito.spy(new AnonymizerServiceImpl(anonymizerClientMock, objectMapper));
    }

    @Test
    void anonymizeSuccess() throws JsonProcessingException, AnonymizerException {
        AnonymizerModel anonymizerModel = AnonymizerModel.builder().text(ANONYMIZED_TEXT).build();
        String responseBody = objectMapper.writeValueAsString(anonymizerModel);

        doReturn(HttpStatus.SC_OK).when(httpResponseMock).statusCode();
        doReturn(responseBody).when(httpResponseMock).body();
        doReturn(httpResponseMock).when(anonymizerClientMock).anonymize(anyString());

        String anonymized = sut.anonymize(TEXT);

        assertNotNull(anonymized);
        assertEquals(ANONYMIZED_TEXT, anonymized);

        verify(anonymizerClientMock).anonymize(anyString());
    }

    @Test
    void anonymizeFailClientThrowsAnonymizerException() throws AnonymizerException {
        doThrow(AnonymizerException.class).when(anonymizerClientMock).anonymize(anyString());

        assertThrows(AnonymizerException.class, () -> sut.anonymize(TEXT));

        verify(anonymizerClientMock).anonymize(anyString());
    }

    @Test
    void anonymizeFailResponse400() throws AnonymizerException, JsonProcessingException {
        ErrorResponse errorResponse = buildErrorResponse();
        String responseBody = objectMapper.writeValueAsString(errorResponse);

        doReturn(HttpStatus.SC_BAD_REQUEST).when(httpResponseMock).statusCode();
        doReturn(responseBody).when(httpResponseMock).body();
        doReturn(httpResponseMock).when(anonymizerClientMock).anonymize(anyString());

        AnonymizerException e = assertThrows(AnonymizerException.class, () -> sut.anonymize(TEXT));

        assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatusCode());

        verify(anonymizerClientMock).anonymize(anyString());
    }

    @Test
    void anonymizeFailResponse429() throws AnonymizerException, JsonProcessingException {
        ErrorMessage errorResponse = ErrorMessage.builder().message("Too Many Requests").build();
        String responseBody = objectMapper.writeValueAsString(errorResponse);

        doReturn(429).when(httpResponseMock).statusCode();
        doReturn(responseBody).when(httpResponseMock).body();
        doReturn(httpResponseMock).when(anonymizerClientMock).anonymize(anyString());

        AnonymizerException e = assertThrows(AnonymizerException.class, () -> sut.anonymize(TEXT));

        assertEquals(429, e.getStatusCode());

        verify(anonymizerClientMock).anonymize(anyString());
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