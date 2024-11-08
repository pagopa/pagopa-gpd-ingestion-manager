package it.gov.pagopa.gpd.ingestion.manager;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.Cardinality;
import com.microsoft.azure.functions.annotation.EventHubOutput;
import com.microsoft.azure.functions.annotation.EventHubTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import it.gov.pagopa.gpd.ingestion.manager.entity.Transfer;
import it.gov.pagopa.gpd.ingestion.manager.model.DataCaptureMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Azure Functions with Azure EventHub trigger.
 */
public class TransferProcessor {
    private final Logger logger = LoggerFactory.getLogger(TransferProcessor.class);

    /**
     * This function will be invoked when an Event Hub trigger occurs
     */
    @FunctionName("EventHubTransferProcessor")
    public void processTransfer(
            @EventHubTrigger(
                    name = "TransferTrigger",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "TRANSFER_INPUT_EVENTHUB_CONN_STRING",
                    cardinality = Cardinality.MANY)
            List<DataCaptureMessage<Transfer>> transferMsg,
            @EventHubOutput(
                    name = "TransferOutput",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "TRANSFER_OUTPUT_EVENTHUB_CONN_STRING")
            OutputBinding<List<DataCaptureMessage<Transfer>>> transferProcessed,
            final ExecutionContext context) {

        String message = String.format("TransferProcessor function called at %s with events list size %s", LocalDateTime.now(), transferMsg.size());
        logger.info(message);

        // persist the item
        try {
            transferProcessed.setValue(transferMsg);
        } catch (Exception e) {
            logger.error(String.format("Generic exception on transfer msg ingestion at %s : %s", LocalDateTime.now(), e.getMessage()));
        }

    }
}
