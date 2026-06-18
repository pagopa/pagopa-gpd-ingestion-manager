package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;
import it.gov.pagopa.gpd.ingestion.manager.model.DeadLetterRecord;
import it.gov.pagopa.gpd.ingestion.manager.service.StorageTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
    public DeadLetterRecord getDeadLetter(String retryStatus, String messageId) {
        TableEntity entity = tableClient.getEntity(retryStatus, messageId);
        return DeadLetterRecord.fromTableEntity(entity);
    }

    @Override
    public List<DeadLetterRecord> getDeadLetterByRetryStatus(String retryStatus) {
        ListEntitiesOptions options = new ListEntitiesOptions()
                .setFilter(String.format("PartitionKey eq '%s'", retryStatus));

        return tableClient.listEntities(options, null, null).stream()
                .map(DeadLetterRecord::fromTableEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteDeadLetter(String retryStatus, String messageId) {
        tableClient.deleteEntity(retryStatus, messageId);
    }
}
