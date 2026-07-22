package it.gov.pagopa.gpd.ingestion.manager.service;

import org.springframework.messaging.support.ErrorMessage;

public interface DeadLetterService {
    void sendToDeadLetter(ErrorMessage message);
}
