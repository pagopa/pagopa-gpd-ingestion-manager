package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.azure.core.http.rest.PagedIterable;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableEntityUpdateMode;
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
                .build();

        tableEntity = new TableEntity(DeadLetterRetryStatus.TO_RETRY.name(), "msg-abc-123")
                .addProperty("entityType", EntityType.PAYMENT_POSITION.name())
                .addProperty("originalMessage", "{\"id\":\"123\"}")
                .addProperty("cause", "Connection Timeout")
                .addProperty("errorCode", "500")
                .addProperty("numOfRetries", 1);
    }

    @Test
    void saveDeadLetter_OK() {
        storageTableService.saveDeadLetter(deadLetterRecord);

        ArgumentCaptor<TableEntity> entityCaptor = ArgumentCaptor.forClass(TableEntity.class);
        verify(tableClient, times(1)).upsertEntity(entityCaptor.capture());

        TableEntity capturedEntity = entityCaptor.getValue();
        assertEquals(DeadLetterRetryStatus.TO_RETRY.name(), capturedEntity.getPartitionKey());
        assertEquals("msg-abc-123", capturedEntity.getRowKey());
    }

    @Test
    void getDeadLetter_OK() {
        when(tableClient.getEntity(DeadLetterRetryStatus.TO_RETRY.name(), "msg-abc-123"))
                .thenReturn(tableEntity);

        DeadLetterRecord result = storageTableService.getDeadLetter(DeadLetterRetryStatus.TO_RETRY, "msg-abc-123");

        assertNotNull(result);
        assertEquals("msg-abc-123", result.getMessageId());
        assertEquals(DeadLetterRetryStatus.TO_RETRY, result.getRetryStatus());
        assertEquals(EntityType.PAYMENT_POSITION, result.getEntityType());
        assertEquals("Connection Timeout", result.getCause());
        verify(tableClient, times(1)).getEntity(DeadLetterRetryStatus.TO_RETRY.name(), "msg-abc-123");
    }

    @Test
    void getDeadLetterByRetryStatus_OK() {
        when(pagedIterable.stream()).thenReturn(Stream.of(tableEntity));

        when(tableClient.listEntities(any(ListEntitiesOptions.class), any(), any()))
                .thenReturn(pagedIterable);

        List<DeadLetterRecord> result = storageTableService.getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("msg-abc-123", result.get(0).getMessageId());

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
        assertEquals("msg-abc-123", capturedEntity.getRowKey());
    }

    @Test
    void deleteDeadLetter_OK() {
        storageTableService.deleteDeadLetter(DeadLetterRetryStatus.TO_RETRY, "msg-abc-123");

        verify(tableClient, times(1)).deleteEntity(DeadLetterRetryStatus.TO_RETRY.name(), "msg-abc-123");
    }
}