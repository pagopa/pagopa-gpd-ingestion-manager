package it.gov.pagopa.gpd.ingestion.manager.client.impl;

import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {PDVTokenizerClientImpl.class})
class PDVTokenizerClientImplTest {

    public static final String PII_BODY = "piiBody";
    @MockBean
    private HttpClient clientMock;
    @Autowired
    @InjectMocks
    private PDVTokenizerClientImpl sut;

    @Test
    void createTokenSuccess() throws PDVTokenizerException, IOException, InterruptedException {
        sut.createToken(PII_BODY);

        verify(clientMock).send(any(), any());
    }

    @Test
    void createTokenFailThrowsIOException() throws IOException, InterruptedException {
        doThrow(IOException.class).when(clientMock).send(any(), any());

        assertThrows(PDVTokenizerException.class, () -> sut.createToken(PII_BODY));

        verify(clientMock).send(any(), any());
    }

    @Test
    void createTokenFailThrowsInterruptedException() throws IOException, InterruptedException {
        doThrow(InterruptedException.class).when(clientMock).send(any(), any());

        assertThrows(PDVTokenizerException.class, () -> sut.createToken(PII_BODY));

        verify(clientMock).send(any(), any());
    }
}