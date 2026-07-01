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
    private static final String TABLE_KEY_ENTITY_ID = "entityId";
    private static final String TABLE_KEY_CAUSE = "cause";
    private static final String TABLE_KEY_ERROR_CODE = "errorCode";
    private static final String TABLE_KEY_ORIGINAL_MESSAGE = "originalMessage";
    private static final String TABLE_KEY_ENTITY_TYPE = "entityType";
    private static final String TABLE_KEY_NUM_OF_RETRIES = "numOfRetries";
    private static final String TABLE_KEY_LOCKED = "locked";
    public static final String TABLE_KEY_LOCK_EXPIRATION = "lockTimestamp";

    private DeadLetterRetryStatus retryStatus;
    private String messageId;
    private String entityId;
    private String cause;
    private String errorCode;
    private String originalMessage;
    private EntityType entityType;
    private int numOfRetries;
    private boolean locked;
    private long lockExpiration;

    public TableEntity toTableEntity() {
        TableEntity entity = new TableEntity(retryStatus.name(), messageId);
        entity.getProperties().put(TABLE_KEY_ENTITY_ID, entityId);
        entity.getProperties().put(TABLE_KEY_CAUSE, cause);
        entity.getProperties().put(TABLE_KEY_ERROR_CODE, errorCode);
        entity.getProperties().put(TABLE_KEY_ORIGINAL_MESSAGE, originalMessage);
        entity.getProperties().put(TABLE_KEY_ENTITY_TYPE, entityType.name());
        entity.getProperties().put(TABLE_KEY_NUM_OF_RETRIES, numOfRetries);
        entity.getProperties().put(TABLE_KEY_LOCKED, locked);
        entity.getProperties().put(TABLE_KEY_LOCK_EXPIRATION, lockExpiration);
        return entity;
    }

    public static DeadLetterRecord fromTableEntity(TableEntity entity) {
        Map<String, Object> props = entity.getProperties();
        return DeadLetterRecord.builder()
                .retryStatus(DeadLetterRetryStatus.valueOf(entity.getPartitionKey()))
                .messageId(entity.getRowKey())
                .entityId((String) props.get(TABLE_KEY_ENTITY_ID))
                .cause((String) props.get(TABLE_KEY_CAUSE))
                .errorCode((String) props.get(TABLE_KEY_ERROR_CODE))
                .originalMessage((String) props.get(TABLE_KEY_ORIGINAL_MESSAGE))
                .entityType(EntityType.valueOf((String) props.get(TABLE_KEY_ENTITY_TYPE)))
                .numOfRetries((Integer) props.get(TABLE_KEY_NUM_OF_RETRIES))
                .locked((Boolean) props.get(TABLE_KEY_LOCKED))
                .lockExpiration((long) props.get(TABLE_KEY_LOCKED))
                .build();
    }
}
