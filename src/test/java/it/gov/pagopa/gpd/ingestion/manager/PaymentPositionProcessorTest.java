package it.gov.pagopa.gpd.ingestion.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import it.gov.pagopa.gpd.ingestion.manager.entity.PaymentPosition;
import it.gov.pagopa.gpd.ingestion.manager.entity.enumeration.PaymentPositionStatus;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
import it.gov.pagopa.gpd.ingestion.manager.model.DataCapturePaymentPosition;
import it.gov.pagopa.gpd.ingestion.manager.service.PDVTokenizerServiceRetryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private ArgumentCaptor<List<DataCapturePaymentPosition>> paymentPositionCaptor;

    @Test
    void runOk() throws PDVTokenizerException, JsonProcessingException {
        when(pdvTokenizerServiceMock.generateTokenForFiscalCodeWithRetry(FISCAL_CODE)).thenReturn(TOKENIZED_FISCAL_CODE);

        List<DataCapturePaymentPosition> paymentPositionsItems = new ArrayList<>();
        paymentPositionsItems.add(generateValidPaymentPosition(FISCAL_CODE, false));

        @SuppressWarnings("unchecked")
        OutputBinding<List<DataCapturePaymentPosition>> documentdb = (OutputBinding<List<DataCapturePaymentPosition>>) spy(OutputBinding.class);

        function = new PaymentPositionProcessor(pdvTokenizerServiceMock);

        // test execution
        assertDoesNotThrow(() -> function.processPaymentPosition(paymentPositionsItems, documentdb, context));

        verify(documentdb).setValue(paymentPositionCaptor.capture());
        DataCapturePaymentPosition captured = paymentPositionCaptor.getValue().get(0);
        assertNull(captured.getBefore());
        assertEquals(TOKENIZED_FISCAL_CODE, captured.getAfter().getFiscalCode());
    }

    @Test
    void runOkBothAfterAndBefore() throws PDVTokenizerException, JsonProcessingException {
        when(pdvTokenizerServiceMock.generateTokenForFiscalCodeWithRetry(FISCAL_CODE)).thenReturn(TOKENIZED_FISCAL_CODE);

        List<DataCapturePaymentPosition> paymentPositionsItems = new ArrayList<>();
        paymentPositionsItems.add(generateValidPaymentPosition(FISCAL_CODE, true));

        @SuppressWarnings("unchecked")
        OutputBinding<List<DataCapturePaymentPosition>> documentdb = (OutputBinding<List<DataCapturePaymentPosition>>) spy(OutputBinding.class);

        function = new PaymentPositionProcessor(pdvTokenizerServiceMock);

        // test execution
        assertDoesNotThrow(() -> function.processPaymentPosition(paymentPositionsItems, documentdb, context));

        verify(documentdb).setValue(paymentPositionCaptor.capture());
        DataCapturePaymentPosition captured = paymentPositionCaptor.getValue().get(0);
        assertEquals(TOKENIZED_FISCAL_CODE, captured.getBefore().getFiscalCode());
        assertEquals(TOKENIZED_FISCAL_CODE, captured.getAfter().getFiscalCode());
    }

    @Test
    void runInvalidFiscalCode() throws PDVTokenizerException, JsonProcessingException {
        List<DataCapturePaymentPosition> paymentPositionsItems = new ArrayList<>();
        paymentPositionsItems.add(generateValidPaymentPosition(INVALID_FISCAL_CODE, false));

        @SuppressWarnings("unchecked")
        OutputBinding<List<DataCapturePaymentPosition>> documentdb = (OutputBinding<List<DataCapturePaymentPosition>>) spy(OutputBinding.class);

        function = new PaymentPositionProcessor(pdvTokenizerServiceMock);

        // test execution
        assertDoesNotThrow(() -> function.processPaymentPosition(paymentPositionsItems, documentdb, context));

        verify(pdvTokenizerServiceMock, never()).generateTokenForFiscalCodeWithRetry(any());
        verify(documentdb).setValue(paymentPositionCaptor.capture());
        DataCapturePaymentPosition captured = paymentPositionCaptor.getValue().get(0);
        assertNull(captured.getBefore());
        assertEquals(INVALID_FISCAL_CODE, captured.getAfter().getFiscalCode());
    }

    @Test
    void runInvalidFiscalCodeBothAfterAndBefore() throws PDVTokenizerException, JsonProcessingException {
        List<DataCapturePaymentPosition> paymentPositionsItems = new ArrayList<>();
        paymentPositionsItems.add(generateValidPaymentPosition(INVALID_FISCAL_CODE, true));

        @SuppressWarnings("unchecked")
        OutputBinding<List<DataCapturePaymentPosition>> documentdb = (OutputBinding<List<DataCapturePaymentPosition>>) spy(OutputBinding.class);

        function = new PaymentPositionProcessor(pdvTokenizerServiceMock);

        // test execution
        assertDoesNotThrow(() -> function.processPaymentPosition(paymentPositionsItems, documentdb, context));

        verify(pdvTokenizerServiceMock, never()).generateTokenForFiscalCodeWithRetry(any());
        verify(documentdb).setValue(paymentPositionCaptor.capture());
        DataCapturePaymentPosition captured = paymentPositionCaptor.getValue().get(0);
        assertEquals(INVALID_FISCAL_CODE, captured.getBefore().getFiscalCode());
        assertEquals(INVALID_FISCAL_CODE, captured.getAfter().getFiscalCode());
    }

    @Test
    void errorTokenizingFiscalCodes() throws PDVTokenizerException, JsonProcessingException {
        when(pdvTokenizerServiceMock.generateTokenForFiscalCodeWithRetry(FISCAL_CODE))
                .thenThrow(new PDVTokenizerException(HTTP_MESSAGE_ERROR, org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR));

        function = new PaymentPositionProcessor(pdvTokenizerServiceMock);

        List<DataCapturePaymentPosition> paymentPositionsItems = new ArrayList<>();
        paymentPositionsItems.add(generateValidPaymentPosition(FISCAL_CODE, false));

        @SuppressWarnings("unchecked")
        OutputBinding<List<DataCapturePaymentPosition>> documentdb = (OutputBinding<List<DataCapturePaymentPosition>>) spy(OutputBinding.class);

        // test execution
        assertDoesNotThrow(() -> function.processPaymentPosition(paymentPositionsItems, documentdb, context));

        verify(documentdb, never()).setValue(any());
    }

    private DataCapturePaymentPosition generateValidPaymentPosition(String fiscalCode, boolean withBefore) {
        PaymentPosition pp = PaymentPosition.builder()
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
                .lastUpdatedDate(new Date().getTime())
                .insertedDate(new Date().getTime())
                .build();

        return DataCapturePaymentPosition.builder()
                .before(withBefore ? pp : null)
                .after(pp)
                .op("c")
                .tsMs(0L)
                .tsNs(0L)
                .tsUs(0L)
                .build();
    }
}
