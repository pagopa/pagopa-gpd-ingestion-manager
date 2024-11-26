package it.gov.pagopa.gpd.ingestion.manager.events.producer;

import it.gov.pagopa.gpd.ingestion.manager.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.PaymentOption;
import org.springframework.stereotype.Service;

/**
 * Interface to use when required to execute sending of a {@link PaymentOption} message through
 * the eventhub channel
 */
@Service
public interface IngestedPaymentOptionProducer {

    /**
     * Send an ingested {@link PaymentOption} to GPD eventhub
     *
     * @param ingestedPaymentOption data to send
     * @return boolean referring if the insertion on the sending channel was successfully
     */
    boolean sendIngestedPaymentOption(DataCaptureMessage<PaymentOption> ingestedPaymentOption);

}