package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.azure.core.http.HttpResponse;
import com.azure.core.http.rest.PagedIterable;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.*;
import it.gov.pagopa.gpd.ingestion.manager.model.DeadLetterRecord;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.DeadLetterRetryStatus;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageTableServiceImplTest {

    @Mock
    private TableClient tableClient;

    @Mock
    private PagedIterable<TableEntity> pagedIterable;

    @InjectMocks
    private StorageTableServiceImpl storageTableService;

    private DeadLetterRecord deadLetterRecord;
    private TableEntity tableEntity;

    @BeforeEach
    void setUp() {
        deadLetterRecord = DeadLetterRecord.builder()
                .messageId("msg-abc-123")
                .retryStatus(DeadLetterRetryStatus.TO_RETRY)
                .entityType(EntityType.PAYMENT_POSITION)
                .originalMessage("{\"id\":\"123\"}")
                .cause("Connection Timeout")
                .errorCode("500")
                .numOfRetries(1)
                .locked(false)
                .build();

        tableEntity = new TableEntity(DeadLetterRetryStatus.TO_RETRY.name(), "msg-abc-123")
                .addProperty("entityType", EntityType.PAYMENT_POSITION.name())
                .addProperty("originalMessage", "{\"id\":\"123\"}")
                .addProperty("cause", "Connection Timeout")
                .addProperty("errorCode", "500")
                .addProperty("locked", false)
                .addProperty("numOfRetries", 1);
    }

    @Test
    void saveDeadLetter_OK() {
        storageTableService.saveDeadLetter(deadLetterRecord);

        ArgumentCaptor<TableEntity> entityCaptor = ArgumentCaptor.forClass(TableEntity.class);
        verify(tableClient, times(1)).upsertEntity(entityCaptor.capture());

        TableEntity capturedEntity = entityCaptor.getValue();
        assertEquals(DeadLetterRetryStatus.TO_RETRY.name(), capturedEntity.getPartitionKey());
        assertEquals(deadLetterRecord.getMessageId(), capturedEntity.getRowKey());
    }

    @Test
    void getDeadLetter_OK() {
        when(tableClient.getEntity(DeadLetterRetryStatus.TO_RETRY.name(), deadLetterRecord.getMessageId()))
                .thenReturn(tableEntity);

        DeadLetterRecord result = storageTableService.getDeadLetter(DeadLetterRetryStatus.TO_RETRY, deadLetterRecord.getMessageId());

        assertNotNull(result);
        assertEquals(deadLetterRecord.getMessageId(), result.getMessageId());
        assertEquals(DeadLetterRetryStatus.TO_RETRY, result.getRetryStatus());
        assertEquals(EntityType.PAYMENT_POSITION, result.getEntityType());
        assertEquals("Connection Timeout", result.getCause());
        verify(tableClient, times(1)).getEntity(DeadLetterRetryStatus.TO_RETRY.name(), deadLetterRecord.getMessageId());
    }

    @Test
    void getDeadLetterByRetryStatus_OK() {
        when(pagedIterable.stream()).thenReturn(Stream.of(tableEntity));

        when(tableClient.listEntities(any(ListEntitiesOptions.class), any(), any()))
                .thenReturn(pagedIterable);

        List<DeadLetterRecord> result = storageTableService.getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(deadLetterRecord.getMessageId(), result.get(0).getMessageId());

        ArgumentCaptor<ListEntitiesOptions> optionsCaptor = ArgumentCaptor.forClass(ListEntitiesOptions.class);
        verify(tableClient, times(1)).listEntities(optionsCaptor.capture(), any(), any());

        ListEntitiesOptions capturedOptions = optionsCaptor.getValue();
        assertEquals("PartitionKey eq 'TO_RETRY'", capturedOptions.getFilter());
    }

    @Test
    void getDeadLetterByRetryStatus_empty() {
        when(pagedIterable.stream()).thenReturn(Stream.empty());
        when(tableClient.listEntities(any(ListEntitiesOptions.class), any(), any()))
                .thenReturn(pagedIterable);

        List<DeadLetterRecord> result = storageTableService.getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void updateDeadLetter_OK() {
        storageTableService.updateDeadLetter(deadLetterRecord);

        ArgumentCaptor<TableEntity> entityCaptor = ArgumentCaptor.forClass(TableEntity.class);
        verify(tableClient, times(1)).updateEntity(entityCaptor.capture(), eq(TableEntityUpdateMode.REPLACE));

        TableEntity capturedEntity = entityCaptor.getValue();
        assertEquals(deadLetterRecord.getMessageId(), capturedEntity.getRowKey());
    }

    @Test
    void updateDeadLetterPartitionKey_OK() {
        storageTableService.updateDeadLetterPartitionKey(deadLetterRecord, DeadLetterRetryStatus.RETRY_MALFORMED);

        ArgumentCaptor<List<TableTransactionAction>> transactionActionsCaptor = ArgumentCaptor.forClass(List.class);
        verify(tableClient, times(1)).submitTransaction(transactionActionsCaptor.capture());

        List<TableTransactionAction> capturedActions = transactionActionsCaptor.getValue();
        for(TableTransactionAction action : capturedActions){
            assertEquals(deadLetterRecord.getMessageId(), action.getEntity().getRowKey());
            if(action.getActionType().equals(TableTransactionActionType.CREATE)){
                assertEquals(DeadLetterRetryStatus.RETRY_MALFORMED.name(), action.getEntity().getPartitionKey());
            } else {
                assertEquals(DeadLetterRetryStatus.TO_RETRY.name(), action.getEntity().getPartitionKey());
            }
        }
    }

    @Test
    void deleteDeadLetter_OK() {
        storageTableService.deleteDeadLetter(deadLetterRecord);

        verify(tableClient, times(1)).deleteEntity(any());
    }

    @Test
    void acquireLockOptimistic_OK() {
        when(tableClient.getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getMessageId()))
                .thenReturn(tableEntity);
        boolean acquired = storageTableService.acquireLockOptimistic(deadLetterRecord);

        assertTrue(acquired);
        verify(tableClient, times(1)).getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getMessageId());
        verify(tableClient, times(1)).updateEntity(any(), any());
    }

    @Test
    void acquireLockOptimistic_KO_alreadyLocked() {
        tableEntity.addProperty("locked", true);
        when(tableClient.getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getMessageId()))
                .thenReturn(tableEntity);

        deadLetterRecord.setLocked(false);
        boolean acquired = storageTableService.acquireLockOptimistic(deadLetterRecord);

        assertFalse(acquired);
        verify(tableClient, times(1)).getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getMessageId());
        verify(tableClient, never()).updateEntity(any());
    }

    @Test
    void acquireLockOptimistic_KO_error422() {
        when(tableClient.getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getMessageId()))
                .thenReturn(tableEntity);

        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(412);
        doThrow(new TableServiceException("error", mockHttpResponse)).when(tableClient).updateEntity(any(), any());
        boolean acquired = storageTableService.acquireLockOptimistic(deadLetterRecord);

        assertFalse(acquired);
        verify(tableClient, times(1)).getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getMessageId());
        verify(tableClient, times(1)).updateEntity(any(), any());
    }

    @Test
    void acquireLockOptimistic_KO_error404() {
        when(tableClient.getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getMessageId()))
                .thenReturn(tableEntity);

        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(404);
        doThrow(new TableServiceException("error", mockHttpResponse)).when(tableClient).updateEntity(any(), any());
        boolean acquired = storageTableService.acquireLockOptimistic(deadLetterRecord);

        assertFalse(acquired);
        verify(tableClient, times(1)).getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getMessageId());
        verify(tableClient, times(1)).updateEntity(any(), any());
    }

    @Test
    void acquireLockOptimistic_KO_genericError() {
        when(tableClient.getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getMessageId()))
                .thenReturn(tableEntity);

        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(500);
        doThrow(new TableServiceException("error", mockHttpResponse)).when(tableClient).updateEntity(any(), any());
        assertThrows(TableServiceException.class, () -> storageTableService.acquireLockOptimistic(deadLetterRecord));

        verify(tableClient, times(1)).getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getMessageId());
        verify(tableClient, times(1)).updateEntity(any(), any());
    }
}