package it.gov.pagopa.gpd.ingestion.manager.scheduler;

import it.gov.pagopa.gpd.ingestion.manager.exception.AppError;
import it.gov.pagopa.gpd.ingestion.manager.exception.AppException;
import it.gov.pagopa.gpd.ingestion.manager.model.DeadLetterRecord;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.DeadLetterRetryStatus;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.EntityType;
import it.gov.pagopa.gpd.ingestion.manager.service.IngestionService;
import it.gov.pagopa.gpd.ingestion.manager.service.StorageTableService;
import it.gov.pagopa.gpd.ingestion.manager.service.impl.IngestionServiceImpl;
import it.gov.pagopa.gpd.ingestion.manager.service.impl.StorageTableServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RetryDeadLetter {

    public static final String KAFKA_HEADER_ID = "id";
    private final boolean isRetryEnabled;
    private final long retryBackoffInterval;

    private final StorageTableService storageTableService;
    private final IngestionService ingestionService;

    @Autowired
    RetryDeadLetter(
            StorageTableServiceImpl storageTableService,
            IngestionServiceImpl ingestionService,
            @Value("${retry.dead.letter.enabled}") boolean isRetryEnabled,
            @Value("${retry.dead.letter.retry.backoff.interval}") long retryBackoffInterval

    ) {
        this.storageTableService = storageTableService;
        this.ingestionService = ingestionService;
        this.isRetryEnabled = isRetryEnabled;
        this.retryBackoffInterval = retryBackoffInterval;
    }

    // Runs every 5 minutes
    @Scheduled(cron = "${retry.dead.letter.cron}")
    public void retryDeadLetter() {
        if (isRetryEnabled) {
            for (DeadLetterRecord dlRecord : retrieveAndAcquireLock()) {
                try {
                    EntityType entityType = dlRecord.getEntityType();
                    ingestDeadLetter(dlRecord, entityType);

                    this.storageTableService.deleteDeadLetter(dlRecord);
                } catch (Exception e) {
                    handleRetryException(dlRecord, e);
                }
            }
        } else {
            log.info("Retry scheduler is currently PAUSED.");
        }
    }

    private List<DeadLetterRecord> retrieveAndAcquireLock() {
        log.debug("Start retry {}", System.currentTimeMillis());
        List<DeadLetterRecord> deadLetterRecords = this.storageTableService.getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY);
        log.debug("Retrieved dead letters {}", System.currentTimeMillis());

        List<DeadLetterRecord> processableDeadLetters = new ArrayList<>();
        for (DeadLetterRecord dlRecord : deadLetterRecords) {
            try {
                if (this.storageTableService.acquireLockOptimistic(dlRecord)) {
                    processableDeadLetters.add(dlRecord);
                }
            } catch (Exception e) {
                handleRetryException(dlRecord, e);
            }
        }
        log.debug("Locked dead letters {}", System.currentTimeMillis());

        log.debug("TOTAL {} LOCKED {}", deadLetterRecords.size(), processableDeadLetters.size());

        return processableDeadLetters;
    }

    private void ingestDeadLetter(DeadLetterRecord dlRecord, EntityType entityType) {
        Message<String> originalMessage = getKafkaMessage(dlRecord);

        if (entityType == null || entityType.equals(EntityType.UNKNOWN)) {
            throw new AppException(AppError.DEAD_LETTER_NOT_PROCESSABLE);
        }
        if (entityType.equals(EntityType.PAYMENT_POSITION)) {
            ingestionService.ingestPaymentPosition(originalMessage);
        }
        if (entityType.equals(EntityType.PAYMENT_OPTION)) {
            ingestionService.ingestPaymentOption(originalMessage);
        }
        if (entityType.equals(EntityType.TRANSFER)) {
            ingestionService.ingestTransfer(originalMessage);
        }
    }

    private static Message<String> getKafkaMessage(DeadLetterRecord dlRecord) {
        Map<String, Object> headers = Map.of(KAFKA_HEADER_ID, dlRecord.getMessageId());
        return new GenericMessage<>(dlRecord.getOriginalMessage(), headers);
    }

    private void handleRetryException(DeadLetterRecord dlRecord, Exception e) {
        log.error(e.getMessage());
        dlRecord.setLockExpiration(
                dlRecord.getNumOfRetries() > 0 ?
                        System.currentTimeMillis() + (retryBackoffInterval * dlRecord.getNumOfRetries() * 1000)
                        : 0
        );
        dlRecord.setNumOfRetries(dlRecord.getNumOfRetries() + 1);

        DeadLetterRetryStatus exceptionRetryStatus = getExceptionRetryStatus(e);

        if (exceptionRetryStatus.equals(DeadLetterRetryStatus.TO_RETRY)) {
            this.storageTableService.updateDeadLetter(dlRecord);
        } else {
            this.storageTableService.updateDeadLetterPartitionKey(dlRecord, exceptionRetryStatus);
        }
    }

    private static DeadLetterRetryStatus getExceptionRetryStatus(Exception e) {
        if (e instanceof AppException appE &&
                (appE.getAppErrorCode().equals(AppError.DEAD_LETTER_NOT_PROCESSABLE) ||
                        appE.getAppErrorCode().equals(AppError.JSON_NOT_PROCESSABLE) ||
                        appE.getAppErrorCode().equals(AppError.NULL_MESSAGE))
        ) {
            return DeadLetterRetryStatus.RETRY_MALFORMED;
        }

        return DeadLetterRetryStatus.TO_RETRY;
    }
}