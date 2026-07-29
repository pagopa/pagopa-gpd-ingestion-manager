package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import it.gov.pagopa.gpd.ingestion.manager.exception.AppError;
import it.gov.pagopa.gpd.ingestion.manager.exception.AppException;
import it.gov.pagopa.gpd.ingestion.manager.model.DeadLetterRecord;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.DeadLetterRetryStatus;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.EntityType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = DeadLetterServiceImpl.class)
class DeadLetterServiceImplTest {

    private static final String CDC_MESSAGE_ID = "12345";
    public static final String GENERIC_EXCEPTION = "generic exception";

    @MockBean
    private StorageTableServiceImpl storageTableService;

    @Captor
    private ArgumentCaptor<DeadLetterRecord> deadLetterRecordCaptor;

    @Autowired
    private DeadLetterServiceImpl sut;

    @Test
    void sendToDeadLetter_OK_id_before() {
        assertDoesNotThrow(() -> sut.sendToDeadLetter(String.format("{\"before\": {\"id\":\"%s\"}}", CDC_MESSAGE_ID), EntityType.UNKNOWN, new AppException(AppError.INTERNAL_SERVER_ERROR)));

        verify(storageTableService)
                .saveDeadLetter(deadLetterRecordCaptor.capture());
        DeadLetterRecord capturedDeadLetterRecord = deadLetterRecordCaptor.getValue();

        assertEquals(CDC_MESSAGE_ID, capturedDeadLetterRecord.getEntityId());
        assertEquals(EntityType.UNKNOWN, capturedDeadLetterRecord.getEntityType());
        assertEquals(DeadLetterRetryStatus.TO_RETRY, capturedDeadLetterRecord.getRetryStatus());
        assertEquals(AppError.INTERNAL_SERVER_ERROR.getDetails(),
                capturedDeadLetterRecord.getCause());
        assertEquals(AppError.INTERNAL_SERVER_ERROR.name(),
                capturedDeadLetterRecord.getErrorCode());
    }

    @Test
    void sendToDeadLetter_OK_id_after() {
        assertDoesNotThrow(() -> sut.sendToDeadLetter(String.format("{\"after\": {\"id\":\"%s\"}}", CDC_MESSAGE_ID), EntityType.UNKNOWN, new AppException(AppError.INTERNAL_SERVER_ERROR)));

        verify(storageTableService)
                .saveDeadLetter(deadLetterRecordCaptor.capture());
        DeadLetterRecord capturedDeadLetterRecord = deadLetterRecordCaptor.getValue();

        assertEquals(CDC_MESSAGE_ID, capturedDeadLetterRecord.getEntityId());
        assertEquals(EntityType.UNKNOWN, capturedDeadLetterRecord.getEntityType());
        assertEquals(DeadLetterRetryStatus.TO_RETRY, capturedDeadLetterRecord.getRetryStatus());
        assertEquals(AppError.INTERNAL_SERVER_ERROR.getDetails(),
                capturedDeadLetterRecord.getCause());
        assertEquals(AppError.INTERNAL_SERVER_ERROR.name(),
                capturedDeadLetterRecord.getErrorCode());
    }

    @Test
    void sendToDeadLetter_OK_malformed() {
        assertDoesNotThrow(() -> sut.sendToDeadLetter("", EntityType.UNKNOWN, new Exception(GENERIC_EXCEPTION)));

        verify(storageTableService)
                .saveDeadLetter(deadLetterRecordCaptor.capture());
        DeadLetterRecord capturedDeadLetterRecord = deadLetterRecordCaptor.getValue();

        assertEquals("unknown", capturedDeadLetterRecord.getEntityId());
        assertEquals(EntityType.UNKNOWN, capturedDeadLetterRecord.getEntityType());
        assertEquals(DeadLetterRetryStatus.RETRY_MALFORMED, capturedDeadLetterRecord.getRetryStatus());
        assertEquals(
                GENERIC_EXCEPTION, capturedDeadLetterRecord.getCause());
        assertEquals(
                AppError.INTERNAL_SERVER_ERROR.name(), capturedDeadLetterRecord.getErrorCode());
    }
}
