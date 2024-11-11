package it.gov.pagopa.gpd.ingestion.manager;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.Cardinality;
import com.microsoft.azure.functions.annotation.EventHubOutput;
import com.microsoft.azure.functions.annotation.EventHubTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import it.gov.pagopa.gpd.ingestion.manager.entity.PaymentPosition;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
import it.gov.pagopa.gpd.ingestion.manager.model.DataCapturePaymentPosition;
import it.gov.pagopa.gpd.ingestion.manager.service.PDVTokenizerServiceRetryWrapper;
import it.gov.pagopa.gpd.ingestion.manager.service.impl.PDVTokenizerServiceRetryWrapperImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.gov.pagopa.gpd.ingestion.manager.utils.ValidationUtils.isValidFiscalCode;

/**
 * Azure Functions with Azure EventHub trigger.
 */
public class PaymentPositionProcessor {
    private final Logger logger = LoggerFactory.getLogger(PaymentPositionProcessor.class);

    private final PDVTokenizerServiceRetryWrapper pdvTokenizerService;

    public PaymentPositionProcessor() {
        this.pdvTokenizerService = new PDVTokenizerServiceRetryWrapperImpl();
    }

    PaymentPositionProcessor(PDVTokenizerServiceRetryWrapper pdvTokenizerService) {
        this.pdvTokenizerService = pdvTokenizerService;
    }

    /**
     * This function will be invoked when a EventHub trigger occurs
     * <p>
     * If valid tax codes, tokenizes the fiscal code value from both the before and after entity values
     *
     * @param paymentPositionMsg       List of messages coming from eventhub with payment positions' values
     * @param paymentPositionProcessed Output binding that will save the tokenized payment position
     * @param context                  Function context
     */
    @FunctionName("EventHubPaymentPositionProcessor")
    public void processPaymentPosition(
            @EventHubTrigger(
                    name = "PaymentPositionTrigger",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "PAYMENT_POSITION_INPUT_EVENTHUB_CONN_STRING",
                    cardinality = Cardinality.MANY)
            List<DataCapturePaymentPosition> paymentPositionMsg,
            @EventHubOutput(
                    name = "PaymentPositionOutput",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "PAYMENT_POSITION_OUTPUT_EVENTHUB_CONN_STRING")
            OutputBinding<List<DataCapturePaymentPosition>> paymentPositionProcessed,
            final ExecutionContext context) {

        logger.info("[{}] function called at {} for payment positions with events list size {}",
                context.getFunctionName(), LocalDateTime.now(), paymentPositionMsg.size());

        // persist the item
        try {
            List<DataCapturePaymentPosition> paymentPositionsTokenized = new ArrayList<>();
            for (DataCapturePaymentPosition pp : paymentPositionMsg) {
                PaymentPosition valuesBefore = pp.getBefore();
                PaymentPosition valuesAfter = pp.getAfter();

                logger.info("[{}] function called at {} with payment position id {}",
                        context.getFunctionName(), LocalDateTime.now(), (valuesAfter != null ? valuesAfter : valuesBefore).getId());

                // tokenize fiscal codes
                if (valuesBefore != null && isValidFiscalCode(valuesBefore.getFiscalCode())) {
                    valuesBefore.setFiscalCode(pdvTokenizerService.generateTokenForFiscalCodeWithRetry(valuesBefore.getFiscalCode()));
                    pp.setBefore(valuesBefore);
                }
                if (valuesAfter != null && isValidFiscalCode(valuesAfter.getFiscalCode())) {
                    valuesAfter.setFiscalCode(pdvTokenizerService.generateTokenForFiscalCodeWithRetry(valuesAfter.getFiscalCode()));
                    pp.setAfter(valuesAfter);
                }

                paymentPositionsTokenized.add(pp);
            }
            paymentPositionProcessed.setValue(paymentPositionsTokenized);

        } catch (PDVTokenizerException e) {
            logger.error("[{}] function error PDVTokenizerException at {} : {}",
                    context.getFunctionName(), LocalDateTime.now(), e.getMessage());
        } catch (Exception e) {
            logger.error("[{}] function error Generic exception at {} : {}",
                    context.getFunctionName(), LocalDateTime.now(), e.getMessage());
        }

    }
}
