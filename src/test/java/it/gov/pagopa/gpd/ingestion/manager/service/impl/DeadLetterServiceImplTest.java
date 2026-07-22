package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import it.gov.pagopa.gpd.ingestion.manager.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.ingestion.manager.exception.AppError;
import it.gov.pagopa.gpd.ingestion.manager.exception.AppException;
import it.gov.pagopa.gpd.ingestion.manager.model.DeadLetterRecord;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.EntityType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = DeadLetterServiceImpl.class)
class DeadLetterServiceImplTest {

    private static final String CDC_MESSAGE_ID = "12345";
    private static final byte[] CDC_MESSAGE_KEY = ("{\"id\":\"" + CDC_MESSAGE_ID + "\"}").getBytes();
    private static final String ORIGINAL_MESSAGE_PAYLOAD =
            "[ERROR] Retrieving original message payload";
    private static final String UNKNOWN_TOPIC = "unknown_topic";
    @Value("${spring.cloud.stream.bindings.ingestPaymentPosition-in-0.destination}")
    private String paymentPositionTopic;
    @Value("${spring.cloud.stream.bindings.ingestPaymentOption-in-0.destination}")
    private String paymentOptionTopic;
    @Value("${spring.cloud.stream.bindings.ingestTransfer-in-0.destination}")
    private String transferTopic;

    @MockBean
    private StorageTableServiceImpl storageTableService;

    @Captor
    private ArgumentCaptor<DeadLetterRecord> deadLetterRecordCaptor;

    @Autowired
    private DeadLetterServiceImpl sut;

    @Test
    void sendToDeadLetter_OK() {
        ErrorMessage errorMessage = buildErrorMessage(UNKNOWN_TOPIC);

        assertDoesNotThrow(() -> sut.sendToDeadLetter(errorMessage));

        verify(storageTableService)
                .saveDeadLetter(deadLetterRecordCaptor.capture());
        DeadLetterRecord capturedDeadLetterRecord = deadLetterRecordCaptor.getValue();

        assertEquals(CDC_MESSAGE_ID, capturedDeadLetterRecord.getEntityId());
        assertEquals(AppError.INTERNAL_SERVER_ERROR.getDetails(),
                capturedDeadLetterRecord.getCause());
        assertEquals(AppError.INTERNAL_SERVER_ERROR.name(),
                capturedDeadLetterRecord.getErrorCode());
    }

    @Test
    void sendToDeadLetter_OK_paymentPosition() {
        ErrorMessage errorMessage = buildErrorMessage(paymentPositionTopic);

        assertDoesNotThrow(() -> sut.sendToDeadLetter(errorMessage));

        verify(storageTableService)
                .saveDeadLetter(deadLetterRecordCaptor.capture());
        DeadLetterRecord capturedDeadLetterRecord = deadLetterRecordCaptor.getValue();

        assertEquals(CDC_MESSAGE_ID, capturedDeadLetterRecord.getEntityId());
        assertEquals(AppError.INTERNAL_SERVER_ERROR.getDetails(),
                capturedDeadLetterRecord.getCause());
        assertEquals(AppError.INTERNAL_SERVER_ERROR.name(),
                capturedDeadLetterRecord.getErrorCode());
        assertEquals(EntityType.PAYMENT_POSITION, capturedDeadLetterRecord.getEntityType());
    }

    @Test
    void sendToDeadLetter_OK_paymentOption() {
        ErrorMessage errorMessage = buildErrorMessage(paymentOptionTopic);

        assertDoesNotThrow(() -> sut.sendToDeadLetter(errorMessage));

        verify(storageTableService)
                .saveDeadLetter(deadLetterRecordCaptor.capture());
        DeadLetterRecord capturedDeadLetterRecord = deadLetterRecordCaptor.getValue();

        assertEquals(CDC_MESSAGE_ID, capturedDeadLetterRecord.getEntityId());
        assertEquals(AppError.INTERNAL_SERVER_ERROR.getDetails(),
                capturedDeadLetterRecord.getCause());
        assertEquals(AppError.INTERNAL_SERVER_ERROR.name(),
                capturedDeadLetterRecord.getErrorCode());
        assertEquals(EntityType.PAYMENT_OPTION, capturedDeadLetterRecord.getEntityType());
    }

    @Test
    void sendToDeadLetter_OK_transfer() {
        ErrorMessage errorMessage = buildErrorMessage(transferTopic);

        assertDoesNotThrow(() -> sut.sendToDeadLetter(errorMessage));

        verify(storageTableService)
                .saveDeadLetter(deadLetterRecordCaptor.capture());
        DeadLetterRecord capturedDeadLetterRecord = deadLetterRecordCaptor.getValue();

        assertEquals(CDC_MESSAGE_ID, capturedDeadLetterRecord.getEntityId());
        assertEquals(AppError.INTERNAL_SERVER_ERROR.getDetails(),
                capturedDeadLetterRecord.getCause());
        assertEquals(AppError.INTERNAL_SERVER_ERROR.name(),
                capturedDeadLetterRecord.getErrorCode());
        assertEquals(EntityType.TRANSFER, capturedDeadLetterRecord.getEntityType());
    }

    @Test
    void sendToDeadLetter_OK2() {
        ErrorMessage errorMessage = buildErrorMessage2();

        assertDoesNotThrow(() -> sut.sendToDeadLetter(errorMessage));

        verify(storageTableService)
                .saveDeadLetter(deadLetterRecordCaptor.capture());
        DeadLetterRecord capturedDeadLetterRecord = deadLetterRecordCaptor.getValue();

        assertEquals(
                AppError.INTERNAL_SERVER_ERROR.details, capturedDeadLetterRecord.getCause());
        assertEquals(
                AppError.INTERNAL_SERVER_ERROR.name(), capturedDeadLetterRecord.getErrorCode());
    }

    @Test
    void sendToDeadLetter_KO_ERROR_PARSING_MESSAGE_KEY() {
        ErrorMessage errorMessage = buildErrorMessageWithoutOriginalMessageHeaders();

        assertDoesNotThrow(() -> sut.sendToDeadLetter(errorMessage));

        verify(storageTableService).saveDeadLetter(deadLetterRecordCaptor.capture());
        DeadLetterRecord capturedDeadLetterRecord = deadLetterRecordCaptor.getValue();

        assertEquals(Objects.requireNonNull(errorMessage.getHeaders().getId()).toString(),
                capturedDeadLetterRecord.getMessageId());
    }

    @Test
    void sendToDeadLetter__KO_NULL_ORIGINAL_MESSAGE() {
        ErrorMessage errorMessage = buildErrorMessageWithoutMessage();

        assertDoesNotThrow(() -> sut.sendToDeadLetter(errorMessage));

        verify(storageTableService).saveDeadLetter(deadLetterRecordCaptor.capture());
        DeadLetterRecord capturedDeadLetterRecord = deadLetterRecordCaptor.getValue();

        assertEquals(Objects.requireNonNull(errorMessage.getHeaders().getId()).toString(),
                capturedDeadLetterRecord.getMessageId());
        assertEquals(ORIGINAL_MESSAGE_PAYLOAD, capturedDeadLetterRecord.getOriginalMessage());
    }

    private ErrorMessage buildErrorMessage(String topic) {
        AppException appException = new AppException(AppError.INTERNAL_SERVER_ERROR);

        MessageHeaders originalMessageHeaders =
                new MessageHeaders(Map.of(KafkaHeaders.RECEIVED_KEY, CDC_MESSAGE_KEY, KafkaHeaders.RECEIVED_TOPIC, topic, "id", UUID.randomUUID()));
        MessageHeaders errorMessageHeaders = new MessageHeaders(Collections.emptyMap());
        Message<byte[]> originalMessage =
                new GenericMessage<>(
                        String.valueOf(new DataCaptureMessage<>()).getBytes(), originalMessageHeaders);

        return new ErrorMessage(
                new MessageHandlingException(originalMessage, appException),
                errorMessageHeaders,
                originalMessage);
    }

    private ErrorMessage buildErrorMessage2() {
        AppException appException = new AppException(AppError.INTERNAL_SERVER_ERROR);

        MessageHeaders errorMessageHeaders = new MessageHeaders(Collections.emptyMap());
        Message<String> originalMessage = new GenericMessage<>("{}");

        return new ErrorMessage(
                new MessageHandlingException(originalMessage, appException),
                errorMessageHeaders,
                originalMessage);
    }

    private ErrorMessage buildErrorMessageWithoutMessage() {
        AppException appException = new AppException(AppError.INTERNAL_SERVER_ERROR);

        return new ErrorMessage(new Exception(appException), Collections.emptyMap());
    }

    private ErrorMessage buildErrorMessageWithoutOriginalMessageHeaders() {
        AppException appException = new AppException(AppError.INTERNAL_SERVER_ERROR);

        MessageHeaders originalMessageHeaders = new MessageHeaders(Collections.emptyMap());
        MessageHeaders errorMessageHeaders = new MessageHeaders(Collections.emptyMap());
        Message<byte[]> originalMessage =
                new GenericMessage<>(
                        String.valueOf(new DataCaptureMessage<>()).getBytes(), originalMessageHeaders);

        return new ErrorMessage(new Exception(appException), errorMessageHeaders, originalMessage);
    }
}
