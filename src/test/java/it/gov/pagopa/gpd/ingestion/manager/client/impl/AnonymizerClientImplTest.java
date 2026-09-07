package it.gov.pagopa.gpd.ingestion.manager.client.impl;

import it.gov.pagopa.gpd.ingestion.manager.exception.AnonymizerException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {AnonymizerClientImpl.class})
class AnonymizerClientImplTest {

    public static final String BODY = "Targa XXXXXX";
    @MockBean
    private HttpClient clientMock;
    @Autowired
    @InjectMocks
    private AnonymizerClientImpl sut;

    @BeforeAll
    static void setup() {
        MockedStatic<MDC> mockStatic = Mockito.mockStatic(MDC.class);
        mockStatic.when(() -> MDC.get("requestId"))
                .thenReturn("requestId");
    }

    @Test
    void anonymizeSuccess() throws IOException, InterruptedException, AnonymizerException {
        sut.anonymize(BODY);

        verify(clientMock).send(any(), any());
    }

    @Test
    void anonymizeFailThrowsIOException() throws IOException, InterruptedException {
        doThrow(IOException.class).when(clientMock).send(any(), any());

        assertThrows(AnonymizerException.class, () -> sut.anonymize(BODY));

        verify(clientMock).send(any(), any());
    }

    @Test
    void anonymizeFailThrowsInterruptedException() throws IOException, InterruptedException {
        doThrow(InterruptedException.class).when(clientMock).send(any(), any());

        assertThrows(AnonymizerException.class, () -> sut.anonymize(BODY));

        verify(clientMock).send(any(), any());
    }
}