package it.gov.pagopa.gpd.ingestion.manager.events.producer;

import it.gov.pagopa.gpd.ingestion.manager.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.Transfer;
import org.springframework.stereotype.Service;

/**
 * Interface to use when required to execute sending of a notice generation request through
 * the eventhub channel
 */
@Service
public interface IngestedTransferProducer {

    /**
     * Send an ingested Transfer to GPD eventhub
     *
     * @param ingestedTransfer data to send
     * @return boolean referring if the insertion on the sending channel was successfully
     */
    boolean sendIngestedTransfer(DataCaptureMessage<Transfer> ingestedTransfer);

}