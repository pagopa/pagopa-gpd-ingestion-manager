package it.gov.pagopa.gpd.ingestion.manager.service;

import it.gov.pagopa.gpd.ingestion.manager.model.DeadLetterRecord;

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
    DeadLetterRecord getDeadLetter(String retryStatus, String messageId);

    /**
     * Retrieve all dead letter records by retry status
     * @param retryStatus Record's partitionKey
     * @return list of all {@link DeadLetterRecord} found by partitionKey
     */
    List<DeadLetterRecord> getDeadLetterByRetryStatus(String retryStatus);

    /**
     * Delete a dead letter record
     * @param retryStatus Record's partitionKey
     * @param messageId Record's rowKey
     */
    void deleteDeadLetter(String retryStatus, String messageId);
}
