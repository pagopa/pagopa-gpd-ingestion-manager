package it.gov.pagopa.gpd.ingestion.manager.scheduler;

import it.gov.pagopa.gpd.ingestion.manager.model.DeadLetterRecord;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.DeadLetterRetryStatus;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.EntityType;
import it.gov.pagopa.gpd.ingestion.manager.service.impl.IngestionServiceImpl;
import it.gov.pagopa.gpd.ingestion.manager.service.impl.StorageTableServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryDeadLetterTest {

    @Mock
    private StorageTableServiceImpl storageTableService;

    @Mock
    private IngestionServiceImpl ingestionService;

    @InjectMocks
    private RetryDeadLetter retryDeadLetter;

    private DeadLetterRecord baseRecord;

    @BeforeEach
    void setUp() {
        baseRecord = DeadLetterRecord.builder()
                .messageId("msg-123")
                .retryStatus(DeadLetterRetryStatus.TO_RETRY)
                .originalMessage("{\"key\":\"value\"}")
                .numOfRetries(0)
                .lockExpiration(null)
                .build();
    }

    @Test
    void retryDeadLetter_disabled_shouldDoNothing() {
        retryDeadLetter.setRetryEnabled(false);

        retryDeadLetter.retryDeadLetter();

        verifyNoInteractions(storageTableService);
        verifyNoInteractions(ingestionService);
    }

    @Test
    void retryDeadLetter_noRecords() {
        retryDeadLetter.setRetryEnabled(true);
        when(storageTableService.getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY))
                .thenReturn(Collections.emptyList());

        retryDeadLetter.retryDeadLetter();

        verify(storageTableService, times(1)).getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY);
        verifyNoMoreInteractions(storageTableService);
        verifyNoInteractions(ingestionService);
    }

    @Test
    void retryDeadLetter_nullEntityType_OK_MALFORMED() {
        retryDeadLetter.setRetryEnabled(true);
        baseRecord.setEntityType(null);

        when(storageTableService.getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY))
                .thenReturn(List.of(baseRecord));
        when(storageTableService.acquireLockOptimistic(baseRecord))
                .thenReturn(true);

        retryDeadLetter.retryDeadLetter();

        ArgumentCaptor<DeadLetterRecord> capturedRecord = ArgumentCaptor.forClass(DeadLetterRecord.class);
        verify(storageTableService, times(1)).updateDeadLetterPartitionKey(capturedRecord.capture(), eq(DeadLetterRetryStatus.RETRY_MALFORMED));
        verify(storageTableService, never()).deleteDeadLetter(any());
        verifyNoInteractions(ingestionService);
    }

    @Test
    void retryDeadLetter_unknownEntityType_OK_MALFORMED() {
        retryDeadLetter.setRetryEnabled(true);
        baseRecord.setEntityType(EntityType.UNKNOWN);

        when(storageTableService.getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY))
                .thenReturn(List.of(baseRecord));
        when(storageTableService.acquireLockOptimistic(baseRecord))
                .thenReturn(true);

        retryDeadLetter.retryDeadLetter();

        verify(storageTableService, times(1)).updateDeadLetterPartitionKey(baseRecord, DeadLetterRetryStatus.RETRY_MALFORMED);
        verifyNoInteractions(ingestionService);
    }

    @Test
    void retryDeadLetter_withPaymentPosition_OK() {
        retryDeadLetter.setRetryEnabled(true);
        baseRecord.setEntityType(EntityType.PAYMENT_POSITION);

        when(storageTableService.getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY))
                .thenReturn(List.of(baseRecord));
        when(storageTableService.acquireLockOptimistic(baseRecord))
                .thenReturn(true);

        retryDeadLetter.retryDeadLetter();

        verify(ingestionService, times(1)).ingestPaymentPosition(baseRecord.getOriginalMessage());
        verify(storageTableService, times(1)).deleteDeadLetter(baseRecord);
        verify(storageTableService, never()).updateDeadLetter(any());
    }

    @Test
    void retryDeadLetter_withPaymentOption_OK() {
        retryDeadLetter.setRetryEnabled(true);
        baseRecord.setEntityType(EntityType.PAYMENT_OPTION);

        when(storageTableService.getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY))
                .thenReturn(List.of(baseRecord));
        when(storageTableService.acquireLockOptimistic(baseRecord))
                .thenReturn(true);

        retryDeadLetter.retryDeadLetter();

        verify(ingestionService, times(1)).ingestPaymentOption(baseRecord.getOriginalMessage());
        verify(storageTableService, times(1)).deleteDeadLetter(baseRecord);
    }

    @Test
    void retryDeadLetter_withTransfer_OK() {
        retryDeadLetter.setRetryEnabled(true);
        baseRecord.setEntityType(EntityType.TRANSFER);

        when(storageTableService.getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY))
                .thenReturn(List.of(baseRecord));
        when(storageTableService.acquireLockOptimistic(baseRecord))
                .thenReturn(true);

        retryDeadLetter.retryDeadLetter();

        verify(ingestionService, times(1)).ingestTransfer(baseRecord.getOriginalMessage());
        verify(storageTableService, times(1)).deleteDeadLetter(baseRecord);
    }

    @Test
    void retryDeadLetter_KO_updateNumRetry() {
        retryDeadLetter.setRetryEnabled(true);
        baseRecord.setEntityType(EntityType.PAYMENT_POSITION);
        int initialRetries = baseRecord.getNumOfRetries(); // 0

        when(storageTableService.getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY))
                .thenReturn(List.of(baseRecord));
        when(storageTableService.acquireLockOptimistic(baseRecord))
                .thenReturn(true);

        doThrow(new RuntimeException("Kafka or DB connection error"))
                .when(ingestionService).ingestPaymentPosition(any());

        retryDeadLetter.retryDeadLetter();

        assertEquals(initialRetries + 1, baseRecord.getNumOfRetries());
        verify(storageTableService, times(1)).updateDeadLetter(baseRecord);
        verify(storageTableService, never()).deleteDeadLetter(any());
    }
}