package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.*;
import it.gov.pagopa.gpd.ingestion.manager.model.DeadLetterRecord;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.DeadLetterRetryStatus;
import it.gov.pagopa.gpd.ingestion.manager.service.StorageTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static it.gov.pagopa.gpd.ingestion.manager.model.DeadLetterRecord.TABLE_KEY_LOCK_EXPIRATION;

@Service
public class StorageTableServiceImpl implements StorageTableService {
    private final TableClient tableClient;

    @Autowired
    public StorageTableServiceImpl(TableClient tableClient) {
        this.tableClient = tableClient;
    }

    @Override
    public void saveDeadLetter(DeadLetterRecord deadLetterRecord) {
        this.tableClient.upsertEntity(deadLetterRecord.toTableEntity());
    }

    @Override
    public TableEntity getDeadLetter(DeadLetterRetryStatus retryStatus, String messageId) {
        return this.tableClient.getEntity(retryStatus.name(), messageId);
    }

    @Override
    public List<DeadLetterRecord> getDeadLetterByRetryStatus(DeadLetterRetryStatus retryStatus) {
        ListEntitiesOptions options = new ListEntitiesOptions()
                .setFilter(String.format("PartitionKey eq '%s'", retryStatus.name()));

        return this.tableClient.listEntities(options, null, null).stream()
                .map(DeadLetterRecord::fromTableEntity)
                .toList();
    }

    @Override
    public void updateDeadLetter(DeadLetterRecord deadLetterRecord) {
        this.tableClient.updateEntity(deadLetterRecord.toTableEntity(), TableEntityUpdateMode.REPLACE);
    }

    @Override
    public void updateDeadLetterPartitionKey(DeadLetterRecord deadLetterRecord, DeadLetterRetryStatus newPartitionKey) {
        // Needs to delete and recreate to update partitionKey
        TableEntity oldEntity = deadLetterRecord.toTableEntity();

        deadLetterRecord.setRetryStatus(newPartitionKey);
        tableClient.upsertEntity(deadLetterRecord.toTableEntity());

        tableClient.deleteEntity(oldEntity.getPartitionKey(), oldEntity.getRowKey());
    }

    @Override
    public void deleteDeadLetter(DeadLetterRecord dlRecord) {
        this.tableClient.deleteEntity(dlRecord.toTableEntity());
    }

    @Override
    public boolean acquireLockOptimistic(DeadLetterRecord dlRecord) {
        try {
            long timestampNow = System.currentTimeMillis();

            // Retrieve the table entity and keep it untrasformed to use the same ETag for concurrency
            TableEntity tableEntity = this.getDeadLetter(dlRecord.getRetryStatus(), dlRecord.getMessageId());

            DeadLetterRecord tableRecord = DeadLetterRecord.fromTableEntity(tableEntity);
            if (tableRecord.getLockExpiration() != null && tableRecord.getLockExpiration() > timestampNow) {
                return false;
            }

            //Acquire lock
            long lockExpiration = timestampNow + (5L * 60 * 1000); // 5 minutes in milliseconds
            tableEntity.getProperties().put(TABLE_KEY_LOCK_EXPIRATION, lockExpiration);
            this.tableClient.updateEntity(tableEntity, TableEntityUpdateMode.MERGE);

            dlRecord.setLockExpiration(lockExpiration);

            return true;
        } catch (TableServiceException e) {
            if (e.getResponse().getStatusCode() == 412 || e.getResponse().getStatusCode() == 404) {
                // Error 412: Precondition Failed (the dlRecord has been changed by another client)
                // Error 404: Entity Not Found (the dlRecord has been deleted or changed partitionKey)
                return false;
            }
            throw e;
        }
    }
}
