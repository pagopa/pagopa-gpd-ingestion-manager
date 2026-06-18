package it.gov.pagopa.gpd.ingestion.manager.model;

import com.azure.data.tables.models.TableEntity;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.DeadLetterRetryStatus;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.EntityType;
import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor()
@Builder
public class DeadLetterRecord {
    private DeadLetterRetryStatus retryStatus;
    private String messageId;
    private String cause;
    private String errorCode;
    private String originalMessage;
    private EntityType entityType;

    public TableEntity toTableEntity() {
        TableEntity entity = new TableEntity(retryStatus.name(), messageId);
        entity.getProperties().put("cause", cause);
        entity.getProperties().put("errorCode", errorCode);
        entity.getProperties().put("originalMessage", originalMessage);
        entity.getProperties().put("entityType", entityType.name());
        return entity;
    }

    public static DeadLetterRecord fromTableEntity(TableEntity entity) {
        Map<String, Object> props = entity.getProperties();
        return DeadLetterRecord.builder()
                .retryStatus(DeadLetterRetryStatus.valueOf(entity.getPartitionKey()))
                .messageId(entity.getRowKey())
                .cause((String) props.get("cause"))
                .errorCode((String) props.get("errorCode"))
                .originalMessage((String) props.get("originalMessage"))
                .entityType(EntityType.valueOf((String) props.get("entityType")))
                .build();
    }
}
