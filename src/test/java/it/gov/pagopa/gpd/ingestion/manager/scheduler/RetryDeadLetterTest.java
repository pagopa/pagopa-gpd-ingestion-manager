package it.gov.pagopa.gpd.ingestion.manager.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gpd.ingestion.manager.model.DeadLetterRecord;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.DeadLetterRetryStatus;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.EntityType;
import it.gov.pagopa.gpd.ingestion.manager.service.impl.IngestionServiceImpl;
import it.gov.pagopa.gpd.ingestion.manager.service.impl.StorageTableServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {RetryDeadLetter.class})
class RetryDeadLetterTest {

    @MockBean
    private StorageTableServiceImpl storageTableService;

    @MockBean
    private IngestionServiceImpl ingestionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RetryDeadLetter retryDeadLetter;

    private DeadLetterRecord baseRecord;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        baseRecord = DeadLetterRecord.builder()
                .rowKey("msg-123")
                .retryStatus(DeadLetterRetryStatus.TO_RETRY)
                .originalMessage(this.objectMapper.writeValueAsString(getMessage("{\"key\":\"value\"}")))
                .numOfRetries(0)
                .lockExpiration(null)
                .build();
    }

    @Test
    void retryDeadLetter_disabled_shouldDoNothing() {
        ReflectionTestUtils.setField(retryDeadLetter, "isRetryEnabled", false);

        retryDeadLetter.retryDeadLetter();

        verifyNoInteractions(storageTableService);
        verifyNoInteractions(ingestionService);
        ReflectionTestUtils.setField(retryDeadLetter, "isRetryEnabled", true);
    }

    @Test
    void retryDeadLetter_noRecords() {
        when(storageTableService.getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY))
                .thenReturn(Collections.emptyList());

        retryDeadLetter.retryDeadLetter();

        verify(storageTableService, times(1)).getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY);
        verifyNoMoreInteractions(storageTableService);
        verifyNoInteractions(ingestionService);
    }

    @Test
    void retryDeadLetter_nullEntityType_OK_MALFORMED() {
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
        baseRecord.setEntityType(EntityType.PAYMENT_POSITION);

        when(storageTableService.getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY))
                .thenReturn(List.of(baseRecord));
        when(storageTableService.acquireLockOptimistic(baseRecord))
                .thenReturn(true);

        retryDeadLetter.retryDeadLetter();

        verify(ingestionService, times(1)).ingestPaymentPosition(anyString());
        verify(storageTableService, times(1)).deleteDeadLetter(baseRecord);
        verify(storageTableService, never()).updateDeadLetter(any());
    }

    @Test
    void retryDeadLetter_withPaymentOption_OK() {
        baseRecord.setEntityType(EntityType.PAYMENT_OPTION);

        when(storageTableService.getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY))
                .thenReturn(List.of(baseRecord));
        when(storageTableService.acquireLockOptimistic(baseRecord))
                .thenReturn(true);

        retryDeadLetter.retryDeadLetter();

        verify(ingestionService, times(1)).ingestPaymentOption(any());
        verify(storageTableService, times(1)).deleteDeadLetter(baseRecord);
    }

    @Test
    void retryDeadLetter_withTransfer_OK() {
        baseRecord.setEntityType(EntityType.TRANSFER);

        when(storageTableService.getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY))
                .thenReturn(List.of(baseRecord));
        when(storageTableService.acquireLockOptimistic(baseRecord))
                .thenReturn(true);

        retryDeadLetter.retryDeadLetter();

        verify(ingestionService, times(1)).ingestTransfer(anyString());
        verify(storageTableService, times(1)).deleteDeadLetter(baseRecord);
    }

    @Test
    void retryDeadLetter_KO_updateNumRetry() {
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

    private Message<String> getMessage(String entity) {
        Map<String, Object> headers = Map.of("id", "id");
        return new GenericMessage<>(entity, headers);
    }
}