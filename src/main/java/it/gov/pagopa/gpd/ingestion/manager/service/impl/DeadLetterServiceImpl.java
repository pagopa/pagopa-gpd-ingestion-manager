package it.gov.pagopa.gpd.ingestion.manager.service.impl;

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
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
            @Value("${spring.cloud.stream.bindings.ingestPaymentPosition-in-0.destination}")
            String paymentPositionTopic,
            @Value("${spring.cloud.stream.bindings.ingestPaymentOption-in-0.destination}")
            String paymentOptionTopic,
            @Value("${spring.cloud.stream.bindings.ingestTransfer-in-0.destination}")
            String transferTopic
    ) {
        this.storageTableService = storageTableService;
        this.paymentPositionTopic = paymentPositionTopic;
        this.paymentOptionTopic = paymentOptionTopic;
        this.transferTopic = transferTopic;
    }

    @Override
    public void sendToDeadLetter(ErrorMessage errorMessage) {
        AppException appException = (AppException) errorMessage.getPayload().getCause();

        String messageId = getMessageId(errorMessage);
        String originalMessagePayload = getOriginalMessagePayload(errorMessage);
        EntityType entityType = getEntityType(errorMessage);

        DeadLetterRecord deadLetterRecord = DeadLetterRecord.builder()
                .retryStatus(DeadLetterRetryStatus.TO_RETRY)
                .messageId(messageId)
                .cause(appException.getMessage())
                .errorCode(String.valueOf(appException.getAppErrorCode()))
                .originalMessage(originalMessagePayload)
                .entityType(entityType)
                .build();

        storageTableService.saveDeadLetter(deadLetterRecord);
    }

    private String getOriginalMessagePayload(ErrorMessage errorMessage) {
        String originalMessagePayload = "\"[ERROR] Retrieving original message payload\"";
        Message<?> originalMessage = errorMessage.getOriginalMessage();
        if (originalMessage != null) {
            try {
                originalMessagePayload = messageToString(originalMessage.getPayload());
            } catch (Exception ignored) {
                // handled after
                log.warn("Unable to retrieve original message payload for messageId", ignored);
            }
        }
        return originalMessagePayload;
    }

    private String getMessageId(ErrorMessage errorMessage) {
        String messageId = String.valueOf(errorMessage.getHeaders().getId());
        Message<?> originalMessage = errorMessage.getOriginalMessage();
        if (originalMessage != null) {
            Object cdcMessageKey = originalMessage.getHeaders().get(KafkaHeaders.RECEIVED_KEY);
            if (cdcMessageKey != null) {
                try {
                    messageId = new JSONObject(messageToString(cdcMessageKey)).get("id").toString();
                } catch (Exception ignored) {
                    // handled after
                }
            }
        }
        return messageId;
    }

    private EntityType getEntityType(ErrorMessage errorMessage) {
        Message<?> originalMessage = errorMessage.getOriginalMessage();

        if(originalMessage != null){
            ArrayList<?> headerTopic = originalMessage.getHeaders().get(KafkaHeaders.RECEIVED_TOPIC, ArrayList.class);
            String receivedTopic = headerTopic == null || headerTopic.isEmpty() ? null : String.valueOf(headerTopic.get(0));
            if (receivedTopic != null) {
                if (receivedTopic.equals(paymentPositionTopic)) {
                    return EntityType.PAYMENT_POSITION;
                }
                if (receivedTopic.equals(paymentOptionTopic)) {
                    return EntityType.PAYMENT_OPTION;
                }
                if (receivedTopic.equals(transferTopic)) {
                    return EntityType.TRANSFER;
                }
            }
        }

        return EntityType.UNKNOWN;
    }

    public static String messageToString(Object message) {
        if (message == null) {
            return "message is null";
        }

        if (message instanceof byte[] byteArray) {
            return new String(byteArray, StandardCharsets.UTF_8);
        }

        if (message instanceof List<?> list && !list.isEmpty()) {
            Object firstElement = list.get(0);
            if (firstElement instanceof byte[] byteArray) {
                return new String(byteArray, StandardCharsets.UTF_8);
            }
        }

        return String.valueOf(message);
    }
}
