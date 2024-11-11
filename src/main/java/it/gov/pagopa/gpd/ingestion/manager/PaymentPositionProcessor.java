package it.gov.pagopa.gpd.ingestion.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.Cardinality;
import com.microsoft.azure.functions.annotation.EventHubOutput;
import com.microsoft.azure.functions.annotation.EventHubTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import it.gov.pagopa.gpd.ingestion.manager.entity.PaymentPosition;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerUnexpectedException;
import it.gov.pagopa.gpd.ingestion.manager.model.DataCaptureMessage;
import it.gov.pagopa.gpd.ingestion.manager.service.PDVTokenizerService;
import it.gov.pagopa.gpd.ingestion.manager.service.impl.PDVTokenizerServiceImpl;
import it.gov.pagopa.gpd.ingestion.manager.utils.ObjectMapperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static it.gov.pagopa.gpd.ingestion.manager.utils.ValidationUtils.isValidFiscalCode;

/**
 * Azure Functions with Azure EventHub trigger.
 */
public class PaymentPositionProcessor {
    private final Logger logger = LoggerFactory.getLogger(PaymentPositionProcessor.class);

    private final PDVTokenizerService pdvTokenizerService;

    public PaymentPositionProcessor() {
        this.pdvTokenizerService = new PDVTokenizerServiceImpl();
    }

    PaymentPositionProcessor(PDVTokenizerService pdvTokenizerService) {
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
            String paymentPositionMsg,
            @EventHubOutput(
                    name = "PaymentPositionOutput",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "PAYMENT_POSITION_OUTPUT_EVENTHUB_CONN_STRING")
            OutputBinding<List<DataCaptureMessage<PaymentPosition>>> paymentPositionProcessed,
            final ExecutionContext context) {

        logger.info("[{}] function called at {} for payment positions",
                context.getFunctionName(), LocalDateTime.now());

        List<DataCaptureMessage<PaymentPosition>> paymentPositionList = new ArrayList<>();
        try {
            paymentPositionList = ObjectMapperUtils.mapDataCapturePaymentPositionListString(paymentPositionMsg, new TypeReference<>() {
            }).stream().filter(Objects::nonNull).toList();
        } catch (JsonProcessingException e) {
            logger.error("[{}] function error JsonProcessingException at {} : {}",
                    context.getFunctionName(), LocalDateTime.now(), e.getMessage());
        }

        logger.info("[{}] function called at {} for payment positions with events list size {}",
                context.getFunctionName(), LocalDateTime.now(), paymentPositionList.size());

        if (!paymentPositionList.isEmpty()) {
            // persist the item
            try {
                List<DataCaptureMessage<PaymentPosition>> paymentPositionsTokenized = new ArrayList<>();
                for (DataCaptureMessage<PaymentPosition> pp : paymentPositionList) {
                    if (pp == null) {
                        continue;
                    }
                    PaymentPosition valuesBefore = pp.getBefore();
                    PaymentPosition valuesAfter = pp.getAfter();

                    logger.info("[{}] function called at {} with payment position id {}",
                            context.getFunctionName(), LocalDateTime.now(), (valuesAfter != null ? valuesAfter : valuesBefore).getId());

                    // tokenize fiscal codes
                    if (valuesBefore != null && isValidFiscalCode(valuesBefore.getFiscalCode())) {
                        valuesBefore.setFiscalCode(pdvTokenizerService.generateTokenForFiscalCode(valuesBefore.getFiscalCode()));
                        pp.setBefore(valuesBefore);
                    }
                    if (valuesAfter != null && isValidFiscalCode(valuesAfter.getFiscalCode())) {
                        valuesAfter.setFiscalCode(pdvTokenizerService.generateTokenForFiscalCode(valuesAfter.getFiscalCode()));
                        pp.setAfter(valuesAfter);
                    }

                    paymentPositionsTokenized.add(pp);
                }
                paymentPositionProcessed.setValue(paymentPositionsTokenized);

            } catch (PDVTokenizerException e) {
                logger.error("[{}] function error PDVTokenizerException at {} : {}",
                        context.getFunctionName(), LocalDateTime.now(), e.getMessage());
            } catch (PDVTokenizerUnexpectedException e) {
                logger.error("[{}] function error PDVTokenizerUnexpectedException at {} : {}",
                        context.getFunctionName(), LocalDateTime.now(), e.getMessage());
            } catch (Exception e) {
                logger.error("[{}] function error Generic exception at {} : {}",
                        context.getFunctionName(), LocalDateTime.now(), e.getMessage());
            }
        }
    }
}
