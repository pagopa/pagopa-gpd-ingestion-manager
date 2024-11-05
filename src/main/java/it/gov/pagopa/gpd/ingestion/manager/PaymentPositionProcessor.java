package it.gov.pagopa.gpd.ingestion.manager;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import it.gov.pagopa.gpd.ingestion.manager.entity.PaymentPosition;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
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
     * This function will be invoked when an Event Hub trigger occurs
     */
    @FunctionName("EventHubPaymentPositionProcessor")
    public void processPaymentPosition(
            @EventHubTrigger(
                    name = "PaymentPositionTrigger",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "PAYMENT_POSITION_INPUT_EVENTHUB_CONN_STRING",
                    cardinality = Cardinality.MANY)
            List<PaymentPosition> paymentPositionMsg,
            @EventHubOutput(
                    name = "PaymentPositionOutput",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "PAYMENT_POSITION_OUTPUT_EVENTHUB_CONN_STRING")
            OutputBinding<List<PaymentPosition>> paymentPositionProcessed,
            final ExecutionContext context) {

        String message = String.format("PaymentPositionProcessor function called at %s with events list size %s", LocalDateTime.now(), paymentPositionMsg.size());
        logger.info(message);

        // persist the item
        try {
            List<PaymentPosition> paymentPositionsTokenized = new ArrayList<>();
            for (PaymentPosition pp : paymentPositionMsg) {
                String msg = String.format("PaymentPositionProcessor function called at %s with object id %s",
                        LocalDateTime.now(), pp.getId());
                logger.info(msg);

                // tokenize fiscal code
                if (isValidFiscalCode(pp.getFiscalCode())) {
                    pp.setFiscalCode(pdvTokenizerService.generateTokenForFiscalCodeWithRetry(pp.getFiscalCode()));
                }

                paymentPositionsTokenized.add(pp);
            }
            paymentPositionProcessed.setValue(paymentPositionsTokenized);

        } catch (NullPointerException e) {
            logger.error(String.format("NullPointerException exception on paymentPosition msg ingestion at %s : %s", LocalDateTime.now(), e.getMessage()));
        } catch (PDVTokenizerException e) {
            logger.error(String.format("PDVTokenizerException on paymentPosition msg ingestion at %s : %s", LocalDateTime.now(), e.getMessage()));
        } catch (Exception e) {
            logger.error(String.format("Generic exception on paymentPosition msg ingestion at %s : %s", LocalDateTime.now(), e.getMessage()));
        }

    }
}
