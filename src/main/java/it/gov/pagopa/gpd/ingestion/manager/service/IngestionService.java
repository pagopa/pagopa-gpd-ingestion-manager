package it.gov.pagopa.gpd.ingestion.manager.service;

import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.Transfer;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.PaymentPosition;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.PaymentOption;

public interface IngestionService {

    /**
     * Ingest a {@link PaymentPosition} message
     * from GPD eventhub and tokenizes the tax codes
     *
     * @param message PaymentPosition message
     */
    void ingestPaymentPosition(String message);

    /**
     * Ingest a {@link PaymentOption} message
     * from GPD eventhub
     *
     * @param message PaymentOption message
     */
    void ingestPaymentOption(String message);

    /**
     * Ingest a {@link Transfer} message
     * from GPD eventhub
     *
     * @param message Transfer message
     */
    void ingestTransfer(String message);
}
