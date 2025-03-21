package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gpd.ingestion.manager.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.PaymentOption;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.PaymentPosition;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.Transfer;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.enumeration.PaymentOptionStatus;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.enumeration.PaymentPositionStatus;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.enumeration.TransferStatus;
import it.gov.pagopa.gpd.ingestion.manager.events.producer.impl.IngestedPaymentOptionProducerImpl;
import it.gov.pagopa.gpd.ingestion.manager.events.producer.impl.IngestedPaymentPositionProducerImpl;
import it.gov.pagopa.gpd.ingestion.manager.events.producer.impl.IngestedTransferProducerImpl;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
import it.gov.pagopa.gpd.ingestion.manager.service.PDVTokenizerServiceRetryWrapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {IngestionServiceImpl.class, ObjectMapper.class})
class IngestionServiceImplTest {
    public static final String HTTP_MESSAGE_ERROR = "an error occured";
    public static final String TOKENIZED_FISCAL_CODE = "tokenizedFiscalCode";
    private final String FISCAL_CODE = "AAAAAA00A00A000D";
    private final String INVALID_FISCAL_CODE = "invalidFiscalCode";
    @MockBean
    private PDVTokenizerServiceRetryWrapper pdvTokenizerServiceMock;
    @MockBean
    private IngestedPaymentPositionProducerImpl paymentPositionProducer;
    @MockBean
    private IngestedPaymentOptionProducerImpl paymentOptionProducer;
    @MockBean
    private IngestedTransferProducerImpl transferProducer;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @InjectMocks
    private IngestionServiceImpl sut;

    @Captor
    private ArgumentCaptor<DataCaptureMessage<PaymentPosition>> paymentPositionCaptor;

    @Captor
    private ArgumentCaptor<DataCaptureMessage<PaymentOption>> paymentOptionCaptor;

    @Captor
    private ArgumentCaptor<DataCaptureMessage<Transfer>> transferCaptor;

    // Test Ingestion Payment Position
    @Test
    void ingestPaymentPositionRunOk() throws PDVTokenizerException, JsonProcessingException {
        when(pdvTokenizerServiceMock.generateTokenForFiscalCodeWithRetry(FISCAL_CODE))
                .thenReturn(TOKENIZED_FISCAL_CODE);

        DataCaptureMessage<PaymentPosition> ppList = generateValidPaymentPosition(FISCAL_CODE, false);
        List<String> paymentPositionsItems =
                Collections.singletonList(objectMapper.writeValueAsString(ppList));

        sut =
                new IngestionServiceImpl(
                        objectMapper,
                        pdvTokenizerServiceMock,
                        paymentPositionProducer,
                        paymentOptionProducer,
                        transferProducer,
                        false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentPositions(paymentPositionsItems));

        verify(paymentPositionProducer).sendIngestedPaymentPosition(paymentPositionCaptor.capture());
        DataCaptureMessage<PaymentPosition> captured = paymentPositionCaptor.getValue();
        assertNull(captured.getBefore());
        assertEquals(TOKENIZED_FISCAL_CODE, captured.getAfter().getFiscalCode());
    }

    @Test
    void ingestPaymentPositionRunOkBothAfterAndBefore()
            throws PDVTokenizerException, JsonProcessingException {
        when(pdvTokenizerServiceMock.generateTokenForFiscalCodeWithRetry(FISCAL_CODE))
                .thenReturn(TOKENIZED_FISCAL_CODE);

        DataCaptureMessage<PaymentPosition> ppList = generateValidPaymentPosition(FISCAL_CODE, true);
        List<String> paymentPositionsItems =
                Collections.singletonList(objectMapper.writeValueAsString(ppList));

        sut =
                new IngestionServiceImpl(
                        objectMapper,
                        pdvTokenizerServiceMock,
                        paymentPositionProducer,
                        paymentOptionProducer,
                        transferProducer,
                        false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentPositions(paymentPositionsItems));

        verify(paymentPositionProducer).sendIngestedPaymentPosition(paymentPositionCaptor.capture());
        DataCaptureMessage<PaymentPosition> captured = paymentPositionCaptor.getValue();
        assertEquals(TOKENIZED_FISCAL_CODE, captured.getBefore().getFiscalCode());
        assertEquals(TOKENIZED_FISCAL_CODE, captured.getAfter().getFiscalCode());
    }

    @Test
    void ingestPaymentPositionRunInvalidFiscalCode()
            throws PDVTokenizerException, JsonProcessingException {
        DataCaptureMessage<PaymentPosition> ppList =
                generateValidPaymentPosition(INVALID_FISCAL_CODE, false);
        List<String> paymentPositionsItems =
                Collections.singletonList(objectMapper.writeValueAsString(ppList));

        sut =
                new IngestionServiceImpl(
                        objectMapper,
                        pdvTokenizerServiceMock,
                        paymentPositionProducer,
                        paymentOptionProducer,
                        transferProducer, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentPositions(paymentPositionsItems));

        verify(pdvTokenizerServiceMock, never()).generateTokenForFiscalCodeWithRetry(any());
        verify(paymentPositionProducer).sendIngestedPaymentPosition(paymentPositionCaptor.capture());
        DataCaptureMessage<PaymentPosition> captured = paymentPositionCaptor.getValue();
        assertNull(captured.getBefore());
        assertEquals(INVALID_FISCAL_CODE, captured.getAfter().getFiscalCode());
    }

    @Test
    void ingestPaymentPositionRunInvalidFiscalCodeBothAfterAndBefore()
            throws PDVTokenizerException, JsonProcessingException {
        DataCaptureMessage<PaymentPosition> ppList =
                generateValidPaymentPosition(INVALID_FISCAL_CODE, true);
        List<String> paymentPositionsItems =
                Collections.singletonList(objectMapper.writeValueAsString(ppList));

        sut =
                new IngestionServiceImpl(
                        objectMapper,
                        pdvTokenizerServiceMock,
                        paymentPositionProducer,
                        paymentOptionProducer,
                        transferProducer, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentPositions(paymentPositionsItems));

        verify(pdvTokenizerServiceMock, never()).generateTokenForFiscalCodeWithRetry(any());
        verify(paymentPositionProducer).sendIngestedPaymentPosition(paymentPositionCaptor.capture());
        DataCaptureMessage<PaymentPosition> captured = paymentPositionCaptor.getValue();
        assertEquals(INVALID_FISCAL_CODE, captured.getBefore().getFiscalCode());
        assertEquals(INVALID_FISCAL_CODE, captured.getAfter().getFiscalCode());
    }

    @Test
    void ingestPaymentPositionRunNullFiscalCode()
            throws PDVTokenizerException, JsonProcessingException {
        DataCaptureMessage<PaymentPosition> ppList = generateValidPaymentPosition(null, false);
        List<String> paymentPositionsItems =
                Collections.singletonList(objectMapper.writeValueAsString(ppList));

        sut =
                new IngestionServiceImpl(
                        objectMapper,
                        pdvTokenizerServiceMock,
                        paymentPositionProducer,
                        paymentOptionProducer,
                        transferProducer, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentPositions(paymentPositionsItems));

        verify(pdvTokenizerServiceMock, never()).generateTokenForFiscalCodeWithRetry(any());
        verify(paymentPositionProducer).sendIngestedPaymentPosition(paymentPositionCaptor.capture());
        DataCaptureMessage<PaymentPosition> captured = paymentPositionCaptor.getValue();
        assertNull(captured.getBefore());
        assertNull(captured.getAfter().getFiscalCode());
    }

    @Test
    void ingestPaymentPositionErrorTokenizingFiscalCodes()
            throws PDVTokenizerException, JsonProcessingException {
        when(pdvTokenizerServiceMock.generateTokenForFiscalCodeWithRetry(FISCAL_CODE))
                .thenThrow(
                        new PDVTokenizerException(
                                HTTP_MESSAGE_ERROR, org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR));

        sut =
                new IngestionServiceImpl(
                        objectMapper,
                        pdvTokenizerServiceMock,
                        paymentPositionProducer,
                        paymentOptionProducer,
                        transferProducer, false);

        DataCaptureMessage<PaymentPosition> ppList = generateValidPaymentPosition(FISCAL_CODE, false);
        List<String> paymentPositionsItems =
                Collections.singletonList(objectMapper.writeValueAsString(ppList));

        assertDoesNotThrow(() -> sut.ingestPaymentPositions(paymentPositionsItems));

        verify(paymentPositionProducer, never()).sendIngestedPaymentPosition(any());
    }

    @Test
    void ingestPaymentPositionRunOkBothAfterAndBeforeWithPlaceolhderOnPDvError()
            throws PDVTokenizerException, JsonProcessingException {
        when(pdvTokenizerServiceMock.generateTokenForFiscalCodeWithRetry(FISCAL_CODE))
                .thenThrow(new PDVTokenizerException("test", 500));

        DataCaptureMessage<PaymentPosition> ppList = generateValidPaymentPosition(FISCAL_CODE, true);
        List<String> paymentPositionsItems =
                Collections.singletonList(objectMapper.writeValueAsString(ppList));

        sut =
                new IngestionServiceImpl(
                        objectMapper,
                        pdvTokenizerServiceMock,
                        paymentPositionProducer,
                        paymentOptionProducer,
                        transferProducer,
                        true);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentPositions(paymentPositionsItems));

        verify(paymentPositionProducer).sendIngestedPaymentPosition(paymentPositionCaptor.capture());
        DataCaptureMessage<PaymentPosition> captured = paymentPositionCaptor.getValue();
        assertEquals("PDV_CF_TOKENIZER", captured.getBefore().getFiscalCode());
        assertEquals("PDV_CF_TOKENIZER", captured.getAfter().getFiscalCode());
    }

    @Test
    void ingestPaymentPositionRunNullMessage() throws PDVTokenizerException, JsonProcessingException {
        List<String> paymentPositionsItems = Collections.singletonList("");

        sut =
                new IngestionServiceImpl(
                        objectMapper,
                        pdvTokenizerServiceMock,
                        paymentPositionProducer,
                        paymentOptionProducer,
                        transferProducer, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentPositions(paymentPositionsItems));

        verify(pdvTokenizerServiceMock, never()).generateTokenForFiscalCodeWithRetry(any());
        verify(paymentPositionProducer, never()).sendIngestedPaymentPosition(any());
    }

    private DataCaptureMessage<PaymentPosition> generateValidPaymentPosition(
            String fiscalCode, boolean withBefore) {
        PaymentPosition pp =
                PaymentPosition.builder()
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

        return DataCaptureMessage.<PaymentPosition>builder()
                .before(withBefore ? pp : null)
                .after(pp)
                .op("c")
                .tsMs(0L)
                .tsNs(0L)
                .tsUs(0L)
                .build();
    }

    // Test Ingestion Payment Option
    @Test
    void ingestPaymentOptionRunOk() throws PDVTokenizerException, JsonProcessingException {
        when(pdvTokenizerServiceMock.generateTokenForFiscalCodeWithRetry(FISCAL_CODE))
                .thenReturn(TOKENIZED_FISCAL_CODE);

        DataCaptureMessage<PaymentOption> poList = generateValidPaymentOption(FISCAL_CODE, false);
        List<String> paymentPositionsItems =
                Collections.singletonList(objectMapper.writeValueAsString(poList));

        sut =
                new IngestionServiceImpl(
                        objectMapper,
                        pdvTokenizerServiceMock,
                        paymentPositionProducer,
                        paymentOptionProducer,
                        transferProducer,
                        false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentOptions(paymentPositionsItems));

        verify(paymentOptionProducer).sendIngestedPaymentOption(paymentOptionCaptor.capture());
        DataCaptureMessage<PaymentOption> captured = paymentOptionCaptor.getValue();
        assertNull(captured.getBefore());
        assertEquals(TOKENIZED_FISCAL_CODE, captured.getAfter().getFiscalCode());
    }

    @Test
    void ingestPaymentOptionRunOkBothAfterAndBefore()
            throws PDVTokenizerException, JsonProcessingException {
        when(pdvTokenizerServiceMock.generateTokenForFiscalCodeWithRetry(FISCAL_CODE))
                .thenReturn(TOKENIZED_FISCAL_CODE);

        DataCaptureMessage<PaymentOption> poList = generateValidPaymentOption(FISCAL_CODE, true);
        List<String> paymentPositionsItems =
                Collections.singletonList(objectMapper.writeValueAsString(poList));

        sut =
                new IngestionServiceImpl(
                        objectMapper,
                        pdvTokenizerServiceMock,
                        paymentPositionProducer,
                        paymentOptionProducer,
                        transferProducer,
                        false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentOptions(paymentPositionsItems));

        verify(paymentOptionProducer).sendIngestedPaymentOption(paymentOptionCaptor.capture());
        DataCaptureMessage<PaymentOption> captured = paymentOptionCaptor.getValue();
        assertEquals(TOKENIZED_FISCAL_CODE, captured.getBefore().getFiscalCode());
        assertEquals(TOKENIZED_FISCAL_CODE, captured.getAfter().getFiscalCode());
    }

    @Test
    void ingestPaymentOptionRunInvalidFiscalCode()
            throws PDVTokenizerException, JsonProcessingException {
        DataCaptureMessage<PaymentOption> poList =
                generateValidPaymentOption(INVALID_FISCAL_CODE, false);
        List<String> paymentPositionsItems =
                Collections.singletonList(objectMapper.writeValueAsString(poList));

        sut =
                new IngestionServiceImpl(
                        objectMapper,
                        pdvTokenizerServiceMock,
                        paymentPositionProducer,
                        paymentOptionProducer,
                        transferProducer, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentOptions(paymentPositionsItems));

        verify(pdvTokenizerServiceMock, never()).generateTokenForFiscalCodeWithRetry(any());
        verify(paymentOptionProducer).sendIngestedPaymentOption(paymentOptionCaptor.capture());
        DataCaptureMessage<PaymentOption> captured = paymentOptionCaptor.getValue();
        assertNull(captured.getBefore());
        assertEquals(INVALID_FISCAL_CODE, captured.getAfter().getFiscalCode());
    }

    @Test
    void ingestPaymentOptionRunInvalidFiscalCodeBothAfterAndBefore()
            throws PDVTokenizerException, JsonProcessingException {
        DataCaptureMessage<PaymentOption> poList =
                generateValidPaymentOption(INVALID_FISCAL_CODE, true);
        List<String> paymentPositionsItems =
                Collections.singletonList(objectMapper.writeValueAsString(poList));

        sut =
                new IngestionServiceImpl(
                        objectMapper,
                        pdvTokenizerServiceMock,
                        paymentPositionProducer,
                        paymentOptionProducer,
                        transferProducer, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentOptions(paymentPositionsItems));

        verify(pdvTokenizerServiceMock, never()).generateTokenForFiscalCodeWithRetry(any());
        verify(paymentOptionProducer).sendIngestedPaymentOption(paymentOptionCaptor.capture());
        DataCaptureMessage<PaymentOption> captured = paymentOptionCaptor.getValue();
        assertEquals(INVALID_FISCAL_CODE, captured.getBefore().getFiscalCode());
        assertEquals(INVALID_FISCAL_CODE, captured.getAfter().getFiscalCode());
    }

    @Test
    void ingestPaymentOptionRunNullFiscalCode()
            throws PDVTokenizerException, JsonProcessingException {
        DataCaptureMessage<PaymentOption> poList = generateValidPaymentOption(null, false);
        List<String> paymentPositionsItems =
                Collections.singletonList(objectMapper.writeValueAsString(poList));

        sut =
                new IngestionServiceImpl(
                        objectMapper,
                        pdvTokenizerServiceMock,
                        paymentPositionProducer,
                        paymentOptionProducer,
                        transferProducer, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentOptions(paymentPositionsItems));

        verify(pdvTokenizerServiceMock, never()).generateTokenForFiscalCodeWithRetry(any());
        verify(paymentOptionProducer).sendIngestedPaymentOption(paymentOptionCaptor.capture());
        DataCaptureMessage<PaymentOption> captured = paymentOptionCaptor.getValue();
        assertNull(captured.getBefore());
        assertNull(captured.getAfter().getFiscalCode());
    }

    @Test
    void ingestPaymentOptionErrorTokenizingFiscalCodes()
            throws PDVTokenizerException, JsonProcessingException {
        when(pdvTokenizerServiceMock.generateTokenForFiscalCodeWithRetry(FISCAL_CODE))
                .thenThrow(
                        new PDVTokenizerException(
                                HTTP_MESSAGE_ERROR, org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR));

        sut =
                new IngestionServiceImpl(
                        objectMapper,
                        pdvTokenizerServiceMock,
                        paymentPositionProducer,
                        paymentOptionProducer,
                        transferProducer, false);

        DataCaptureMessage<PaymentOption> poList = generateValidPaymentOption(FISCAL_CODE, false);
        List<String> paymentOptionsItems =
                Collections.singletonList(objectMapper.writeValueAsString(poList));

        assertDoesNotThrow(() -> sut.ingestPaymentOptions(paymentOptionsItems));

        verify(paymentOptionProducer, never()).sendIngestedPaymentOption(any());
    }

    @Test
    void ingestPaymentOptionRunOkBothAfterAndBeforeWithPlaceholderOnPDvError()
            throws PDVTokenizerException, JsonProcessingException {
        when(pdvTokenizerServiceMock.generateTokenForFiscalCodeWithRetry(FISCAL_CODE))
                .thenThrow(new PDVTokenizerException("test", 500));

        DataCaptureMessage<PaymentOption> poList = generateValidPaymentOption(FISCAL_CODE, true);
        List<String> paymentOptionsItems =
                Collections.singletonList(objectMapper.writeValueAsString(poList));

        sut =
                new IngestionServiceImpl(
                        objectMapper,
                        pdvTokenizerServiceMock,
                        paymentPositionProducer,
                        paymentOptionProducer,
                        transferProducer,
                        true);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentOptions(paymentOptionsItems));

        verify(paymentOptionProducer).sendIngestedPaymentOption(paymentOptionCaptor.capture());
        DataCaptureMessage<PaymentOption> captured = paymentOptionCaptor.getValue();
        assertEquals("PDV_CF_TOKENIZER", captured.getBefore().getFiscalCode());
        assertEquals("PDV_CF_TOKENIZER", captured.getAfter().getFiscalCode());
    }


    @Test
    void ingestPaymentOptionRunNullMessage() {
        List<String> paymentOptionsItems = Collections.singletonList("");

        sut =
                new IngestionServiceImpl(
                        objectMapper,
                        pdvTokenizerServiceMock,
                        paymentPositionProducer,
                        paymentOptionProducer,
                        transferProducer, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentOptions(paymentOptionsItems));

        verify(paymentOptionProducer, never()).sendIngestedPaymentOption(any());
    }

    private DataCaptureMessage<PaymentOption> generateValidPaymentOption(String fiscalCode, Boolean withBefore) {
        PaymentOption pp =
                PaymentOption.builder()
                        .id(0)
                        .paymentPositionId(0)
                        .amount(0)
                        .description("description")
                        .dueDate(new Date().getTime())
                        .fee(0)
                        .flowReportingId("flowReportingId")
                        .receiptId("receiptId")
                        .insertedDate(new Date().getTime())
                        .isPartialPayment(true)
                        .iuv("iuv")
                        .lastUpdateDate(new Date().getTime())
                        .organizationFiscalCode("organizationFiscalCode")
                        .status(PaymentOptionStatus.PO_PAID)
                        .paymentDate(new Date().getTime())
                        .paymentMethod("paymentMethod")
                        .pspCompany("pspCompany")
                        .reportingDate(new Date().getTime())
                        .retentionDate(new Date().getTime())
                        .notificationFee(0)
                        .lastUpdatedDateNotificationFee(0L)
                        .fiscalCode(fiscalCode != null ? fiscalCode : null)
                        .postalCode("postalCode")
                        .province("province")
                        .region("region")
                        .type("type")
                        .build();

        return DataCaptureMessage.<PaymentOption>builder()
                .before(withBefore ? pp : null)
                .after(pp)
                .op("c")
                .tsMs(0L)
                .tsNs(0L)
                .tsUs(0L)
                .build();
    }

    // Test Ingestion Transfer
    @Test
    void ingestTransferRunOk() throws JsonProcessingException {
        DataCaptureMessage<Transfer> tr = generateValidTransfer();
        List<String> transferItems = Collections.singletonList(objectMapper.writeValueAsString(tr));

        sut =
                new IngestionServiceImpl(
                        objectMapper,
                        pdvTokenizerServiceMock,
                        paymentPositionProducer,
                        paymentOptionProducer,
                        transferProducer, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestTransfers(transferItems));

        verify(transferProducer).sendIngestedTransfer(transferCaptor.capture());
        DataCaptureMessage<Transfer> captured = transferCaptor.getValue();
        assertNull(captured.getBefore());
        assertEquals(tr.getAfter().getId(), captured.getAfter().getId());
    }

    @Test
    void ingestTransferRunNullMessage() {
        List<String> transferItems = Collections.singletonList("");

        sut =
                new IngestionServiceImpl(
                        objectMapper,
                        pdvTokenizerServiceMock,
                        paymentPositionProducer,
                        paymentOptionProducer,
                        transferProducer, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestTransfers(transferItems));

        verify(transferProducer, never()).sendIngestedTransfer(any());
    }

    private DataCaptureMessage<Transfer> generateValidTransfer() {
        Transfer pp =
                Transfer.builder()
                        .id(0)
                        .amount(0)
                        .category("category")
                        .transferId("transferId")
                        .insertedDate(new Date().getTime())
                        .iuv("iuv")
                        .lastUpdateDate(new Date().getTime())
                        .organizationFiscalCode("organizationFiscalCode")
                        .status(TransferStatus.T_REPORTED)
                        .paymentOptionId(0)
                        .build();

        return DataCaptureMessage.<Transfer>builder()
                .before(null)
                .after(pp)
                .op("c")
                .tsMs(0L)
                .tsNs(0L)
                .tsUs(0L)
                .build();
    }
}
