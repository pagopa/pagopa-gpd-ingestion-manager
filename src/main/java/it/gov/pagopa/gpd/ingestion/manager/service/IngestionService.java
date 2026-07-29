package it.gov.pagopa.gpd.ingestion.manager.service;

public interface IngestionService {

    /**
     * Ingest a {@link it.gov.pagopa.gpd.ingestion.manager.events.model.entity.PaymentPosition} message
     * from GPD eventhub and tokenizes the tax codes
     *
     * @param message PaymentPosition message
     */
    void ingestPaymentPosition(String message);

    /**
     * Ingest a {@link it.gov.pagopa.gpd.ingestion.manager.events.model.entity.PaymentOption} message
     * from GPD eventhub
     *
     * @param message PaymentOption message
     */
    void ingestPaymentOption(String message);

    /**
     * Ingest a {@link it.gov.pagopa.gpd.ingestion.manager.events.model.entity.Transfer} message
     * from GPD eventhub
     *
     * @param message Transfer message
     */
    void ingestTransfer(String message);
}
