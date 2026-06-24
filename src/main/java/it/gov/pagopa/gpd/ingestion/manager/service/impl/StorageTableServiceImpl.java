package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableEntityUpdateMode;
import it.gov.pagopa.gpd.ingestion.manager.model.DeadLetterRecord;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.DeadLetterRetryStatus;
import it.gov.pagopa.gpd.ingestion.manager.service.StorageTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StorageTableServiceImpl implements StorageTableService {
    private final TableClient tableClient;

    @Autowired
    public StorageTableServiceImpl(TableClient tableClient) {
        this.tableClient = tableClient;
    }

    @Override
    public void saveDeadLetter(DeadLetterRecord deadLetterRecord) {
        tableClient.upsertEntity(deadLetterRecord.toTableEntity());
    }

    @Override
    public DeadLetterRecord getDeadLetter(DeadLetterRetryStatus retryStatus, String messageId) {
        TableEntity entity = tableClient.getEntity(retryStatus.name(), messageId);
        return DeadLetterRecord.fromTableEntity(entity);
    }

    @Override
    public List<DeadLetterRecord> getDeadLetterByRetryStatus(DeadLetterRetryStatus retryStatus) {
        ListEntitiesOptions options = new ListEntitiesOptions()
                .setFilter(String.format("PartitionKey eq '%s'", retryStatus.name()));

        return tableClient.listEntities(options, null, null).stream()
                .map(DeadLetterRecord::fromTableEntity)
                .toList();
    }

    @Override
    public void updateDeadLetter(DeadLetterRecord deadLetterRecord) {
        tableClient.updateEntity(deadLetterRecord.toTableEntity(), TableEntityUpdateMode.REPLACE);
    }

    @Override
    public void deleteDeadLetter(DeadLetterRetryStatus retryStatus, String messageId) {
        tableClient.deleteEntity(retryStatus.name(), messageId);
    }
}
