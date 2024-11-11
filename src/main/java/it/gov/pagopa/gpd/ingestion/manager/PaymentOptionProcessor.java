package it.gov.pagopa.gpd.ingestion.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.Cardinality;
import com.microsoft.azure.functions.annotation.EventHubOutput;
import com.microsoft.azure.functions.annotation.EventHubTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import it.gov.pagopa.gpd.ingestion.manager.entity.PaymentOption;
import it.gov.pagopa.gpd.ingestion.manager.model.DataCaptureMessage;
import it.gov.pagopa.gpd.ingestion.manager.utils.ObjectMapperUtils;
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
     * This function will be invoked when a EventHub trigger occurs
     * <p>
     * Map PaymentOption fields to ignore unused ones
     *
     * @param paymentOptionMsg       List of messages coming from eventhub with payment options' values
     * @param paymentOptionProcessed Output binding that will save the ingested payment options
     * @param context                Function context
     */
    @FunctionName("EventHubPaymentOptionProcessor")
    public void processPaymentOption(
            @EventHubTrigger(
                    name = "PaymentOptionTrigger",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "PAYMENT_OPTION_INPUT_EVENTHUB_CONN_STRING",
                    cardinality = Cardinality.MANY)
            String paymentOptionMsg,
            @EventHubOutput(
                    name = "PaymentOptionOutput",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "PAYMENT_OPTION_OUTPUT_EVENTHUB_CONN_STRING")
            OutputBinding<List<DataCaptureMessage<PaymentOption>>> paymentOptionProcessed,
            final ExecutionContext context) {

        logger.info("[{}] function called at {} for payment options",
                context.getFunctionName(), LocalDateTime.now());

        // persist the item
        try {
            List<DataCaptureMessage<PaymentOption>> paymentOptionList = ObjectMapperUtils.mapDataCapturePaymentOptionListString(paymentOptionMsg, new TypeReference<>() {});

            logger.info("[{}] function called at {} for payment options with events list size {}",
                    context.getFunctionName(), LocalDateTime.now(), paymentOptionList.size());

            paymentOptionProcessed.setValue(paymentOptionList);
        } catch (Exception e) {
            logger.error("[{}] function error Generic exception at {} : {}",
                    context.getFunctionName(), LocalDateTime.now(), e.getMessage());
        }

    }
}
