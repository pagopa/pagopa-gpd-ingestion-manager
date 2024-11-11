package it.gov.pagopa.gpd.ingestion.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.Cardinality;
import com.microsoft.azure.functions.annotation.EventHubOutput;
import com.microsoft.azure.functions.annotation.EventHubTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import it.gov.pagopa.gpd.ingestion.manager.entity.Transfer;
import it.gov.pagopa.gpd.ingestion.manager.model.DataCaptureMessage;
import it.gov.pagopa.gpd.ingestion.manager.utils.ObjectMapperUtils;
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
     * This function will be invoked when a EventHub trigger occurs
     * <p>
     * Map Transfer fields to ignore unused ones
     *
     * @param transferMsg       List of messages coming from eventhub with transfers' values
     * @param transferProcessed Output binding that will save the ingested transfers
     * @param context           Function context
     */
    @FunctionName("EventHubTransferProcessor")
    public void processTransfer(
            @EventHubTrigger(
                    name = "TransferTrigger",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "TRANSFER_INPUT_EVENTHUB_CONN_STRING",
                    cardinality = Cardinality.MANY)
            String transferMsg,
            @EventHubOutput(
                    name = "TransferOutput",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "TRANSFER_OUTPUT_EVENTHUB_CONN_STRING")
            OutputBinding<List<DataCaptureMessage<Transfer>>> transferProcessed,
            final ExecutionContext context) {

        logger.info("[{}] function called at {} for transfers",
                context.getFunctionName(), LocalDateTime.now());

        // persist the item
        try {
            List<DataCaptureMessage<Transfer>> transferList = ObjectMapperUtils.mapDataCaptureTransferListString(transferMsg, new TypeReference<>() {
            });

            logger.info("[{}] function called at {} for transfers with events list size {}",
                    context.getFunctionName(), LocalDateTime.now(), transferList.size());

            transferProcessed.setValue(transferList);
        } catch (Exception e) {
            logger.error("[{}] function error Generic exception at {} : {}",
                    context.getFunctionName(), LocalDateTime.now(), e.getMessage());
        }

    }
}
