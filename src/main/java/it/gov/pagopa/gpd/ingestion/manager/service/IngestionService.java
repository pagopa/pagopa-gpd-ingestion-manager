package it.gov.pagopa.gpd.ingestion.manager.service;

import java.util.List;

public interface IngestionService {

    /**
     * Ingest a {@link it.gov.pagopa.gpd.ingestion.manager.events.model.entity.PaymentPosition} message
     * from GPD eventhub and tokenizes the tax codes
     *
     * @param messages PaymentPosition messages
     */
    void ingestPaymentPositions(List<String> messages);

    /**
     * Ingest a {@link it.gov.pagopa.gpd.ingestion.manager.events.model.entity.PaymentOption} message
     * from GPD eventhub
     *
     * @param messages PaymentOption messages
     */
    void ingestPaymentOptions(List<String> messages);

    /**
     * Ingest a {@link it.gov.pagopa.gpd.ingestion.manager.events.model.entity.Transfer} message
     * from GPD eventhub
     *
     * @param messages Transfer messages
     */
    void ingestTransfers(List<String> messages);
}
