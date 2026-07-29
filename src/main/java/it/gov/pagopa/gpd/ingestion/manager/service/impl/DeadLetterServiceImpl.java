package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import it.gov.pagopa.gpd.ingestion.manager.exception.AppError;
import it.gov.pagopa.gpd.ingestion.manager.exception.AppException;
import it.gov.pagopa.gpd.ingestion.manager.model.DeadLetterRecord;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.DeadLetterRetryStatus;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.EntityType;
import it.gov.pagopa.gpd.ingestion.manager.service.DeadLetterService;
import it.gov.pagopa.gpd.ingestion.manager.service.StorageTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeadLetterServiceImpl implements DeadLetterService {
    public static final String ENTITY_ID_UNKNOWN = "unknown";
    private final String paymentPositionTopic;
    private final String paymentOptionTopic;
    private final String transferTopic;

    private final StorageTableService storageTableService;

    @Autowired
    public DeadLetterServiceImpl(
            StorageTableServiceImpl storageTableService,
            @Value("${spring.cloud.stream.bindings.ingestPaymentPosition-in-0.destination}") String paymentPositionTopic,
            @Value("${spring.cloud.stream.bindings.ingestPaymentOption-in-0.destination}") String paymentOptionTopic,
            @Value("${spring.cloud.stream.bindings.ingestTransfer-in-0.destination}") String transferTopic
    ) {
        this.storageTableService = storageTableService;
        this.paymentPositionTopic = paymentPositionTopic;
        this.paymentOptionTopic = paymentOptionTopic;
        this.transferTopic = transferTopic;
    }

    @Override
    public void sendToDeadLetter(String failedMessage, EntityType entityType, Exception exception) {
        String cause;
        String errorCode = AppError.INTERNAL_SERVER_ERROR.name();
        if (exception instanceof AppException appException) {
            cause = appException.getMessage();
            errorCode = appException.getAppErrorCode().name();
        } else {
            cause = exception.getMessage();
        }

        UUID errorMessageId = UUID.nameUUIDFromBytes(failedMessage.getBytes(StandardCharsets.UTF_8));
        JSONObject jsonMessage;
        try{
            jsonMessage = new JSONObject(failedMessage);
        } catch(Exception ignored){
            log.warn("Failed to parse message as JSON, skipping dead letter creation. Message: {}", failedMessage);
            jsonMessage = new JSONObject();
        }

        String entityId = getEntityId(jsonMessage);
        DeadLetterRecord deadLetterRecord = DeadLetterRecord.builder()
                .retryStatus(ENTITY_ID_UNKNOWN.equals(entityId) ? DeadLetterRetryStatus.RETRY_MALFORMED : DeadLetterRetryStatus.TO_RETRY)
                .rowKey(errorMessageId.toString())
                .entityId(entityId)
                .cause(cause)
                .errorCode(errorCode)
                .originalMessage(failedMessage)
                .entityType(entityType)
                .lockExpiration(0L)
                .numOfRetries(0)
                .build();

        storageTableService.saveDeadLetter(deadLetterRecord);
    }

    private static String getEntityId(JSONObject jsonMessage) {
        if(jsonMessage != null){
            String before = jsonMessage.optString("before", null);
            if(before != null){
                JSONObject beforeJson = new JSONObject(before);
                return beforeJson.optString("id", jsonMessage.optString("id", ENTITY_ID_UNKNOWN));
            }

            String after = jsonMessage.optString("after", null);
            if(after != null){
                JSONObject afterJson = new JSONObject(after);
                return afterJson.optString("id", jsonMessage.optString("id", ENTITY_ID_UNKNOWN));
            }
        }
        return ENTITY_ID_UNKNOWN;
    }
}