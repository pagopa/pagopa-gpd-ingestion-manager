package it.gov.pagopa.gpd.ingestion.manager;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.Cardinality;
import com.microsoft.azure.functions.annotation.EventHubOutput;
import com.microsoft.azure.functions.annotation.EventHubTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import it.gov.pagopa.gpd.ingestion.manager.model.DataCapturePaymentOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Azure Functions with Azure EventHub trigger.
 */
public class PaymentOptionProcessor {

    private final Logger logger = LoggerFactory.getLogger(PaymentOptionProcessor.class);

    /**
     * This function will be invoked when an Event Hub trigger occurs
     */
    @FunctionName("EventHubPaymentOptionProcessor")
    public void processPaymentOption(
            @EventHubTrigger(
                    name = "PaymentOptionTrigger",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "PAYMENT_OPTION_INPUT_EVENTHUB_CONN_STRING",
                    cardinality = Cardinality.MANY)
            List<DataCapturePaymentOption> paymentOptionMsg,
            @EventHubOutput(
                    name = "PaymentOptionOutput",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "PAYMENT_OPTION_OUTPUT_EVENTHUB_CONN_STRING")
            OutputBinding<List<DataCapturePaymentOption>> paymentPositionProcessed,
            final ExecutionContext context) {

        String message = String.format("PaymentOptionProcessor function called at %s with events list size %s", LocalDateTime.now(), paymentOptionMsg.size());
        logger.info(message);

        // persist the item
        try {
            paymentPositionProcessed.setValue(paymentOptionMsg);
        } catch (Exception e) {
            logger.error(String.format("Generic exception on paymentOption msg ingestion at %s : %s", LocalDateTime.now(), e.getMessage()));
        }

    }
}
