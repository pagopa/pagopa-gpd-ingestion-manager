package it.gov.pagopa.gpd.ingestion.manager.service;

import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.EntityType;

public interface DeadLetterService {
    void sendToDeadLetter(String failedMessage, EntityType entityType, Exception exception);
}
