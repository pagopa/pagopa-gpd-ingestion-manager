package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.*;
import it.gov.pagopa.gpd.ingestion.manager.model.DeadLetterRecord;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.DeadLetterRetryStatus;
import it.gov.pagopa.gpd.ingestion.manager.service.StorageTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

import static it.gov.pagopa.gpd.ingestion.manager.model.DeadLetterRecord.TABLE_KEY_LOCK_EXPIRATION;

@Service
public class StorageTableServiceImpl implements StorageTableService {
    private final TableClient tableClient;
    private final int recordLimit;
    private final long lockDurationInSeconds;
    private final int retryMax;

    @Autowired
    public StorageTableServiceImpl(
            TableClient tableClient,
            @Value("${azure.storage.record.limit}") int recordLimit,
            @Value("${azure.storage.record.lock.duration}") long lockDurationInSeconds,
            @Value("${retry.dead.letter.retry.max}") int retryMax
    ) {
        this.tableClient = tableClient;
        this.recordLimit = recordLimit;
        this.lockDurationInSeconds = lockDurationInSeconds;
        this.retryMax = retryMax;
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
        long now = System.currentTimeMillis();
        ListEntitiesOptions options = new ListEntitiesOptions()
                .setFilter(String.format("PartitionKey eq '%s' and lockExpiration le %dL and numOfRetries le %d", retryStatus.name(), now, retryMax));

        return this.tableClient.listEntities(options, null, null).stream().limit(recordLimit)
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
            if (tableRecord.getLockExpiration() != null && tableRecord.getLockExpiration() > timestampNow && dlRecord.getNumOfRetries() < retryMax) {
                return false;
            }

            //Acquire lock
            long lockExpiration = timestampNow + (lockDurationInSeconds * 1000); // lock duration in milliseconds
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
