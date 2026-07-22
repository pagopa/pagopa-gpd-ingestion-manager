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
import it.gov.pagopa.gpd.ingestion.manager.exception.AnonymizerException;
import it.gov.pagopa.gpd.ingestion.manager.exception.AppException;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
import it.gov.pagopa.gpd.ingestion.manager.service.AnonymizerServiceRetryWrapper;
import it.gov.pagopa.gpd.ingestion.manager.service.PDVTokenizerServiceRetryWrapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {IngestionServiceImpl.class, ObjectMapper.class})
class IngestionServiceImplTest {
    public static final String HTTP_MESSAGE_ERROR = "an error occured";
    public static final String TOKENIZED_FISCAL_CODE = "tokenizedFiscalCode";
    public static final String REMITTANCE_INFORMATION = "remittanceInformation";
    public static final String ANONYMIZED_REMITTANCE_INFORMATION = "anonymizedRemittanceInformation";
    public static final long DATE = LocalDate.of(2026, Month.JANUARY, 1)
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli();
    private final String FISCAL_CODE = "AAAAAA00A00A000D";
    private final String INVALID_FISCAL_CODE = "invalidFiscalCode";
    @MockBean
    private PDVTokenizerServiceRetryWrapper pdvTokenizerServiceMock;
    @MockBean
    private AnonymizerServiceRetryWrapper anonimizerServiceMock;
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
    void ingestPaymentPositionRunOk() {
        DataCaptureMessage<PaymentPosition> pp = generateValidPaymentPosition(false);

        sut = new IngestionServiceImpl(
                objectMapper,
                pdvTokenizerServiceMock,
                anonimizerServiceMock,
                paymentPositionProducer,
                paymentOptionProducer,
                transferProducer,
                false, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentPosition(getMessage(objectMapper.writeValueAsString(pp))));

        verify(paymentPositionProducer).sendIngestedPaymentPosition(paymentPositionCaptor.capture());
        DataCaptureMessage<PaymentPosition> captured = paymentPositionCaptor.getValue();
        assertNull(captured.getBefore());
        assertNotNull(captured.getAfter());
    }

    @Test
    void ingestPaymentPositionRunOkBothAfterAndBefore() {
        DataCaptureMessage<PaymentPosition> pp = generateValidPaymentPosition(true);

        sut = new IngestionServiceImpl(
                objectMapper,
                pdvTokenizerServiceMock,
                anonimizerServiceMock,
                paymentPositionProducer,
                paymentOptionProducer,
                transferProducer,
                false, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentPosition(getMessage(objectMapper.writeValueAsString(pp))));

        verify(paymentPositionProducer).sendIngestedPaymentPosition(paymentPositionCaptor.capture());
        DataCaptureMessage<PaymentPosition> captured = paymentPositionCaptor.getValue();
        assertNotNull(captured.getBefore());
        assertNotNull(captured.getAfter());
    }

    @Test
    void ingestPaymentPositionRunTombstoneMessage() {
        sut = new IngestionServiceImpl(
                objectMapper,
                pdvTokenizerServiceMock,
                anonimizerServiceMock,
                paymentPositionProducer,
                paymentOptionProducer,
                transferProducer,
                false, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentPosition(getMessage("")));

        verify(paymentPositionProducer, never()).sendIngestedPaymentPosition(any());
    }

    private DataCaptureMessage<PaymentPosition> generateValidPaymentPosition(boolean withBefore) {
        PaymentPosition pp =
                PaymentPosition.builder()
                        .id(0)
                        .iupd("iupd")
                        .maxDueDate(DATE)
                        .minDueDate(DATE)
                        .organizationFiscalCode("orgFiscalCode")
                        .companyName("companyName")
                        .publishDate(DATE)
                        .status(PaymentPositionStatus.VALID.name())
                        .paymentDate(DATE)
                        .lastUpdatedDate(DATE)
                        .insertedDate(DATE)
                        .pull(false)
                        .payStandIn(false)
                        .serviceType("GPD")
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

        DataCaptureMessage<PaymentOption> po = generateValidPaymentOption(FISCAL_CODE, false);

        sut = new IngestionServiceImpl(
                objectMapper,
                pdvTokenizerServiceMock,
                anonimizerServiceMock,
                paymentPositionProducer,
                paymentOptionProducer,
                transferProducer,
                false, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentOption(getMessage(objectMapper.writeValueAsString(po))));

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

        DataCaptureMessage<PaymentOption> po = generateValidPaymentOption(FISCAL_CODE, true);

        sut = new IngestionServiceImpl(
                objectMapper,
                pdvTokenizerServiceMock,
                anonimizerServiceMock,
                paymentPositionProducer,
                paymentOptionProducer,
                transferProducer,
                false, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentOption(getMessage(objectMapper.writeValueAsString(po))));

        verify(paymentOptionProducer).sendIngestedPaymentOption(paymentOptionCaptor.capture());
        DataCaptureMessage<PaymentOption> captured = paymentOptionCaptor.getValue();
        assertEquals(TOKENIZED_FISCAL_CODE, captured.getBefore().getFiscalCode());
        assertEquals(TOKENIZED_FISCAL_CODE, captured.getAfter().getFiscalCode());
    }

    @Test
    void ingestPaymentOptionRunFiscalCodeLowerCase()
            throws PDVTokenizerException, JsonProcessingException {
        when(pdvTokenizerServiceMock.generateTokenForFiscalCodeWithRetry(FISCAL_CODE.toLowerCase()))
                .thenReturn(TOKENIZED_FISCAL_CODE);

        DataCaptureMessage<PaymentOption> po =
                generateValidPaymentOption(FISCAL_CODE.toLowerCase(), false);

        sut =
                new IngestionServiceImpl(
                        objectMapper,
                        pdvTokenizerServiceMock,
                        anonimizerServiceMock,
                        paymentPositionProducer,
                        paymentOptionProducer,
                        transferProducer, false, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentOption(getMessage(objectMapper.writeValueAsString(po))));

        verify(pdvTokenizerServiceMock, times(1)).generateTokenForFiscalCodeWithRetry(FISCAL_CODE.toLowerCase());
        verify(paymentOptionProducer).sendIngestedPaymentOption(paymentOptionCaptor.capture());
        DataCaptureMessage<PaymentOption> captured = paymentOptionCaptor.getValue();
        assertNull(captured.getBefore());
        assertEquals(TOKENIZED_FISCAL_CODE, captured.getAfter().getFiscalCode());
    }

    @Test
    void ingestPaymentOptionRunInvalidFiscalCode()
            throws PDVTokenizerException, JsonProcessingException {
        DataCaptureMessage<PaymentOption> po =
                generateValidPaymentOption(INVALID_FISCAL_CODE, false);

        sut = new IngestionServiceImpl(
                objectMapper,
                pdvTokenizerServiceMock,
                anonimizerServiceMock,
                paymentPositionProducer,
                paymentOptionProducer,
                transferProducer,
                false, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentOption(getMessage(objectMapper.writeValueAsString(po))));

        verify(pdvTokenizerServiceMock, never()).generateTokenForFiscalCodeWithRetry(any());
        verify(paymentOptionProducer).sendIngestedPaymentOption(paymentOptionCaptor.capture());
        DataCaptureMessage<PaymentOption> captured = paymentOptionCaptor.getValue();
        assertNull(captured.getBefore());
        assertEquals(INVALID_FISCAL_CODE, captured.getAfter().getFiscalCode());
    }

    @Test
    void ingestPaymentOptionRunInvalidFiscalCodeBothAfterAndBefore()
            throws PDVTokenizerException, JsonProcessingException {
        DataCaptureMessage<PaymentOption> po =
                generateValidPaymentOption(INVALID_FISCAL_CODE, true);

        sut = new IngestionServiceImpl(
                objectMapper,
                pdvTokenizerServiceMock,
                anonimizerServiceMock,
                paymentPositionProducer,
                paymentOptionProducer,
                transferProducer,
                false, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentOption(getMessage(objectMapper.writeValueAsString(po))));

        verify(pdvTokenizerServiceMock, never()).generateTokenForFiscalCodeWithRetry(any());
        verify(paymentOptionProducer).sendIngestedPaymentOption(paymentOptionCaptor.capture());
        DataCaptureMessage<PaymentOption> captured = paymentOptionCaptor.getValue();
        assertEquals(INVALID_FISCAL_CODE, captured.getBefore().getFiscalCode());
        assertEquals(INVALID_FISCAL_CODE, captured.getAfter().getFiscalCode());
    }

    @Test
    void ingestPaymentOptionRunNullFiscalCode()
            throws PDVTokenizerException, JsonProcessingException {
        DataCaptureMessage<PaymentOption> po = generateValidPaymentOption(null, false);

        sut = new IngestionServiceImpl(
                objectMapper,
                pdvTokenizerServiceMock,
                anonimizerServiceMock,
                paymentPositionProducer,
                paymentOptionProducer,
                transferProducer,
                false, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentOption(getMessage(objectMapper.writeValueAsString(po))));

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

        sut = new IngestionServiceImpl(
                objectMapper,
                pdvTokenizerServiceMock,
                anonimizerServiceMock,
                paymentPositionProducer,
                paymentOptionProducer,
                transferProducer,
                false, false);

        DataCaptureMessage<PaymentOption> po = generateValidPaymentOption(FISCAL_CODE, false);

        Message<String> message = getMessage(objectMapper.writeValueAsString(po));
        assertThrows(AppException.class, () -> sut.ingestPaymentOption(message));

        verify(paymentOptionProducer, never()).sendIngestedPaymentOption(any());
    }

    @Test
    void ingestPaymentOptionRunOkBothAfterAndBeforeWithPlaceholderOnPDvError()
            throws PDVTokenizerException, JsonProcessingException {
        when(pdvTokenizerServiceMock.generateTokenForFiscalCodeWithRetry(FISCAL_CODE))
                .thenThrow(new PDVTokenizerException("test", 500));

        DataCaptureMessage<PaymentOption> po = generateValidPaymentOption(FISCAL_CODE, true);

        sut = new IngestionServiceImpl(
                objectMapper,
                pdvTokenizerServiceMock,
                anonimizerServiceMock,
                paymentPositionProducer,
                paymentOptionProducer,
                transferProducer,
                true, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentOption(getMessage(objectMapper.writeValueAsString(po))));

        verify(paymentOptionProducer).sendIngestedPaymentOption(paymentOptionCaptor.capture());
        DataCaptureMessage<PaymentOption> captured = paymentOptionCaptor.getValue();
        assertEquals("PDV_CF_TOKENIZER", captured.getBefore().getFiscalCode());
        assertEquals("PDV_CF_TOKENIZER", captured.getAfter().getFiscalCode());
    }


    @Test
    void ingestPaymentOptionRunTombstoneMessage() throws PDVTokenizerException, JsonProcessingException {
        sut = new IngestionServiceImpl(
                objectMapper,
                pdvTokenizerServiceMock,
                anonimizerServiceMock,
                paymentPositionProducer,
                paymentOptionProducer,
                transferProducer,
                false, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestPaymentOption(getMessage("")));
        verify(pdvTokenizerServiceMock, never()).generateTokenForFiscalCodeWithRetry(any());

        verify(paymentOptionProducer, never()).sendIngestedPaymentOption(any());
    }

    private DataCaptureMessage<PaymentOption> generateValidPaymentOption(String fiscalCode, Boolean withBefore) {
        PaymentOption pp =
                PaymentOption.builder()
                        .id(0)
                        .paymentPositionId(0)
                        .amount(0)
                        .description("description")
                        .dueDate(DATE)
                        .fee(0)
                        .flowReportingId("flowReportingId")
                        .insertedDate(DATE)
                        .isPartialPayment(true)
                        .iuv("iuv")
                        .nav("nav")
                        .lastUpdateDate(DATE)
                        .organizationFiscalCode("organizationFiscalCode")
                        .status(PaymentOptionStatus.PO_PAID.name())
                        .retentionDate(DATE)
                        .notificationFee(0)
                        .lastUpdatedDateNotificationFee(0L)
                        .fiscalCode(fiscalCode)
                        .type("type")
                        .region("region")
                        .sendSync(false)
                        .pspCode("pspCode")
                        .switchToExpired(false)
                        .validityDate(DATE)
                        .paymentPlanId("paymentPlanId-0")
                        .paymentOptionDescription("paymentOptionDescription")
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
    void ingestTransferRunOk() throws JsonProcessingException, AnonymizerException {
        when(anonimizerServiceMock.anonymizeWithRetry(REMITTANCE_INFORMATION))
                .thenReturn(ANONYMIZED_REMITTANCE_INFORMATION);
        DataCaptureMessage<Transfer> tr = generateValidTransfer(false);

        sut = new IngestionServiceImpl(
                objectMapper,
                pdvTokenizerServiceMock,
                anonimizerServiceMock,
                paymentPositionProducer,
                paymentOptionProducer,
                transferProducer,
                false, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestTransfer(getMessage(objectMapper.writeValueAsString(tr))));

        verify(anonimizerServiceMock, times(1)).anonymizeWithRetry(REMITTANCE_INFORMATION);
        verify(transferProducer).sendIngestedTransfer(transferCaptor.capture());
        DataCaptureMessage<Transfer> captured = transferCaptor.getValue();
        assertNull(captured.getBefore());
        assertEquals(tr.getAfter().getId(), captured.getAfter().getId());
        assertEquals(ANONYMIZED_REMITTANCE_INFORMATION, captured.getAfter().getRemittanceInformation());
    }

    @Test
    void ingestTransferRunOkBothAfterAndBefore() throws JsonProcessingException, AnonymizerException {
        when(anonimizerServiceMock.anonymizeWithRetry(REMITTANCE_INFORMATION))
                .thenReturn(ANONYMIZED_REMITTANCE_INFORMATION);
        DataCaptureMessage<Transfer> tr = generateValidTransfer(true);

        sut = new IngestionServiceImpl(
                objectMapper,
                pdvTokenizerServiceMock,
                anonimizerServiceMock,
                paymentPositionProducer,
                paymentOptionProducer,
                transferProducer,
                false, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestTransfer(getMessage(objectMapper.writeValueAsString(tr))));

        verify(anonimizerServiceMock, times(2)).anonymizeWithRetry(REMITTANCE_INFORMATION);
        verify(transferProducer).sendIngestedTransfer(transferCaptor.capture());
        DataCaptureMessage<Transfer> captured = transferCaptor.getValue();
        assertEquals(tr.getBefore().getId(), captured.getBefore().getId());
        assertEquals(ANONYMIZED_REMITTANCE_INFORMATION, captured.getBefore().getRemittanceInformation());
        assertEquals(tr.getAfter().getId(), captured.getAfter().getId());
        assertEquals(ANONYMIZED_REMITTANCE_INFORMATION, captured.getAfter().getRemittanceInformation());
    }

    @Test
    void ingestTransferRunNullRemittanceInformation() throws JsonProcessingException, AnonymizerException {
        when(anonimizerServiceMock.anonymizeWithRetry(REMITTANCE_INFORMATION))
                .thenReturn(ANONYMIZED_REMITTANCE_INFORMATION);
        DataCaptureMessage<Transfer> tr = generateValidTransfer(false);
        Transfer transferAfter = tr.getAfter();
        transferAfter.setRemittanceInformation(null);
        tr.setAfter(transferAfter);

        sut = new IngestionServiceImpl(
                objectMapper,
                pdvTokenizerServiceMock,
                anonimizerServiceMock,
                paymentPositionProducer,
                paymentOptionProducer,
                transferProducer,
                false, false);

        // test execution
        assertDoesNotThrow(() -> sut.ingestTransfer(getMessage(objectMapper.writeValueAsString(tr))));

        verify(anonimizerServiceMock, never()).anonymizeWithRetry(REMITTANCE_INFORMATION);
        verify(transferProducer).sendIngestedTransfer(transferCaptor.capture());
        DataCaptureMessage<Transfer> captured = transferCaptor.getValue();
        assertNull(captured.getBefore());
        assertEquals(tr.getAfter().getId(), captured.getAfter().getId());
        assertNull(captured.getAfter().getRemittanceInformation());
    }

    @Test
    void ingestTransferErrorAnonymizingRemittance()
            throws AnonymizerException, JsonProcessingException {
        when(anonimizerServiceMock.anonymizeWithRetry(REMITTANCE_INFORMATION))
                .thenThrow(
                        new AnonymizerException(
                                HTTP_MESSAGE_ERROR, org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR));

        sut = new IngestionServiceImpl(
                objectMapper,
                pdvTokenizerServiceMock,
                anonimizerServiceMock,
                paymentPositionProducer,
                paymentOptionProducer,
                transferProducer,
                false, false);

        DataCaptureMessage<Transfer> tr = generateValidTransfer(false);
        Message<String> msg = getMessage(objectMapper.writeValueAsString(tr));

        assertThrows(AppException.class, () -> sut.ingestTransfer(msg));

        verify(anonimizerServiceMock, times(1)).anonymizeWithRetry(REMITTANCE_INFORMATION);
        verify(transferProducer, never()).sendIngestedTransfer(any());
    }

    @Test
    void ingestTransferRunOkBothAfterAndBeforeWithPlaceholderOnAnonymizerError()
            throws AnonymizerException, JsonProcessingException {
        when(anonimizerServiceMock.anonymizeWithRetry(REMITTANCE_INFORMATION))
                .thenThrow(new AnonymizerException("test", 500));

        DataCaptureMessage<Transfer> tr = generateValidTransfer(true);

        sut = new IngestionServiceImpl(
                objectMapper,
                pdvTokenizerServiceMock,
                anonimizerServiceMock,
                paymentPositionProducer,
                paymentOptionProducer,
                transferProducer,
                false, true);

        // test execution
        assertDoesNotThrow(() -> sut.ingestTransfer(getMessage(objectMapper.writeValueAsString(tr))));

        verify(anonimizerServiceMock, times(2)).anonymizeWithRetry(REMITTANCE_INFORMATION);
        verify(transferProducer).sendIngestedTransfer(transferCaptor.capture());
        DataCaptureMessage<Transfer> captured = transferCaptor.getValue();
        assertEquals("Anonymized", captured.getBefore().getRemittanceInformation());
        assertEquals("Anonymized", captured.getAfter().getRemittanceInformation());
    }

    @Test
    void ingestTransferRunTombstoneMessage() throws AnonymizerException, JsonProcessingException {
        sut = new IngestionServiceImpl(
                objectMapper,
                pdvTokenizerServiceMock,
                anonimizerServiceMock,
                paymentPositionProducer,
                paymentOptionProducer,
                transferProducer,
                false, true);

        // test execution
        assertDoesNotThrow(() -> sut.ingestTransfer(getMessage("")));
        verify(anonimizerServiceMock, never()).anonymizeWithRetry(REMITTANCE_INFORMATION);
        verify(transferProducer, never()).sendIngestedTransfer(any());
    }

    private DataCaptureMessage<Transfer> generateValidTransfer(boolean withBefore) {
        Transfer pp =
                Transfer.builder()
                        .id(0)
                        .paymentOptionId(0)
                        .amount(0)
                        .category("category")
                        .transferId("transferId")
                        .insertedDate(DATE)
                        .iuv("iuv")
                        .lastUpdateDate(DATE)
                        .organizationFiscalCode("organizationFiscalCode")
                        .remittanceInformation(REMITTANCE_INFORMATION)
                        .status(TransferStatus.T_REPORTED.name())
                        .build();

        return DataCaptureMessage.<Transfer>builder()
                .before(withBefore ? pp : null)
                .after(pp)
                .op("c")
                .tsMs(0L)
                .tsNs(0L)
                .tsUs(0L)
                .build();
    }

    private Message<String> getMessage(String entity) {
        Map<String, Object> headers = Map.of("id", "id");
        return new GenericMessage<>(entity, headers);
}
}
