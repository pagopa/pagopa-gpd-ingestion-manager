package it.gov.pagopa.gpd.ingestion.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.OutputBinding;
import it.gov.pagopa.gpd.ingestion.manager.entity.PaymentPosition;
import it.gov.pagopa.gpd.ingestion.manager.entity.enumeration.PaymentPositionStatus;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
import it.gov.pagopa.gpd.ingestion.manager.service.PDVTokenizerServiceRetryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
public class PaymentPositionProcessorTest {
    public static final String HTTP_MESSAGE_ERROR = "an error occured";

    private final String FISCAL_CODE = "AAAAAA00A00A000D";
    private final String INVALID_FISCAL_CODE = "invalidFiscalCode";
    private final String TOKENIZED_FISCAL_CODE = "tokenizedFiscalCode";

    private PaymentPositionProcessor function;

    @Mock
    private ExecutionContext context;
    @Mock
    private PDVTokenizerServiceRetryWrapper pdvTokenizerServiceMock;

    @Captor
    private ArgumentCaptor<List<PaymentPosition>> paymentPositionCaptor;

    @Test
    void runOk() throws PDVTokenizerException, JsonProcessingException {
        when(pdvTokenizerServiceMock.generateTokenForFiscalCodeWithRetry(FISCAL_CODE)).thenReturn(TOKENIZED_FISCAL_CODE);

        List<PaymentPosition> paymentPositionsItems = new ArrayList<>();
        paymentPositionsItems.add(generateValidPaymentPosition(FISCAL_CODE));

        @SuppressWarnings("unchecked")
        OutputBinding<List<PaymentPosition>> documentdb = (OutputBinding<List<PaymentPosition>>) spy(OutputBinding.class);

        function = new PaymentPositionProcessor(pdvTokenizerServiceMock);

        // test execution
        assertDoesNotThrow(() -> function.processPaymentPosition(paymentPositionsItems, documentdb, context));

        verify(documentdb).setValue(paymentPositionCaptor.capture());
        PaymentPosition captured = paymentPositionCaptor.getValue().get(0);
        assertEquals(TOKENIZED_FISCAL_CODE, captured.getFiscalCode());
    }

    @Test
    void runInvalidFiscalCode() throws PDVTokenizerException, JsonProcessingException {
        List<PaymentPosition> paymentPositionsItems = new ArrayList<>();
        paymentPositionsItems.add(generateValidPaymentPosition(INVALID_FISCAL_CODE));

        @SuppressWarnings("unchecked")
        OutputBinding<List<PaymentPosition>> documentdb = (OutputBinding<List<PaymentPosition>>) spy(OutputBinding.class);

        function = new PaymentPositionProcessor(pdvTokenizerServiceMock);

        // test execution
        assertDoesNotThrow(() -> function.processPaymentPosition(paymentPositionsItems, documentdb, context));

        verify(pdvTokenizerServiceMock, never()).generateTokenForFiscalCodeWithRetry(any());
        verify(documentdb).setValue(paymentPositionCaptor.capture());
        PaymentPosition captured = paymentPositionCaptor.getValue().get(0);
        assertEquals(INVALID_FISCAL_CODE, captured.getFiscalCode());
    }

    @Test
    void errorTokenizingFiscalCodes() throws PDVTokenizerException, JsonProcessingException {
        when(pdvTokenizerServiceMock.generateTokenForFiscalCodeWithRetry(FISCAL_CODE))
                .thenThrow(new PDVTokenizerException(HTTP_MESSAGE_ERROR, org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR));

        function = new PaymentPositionProcessor(pdvTokenizerServiceMock);

        List<PaymentPosition> paymentPositionsItems = new ArrayList<>();
        paymentPositionsItems.add(generateValidPaymentPosition(FISCAL_CODE));

        @SuppressWarnings("unchecked")
        OutputBinding<List<PaymentPosition>> documentdb = (OutputBinding<List<PaymentPosition>>) spy(OutputBinding.class);

        // test execution
        assertDoesNotThrow(() -> function.processPaymentPosition(paymentPositionsItems, documentdb, context));

        verify(documentdb, never()).setValue(any());
    }

    private PaymentPosition generateValidPaymentPosition(String fiscalCode){

        return PaymentPosition.builder()
                .id(0)
                .iupd("iupd")
                .fiscalCode(fiscalCode)
                .postalCode("postalCode")
                .province("province")
                .maxDueDate(new Date().getTime())
                .minDueDate(new Date().getTime())
                .organizationFiscalCode("orgFiscalCode")
                .companyName("companyName")
                .publishDate(new Date().getTime())
                .region("region")
                .status(PaymentPositionStatus.VALID)
                .type("type")
                .validityDate(new Date().getTime())
                .switchToExpired(false)
                .paymentDate(new Date().getTime())
                .lastUpdateDate(new Date().getTime())
                .insertedDate(new Date().getTime())
                .build();
    }
}
