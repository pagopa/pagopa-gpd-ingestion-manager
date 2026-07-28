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
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeadLetterServiceImpl implements DeadLetterService {
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
            return;
        }
        DeadLetterRecord deadLetterRecord = DeadLetterRecord.builder()
                .retryStatus(DeadLetterRetryStatus.TO_RETRY)
                .messageId(errorMessageId.toString())
                .entityId(jsonMessage.getString("id"))
                .cause(cause)
                .errorCode(errorCode)
                .originalMessage(failedMessage)
                .entityType(entityType)
                .lockExpiration(0L)
                .numOfRetries(0)
                .build();

        storageTableService.saveDeadLetter(deadLetterRecord);
    }
}