package it.gov.pagopa.gpd.ingestion.manager.service;

import it.gov.pagopa.gpd.ingestion.manager.model.DeadLetterRecord;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.DeadLetterRetryStatus;

import java.util.List;

public interface StorageTableService {
    /**
     * Save a dead letter record on storage table
     * @param deadLetterRecord The mapped error message
     */
    void saveDeadLetter(DeadLetterRecord deadLetterRecord);

    /**
     * Retrieve dead letter record
     * @param retryStatus Record's partitionKey
     * @param messageId Record's rowKey
     * @return found {@link DeadLetterRecord}
     */
    DeadLetterRecord getDeadLetter(DeadLetterRetryStatus retryStatus, String messageId);

    /**
     * Retrieve all dead letter records by retry status
     * @param retryStatus Record's partitionKey
     * @return list of all {@link DeadLetterRecord} found by partitionKey
     */
    List<DeadLetterRecord> getDeadLetterByRetryStatus(DeadLetterRetryStatus retryStatus);

    /**
     * Update the dead letter record
     * @param deadLetterRecord the data to update the record with
     */
    void updateDeadLetter(DeadLetterRecord deadLetterRecord);

    /**
     * Delete and insert the dead letter record to update partitionKey
     * @param deadLetterRecord the record to be updated
     * @param newPartitionKey the retry status to update the record with
     */
    void updateDeadLetterPartitionKey(DeadLetterRecord deadLetterRecord, DeadLetterRetryStatus  newPartitionKey);

    /**
     * Delete a dead letter record
     * @param deadLetterRecord record to be deleted
     */
    void deleteDeadLetter(DeadLetterRecord deadLetterRecord);

    /**
     * Lock a record to prevent double retry
     * @param record dead letter record
     * @return true if the lock succeeded
     */
    boolean acquireLockOptimistic(DeadLetterRecord record);
}
