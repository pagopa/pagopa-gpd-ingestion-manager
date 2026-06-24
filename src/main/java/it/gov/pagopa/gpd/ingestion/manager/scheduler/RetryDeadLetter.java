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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class RetryDeadLetter {

    private final AtomicBoolean isRetryEnabled = new AtomicBoolean(true);

    private final StorageTableService storageTableService;
    private final IngestionService ingestionService;

    @Autowired
    RetryDeadLetter(StorageTableServiceImpl storageTableService, IngestionServiceImpl ingestionService) {
        this.storageTableService = storageTableService;
        this.ingestionService = ingestionService;
    }

    // Runs every 5 minutes
    @Scheduled(cron = "* */5 * * * *")
    public void processRetries() {
        if (isRetryEnabled.get()) {
            List<DeadLetterRecord> deadLetterRecords = this.storageTableService.getDeadLetterByRetryStatus(DeadLetterRetryStatus.TO_RETRY);

            for (DeadLetterRecord dlRecord : deadLetterRecords) {
                EntityType entityType = dlRecord.getEntityType();

                try {
                    if (entityType == null || entityType.equals(EntityType.UNKNOWN)) {
                        throw new AppException(AppError.DEAD_LETTER_NOT_PROCESSABLE);
                    }
                    String originalMessageString = dlRecord.getOriginalMessage();
                    if (entityType.equals(EntityType.PAYMENT_POSITION)) {
                        ingestionService.ingestPaymentPositions(List.of(originalMessageString));
                    }
                    if (entityType.equals(EntityType.PAYMENT_OPTION)) {
                        ingestionService.ingestPaymentOptions(List.of(originalMessageString));
                    }
                    if (entityType.equals(EntityType.TRANSFER)) {
                        ingestionService.ingestTransfers(List.of(originalMessageString));
                    }

                    this.storageTableService.deleteDeadLetter(dlRecord.getRetryStatus(), dlRecord.getMessageId());
                } catch (AppException e) {
                    if (e.getAppErrorCode().equals(AppError.DEAD_LETTER_NOT_PROCESSABLE) ||
                            e.getAppErrorCode().equals(AppError.JSON_NOT_PROCESSABLE) ||
                            e.getAppErrorCode().equals(AppError.NULL_MESSAGE)) {
                        dlRecord.setRetryStatus(DeadLetterRetryStatus.RETRY_MALFORMED);
                    }
                    handleRetryException(dlRecord, e);
                } catch (Exception e) {
                    handleRetryException(dlRecord, e);
                }
            }
        } else {
            log.info("Retry scheduler is currently PAUSED.");
        }

    }

    private void handleRetryException(DeadLetterRecord record, Exception e) {
        log.error(e.getMessage());
        record.setNumOfRetries(record.getNumOfRetries() + 1);
        this.storageTableService.updateDeadLetter(record);
    }

    // Expose endpoints or JMX beans to flip this toggle manually if needed
    public void setRetryEnabled(boolean enabled) {
        this.isRetryEnabled.set(enabled);
    }
}