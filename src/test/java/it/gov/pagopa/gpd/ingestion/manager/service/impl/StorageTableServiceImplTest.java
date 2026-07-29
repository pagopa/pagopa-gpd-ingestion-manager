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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.stream.Stream;

import static it.gov.pagopa.gpd.ingestion.manager.model.DeadLetterRecord.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {StorageTableServiceImpl.class})
class StorageTableServiceImplTest {

    @MockBean
    private TableClient tableClient;

    @Mock
    private PagedIterable<TableEntity> pagedIterable;

    @Autowired
    private StorageTableServiceImpl sut;

    private DeadLetterRecord deadLetterRecord;
    private TableEntity tableEntity;

    @BeforeEach
    void setUp() {
        deadLetterRecord = DeadLetterRecord.builder()
                .rowKey("msg-abc-123")
                .retryStatus(DeadLetterRetryStatus.TO_RETRY)
                .entityType(EntityType.PAYMENT_POSITION)
                .entityId("entityId")
                .originalMessage("{\"id\":\"123\"}")
                .cause("Connection Timeout")
                .errorCode("500")
                .numOfRetries(1)
                .lockExpiration(null)
                .build();

        tableEntity = new TableEntity(DeadLetterRetryStatus.TO_RETRY.name(), "msg-abc-123")
                .addProperty(TABLE_KEY_ENTITY_ID, "entityId")
                .addProperty(TABLE_KEY_ORIGINAL_MESSAGE, "{\"id\":\"123\"}")
                .addProperty(TABLE_KEY_CAUSE, "Connection Timeout")
                .addProperty(TABLE_KEY_ERROR_CODE, "500")
                .addProperty(TABLE_KEY_ENTITY_TYPE, EntityType.PAYMENT_POSITION.name())
                .addProperty(TABLE_KEY_LOCK_EXPIRATION, null)
                .addProperty(TABLE_KEY_NUM_OF_RETRIES, 1);
    }

    @Test
    void saveDeadLetter_OK() {
        sut.saveDeadLetter(deadLetterRecord);

        ArgumentCaptor<TableEntity> entityCaptor = ArgumentCaptor.forClass(TableEntity.class);
        verify(tableClient, times(1)).upsertEntity(entityCaptor.capture());

        TableEntity capturedEntity = entityCaptor.getValue();
        assertEquals(DeadLetterRetryStatus.TO_RETRY.name(), capturedEntity.getPartitionKey());
        assertEquals(deadLetterRecord.getRowKey(), capturedEntity.getRowKey());
    }

    @Test
    void getDeadLetter_OK() {
        when(tableClient.getEntity(DeadLetterRetryStatus.TO_RETRY.name(), deadLetterRecord.getRowKey()))
                .thenReturn(tableEntity);

        TableEntity resultEntity = sut.getDeadLetter(DeadLetterRetryStatus.TO_RETRY, deadLetterRecord.getRowKey());
        DeadLetterRecord result = DeadLetterRecord.fromTableEntity(resultEntity);

        assertNotNull(result);
        assertEquals(deadLetterRecord.getRowKey(), result.getRowKey());
        assertEquals(DeadLetterRetryStatus.TO_RETRY, result.getRetryStatus());
        assertEquals(EntityType.PAYMENT_POSITION, result.getEntityType());
        assertEquals("Connection Timeout", result.getCause());
        verify(tableClient, times(1)).getEntity(DeadLetterRetryStatus.TO_RETRY.name(), deadLetterRecord.getRowKey());
    }

    @Test
    void getDeadLetterByRetryStatus_OK() {
        when(pagedIterable.stream()).thenReturn(Stream.of(tableEntity));

        when(tableClient.listEntities(any(ListEntitiesOptions.class), any(), any()))
                .thenReturn(pagedIterable);

        List<DeadLetterRecord> result = sut.getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(deadLetterRecord.getRowKey(), result.get(0).getRowKey());

        ArgumentCaptor<ListEntitiesOptions> optionsCaptor = ArgumentCaptor.forClass(ListEntitiesOptions.class);
        verify(tableClient, times(1)).listEntities(optionsCaptor.capture(), any(), any());

        ListEntitiesOptions capturedOptions = optionsCaptor.getValue();
        assertTrue(capturedOptions.getFilter().contains("PartitionKey eq 'TO_RETRY' and lockExpiration le"));
    }

    @Test
    void getDeadLetterByRetryStatus_empty() {
        when(pagedIterable.stream()).thenReturn(Stream.empty());
        when(tableClient.listEntities(any(ListEntitiesOptions.class), any(), any()))
                .thenReturn(pagedIterable);

        List<DeadLetterRecord> result = sut.getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void updateDeadLetter_OK() {
        sut.updateDeadLetter(deadLetterRecord);

        ArgumentCaptor<TableEntity> entityCaptor = ArgumentCaptor.forClass(TableEntity.class);
        verify(tableClient, times(1)).updateEntity(entityCaptor.capture(), eq(TableEntityUpdateMode.REPLACE));

        TableEntity capturedEntity = entityCaptor.getValue();
        assertEquals(deadLetterRecord.getRowKey(), capturedEntity.getRowKey());
    }

    @Test
    void updateDeadLetterPartitionKey_OK() {
        sut.updateDeadLetterPartitionKey(deadLetterRecord, DeadLetterRetryStatus.RETRY_MALFORMED);

        assertEquals(DeadLetterRetryStatus.RETRY_MALFORMED, deadLetterRecord.getRetryStatus());
        verify(tableClient, times(1)).upsertEntity(any());
        verify(tableClient, times(1)).deleteEntity(DeadLetterRetryStatus.TO_RETRY.name(), deadLetterRecord.getRowKey());
    }

    @Test
    void deleteDeadLetter_OK() {
        sut.deleteDeadLetter(deadLetterRecord);

        verify(tableClient, times(1)).deleteEntity(any());
    }

    @Test
    void acquireLockOptimistic_OK() {
        when(tableClient.getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getRowKey()))
                .thenReturn(tableEntity);
        boolean acquired = sut.acquireLockOptimistic(deadLetterRecord);

        assertTrue(acquired);
        verify(tableClient, times(1)).getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getRowKey());
        verify(tableClient, times(1)).updateEntityWithResponse(any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void acquireLockOptimistic_KO_alreadyLocked() {
        Long lockExpiration = System.currentTimeMillis() + (5000*50000);
        tableEntity.addProperty(TABLE_KEY_LOCK_EXPIRATION, lockExpiration);
        when(tableClient.getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getRowKey()))
                .thenReturn(tableEntity);

        deadLetterRecord.setLockExpiration(lockExpiration);
        boolean acquired = sut.acquireLockOptimistic(deadLetterRecord);

        assertFalse(acquired);
        verify(tableClient, times(1)).getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getRowKey());
        verify(tableClient, never()).updateEntity(any());
    }

    @Test
    void acquireLockOptimistic_KO_error412() {
        when(tableClient.getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getRowKey()))
                .thenReturn(tableEntity);

        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(412);
        doThrow(new TableServiceException("error", mockHttpResponse)).when(tableClient).updateEntityWithResponse(any(), any(), anyBoolean(), any(), any());
        boolean acquired = sut.acquireLockOptimistic(deadLetterRecord);

        assertFalse(acquired);
        verify(tableClient, times(1)).getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getRowKey());
        verify(tableClient, times(1)).updateEntityWithResponse(any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void acquireLockOptimistic_KO_error404() {
        when(tableClient.getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getRowKey()))
                .thenReturn(tableEntity);

        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(404);
        doThrow(new TableServiceException("error", mockHttpResponse)).when(tableClient).updateEntityWithResponse(any(), any(), anyBoolean(), any(), any());
        boolean acquired = sut.acquireLockOptimistic(deadLetterRecord);

        assertFalse(acquired);
        verify(tableClient, times(1)).getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getRowKey());
        verify(tableClient, times(1)).updateEntityWithResponse(any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void acquireLockOptimistic_KO_genericError() {
        when(tableClient.getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getRowKey()))
                .thenReturn(tableEntity);

        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(500);
        doThrow(new TableServiceException("error", mockHttpResponse)).when(tableClient).updateEntityWithResponse(any(), any(), anyBoolean(), any(), any());
        assertThrows(TableServiceException.class, () -> sut.acquireLockOptimistic(deadLetterRecord));

        verify(tableClient, times(1)).getEntity(deadLetterRecord.getRetryStatus().name(), deadLetterRecord.getRowKey());
        verify(tableClient, times(1)).updateEntityWithResponse(any(), any(), anyBoolean(), any(), any());
    }
}