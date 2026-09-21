package it.gov.pagopa.gpd.ingestion.manager.events.producer;

import it.gov.pagopa.gpd.ingestion.manager.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.PaymentPosition;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.before.PaymentPositionBefore;
import org.springframework.stereotype.Service;

/**
 * Interface to use when required to execute sending of a {@link PaymentPosition} message through
 * the eventhub channel
 */
@Service
public interface IngestedPaymentPositionProducer {

    /**
     * Send an ingested {@link PaymentPosition} to GPD eventhub
     *
     * @param ingestedPaymentPosition data to send
     */
    void sendIngestedPaymentPosition(DataCaptureMessage<PaymentPosition, PaymentPositionBefore> ingestedPaymentPosition);

}