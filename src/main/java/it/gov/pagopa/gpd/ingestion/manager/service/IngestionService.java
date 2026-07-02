package it.gov.pagopa.gpd.ingestion.manager.service;

import org.springframework.messaging.Message;

public interface IngestionService {

    /**
     * Ingest a {@link it.gov.pagopa.gpd.ingestion.manager.events.model.entity.PaymentPosition} message
     * from GPD eventhub and tokenizes the tax codes
     *
     * @param message PaymentPosition message
     */
    void ingestPaymentPosition(Message<String> message);

    /**
     * Ingest a {@link it.gov.pagopa.gpd.ingestion.manager.events.model.entity.PaymentOption} message
     * from GPD eventhub
     *
     * @param message PaymentOption message
     */
    void ingestPaymentOption(Message<String> message);

    /**
     * Ingest a {@link it.gov.pagopa.gpd.ingestion.manager.events.model.entity.Transfer} message
     * from GPD eventhub
     *
     * @param message Transfer message
     */
    void ingestTransfer(Message<String> message);
}
