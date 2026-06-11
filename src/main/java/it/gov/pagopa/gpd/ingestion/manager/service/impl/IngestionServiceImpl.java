package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gpd.ingestion.manager.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.PaymentOption;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.PaymentPosition;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.Transfer;
import it.gov.pagopa.gpd.ingestion.manager.events.producer.IngestedPaymentOptionProducer;
import it.gov.pagopa.gpd.ingestion.manager.events.producer.IngestedPaymentPositionProducer;
import it.gov.pagopa.gpd.ingestion.manager.events.producer.IngestedTransferProducer;
import it.gov.pagopa.gpd.ingestion.manager.exception.AnonymizerException;
import it.gov.pagopa.gpd.ingestion.manager.exception.AnonymizerUnexpectedException;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerUnexpectedException;
import it.gov.pagopa.gpd.ingestion.manager.service.AnonymizerServiceRetryWrapper;
import it.gov.pagopa.gpd.ingestion.manager.service.IngestionService;
import it.gov.pagopa.gpd.ingestion.manager.service.PDVTokenizerServiceRetryWrapper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IngestionServiceImpl implements IngestionService {

    private static final String PDV_TOKENIZER_EXCEPTION_MESSAGE =
            "PaymentOption ingestion error PDVTokenizerException at {}";
    private static final String PDV_CF_TOKENIZER = "PDV_CF_TOKENIZER";

    private static final String ANONYMIZER_EXCEPTION_MESSAGE =
            "Transfer ingestion error AnonymizerException at {}";
    private static final String ANONYMIZE_PLACEHOLDER = "Anonymized";
    private static final String TRANSFER_ENTITY_NAME = "Transfer";
    private static final String PAYMENT_OPTION_ENTITY_NAME = "PaymentOption";
    private static final String PAYMENT_POSITION_ENTITY_NAME = "PaymentPosition";

    private static final Pattern PATTERN_CF = Pattern.compile("^[A-Z]{6}[0-9LMNPQRSTUV]{2}[ABCDEHLMPRST][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z]$");
    private static final Pattern PATTERN_PIVA = Pattern.compile("^\\d{11}$");

    private final ObjectMapper objectMapper;

    private final PDVTokenizerServiceRetryWrapper pdvTokenizerService;
    private final AnonymizerServiceRetryWrapper anonymizerService;

    private final IngestedPaymentPositionProducer paymentPositionProducer;
    private final IngestedPaymentOptionProducer paymentOptionProducer;

    private final IngestedTransferProducer transferProducer;

    private final Boolean placeholderOnPdvKO;
    private final Boolean placeholderOnAnonymizerKO;

    @Autowired
    public IngestionServiceImpl(
            ObjectMapper objectMapper,
            PDVTokenizerServiceRetryWrapper pdvTokenizerService,
            AnonymizerServiceRetryWrapper anonymizerService,
            IngestedPaymentPositionProducer paymentPositionProducer,
            IngestedPaymentOptionProducer paymentOptionProducer,
            IngestedTransferProducer transferProducer,
            @Value("${pdv.tokenizer.placeholderOnPdvKO}") Boolean placeholderOnPdvKO,
            @Value("${anonymizer.placeholderOnAnonymizerKO}") Boolean placeholderOnAnonymizerKO
    ) {
        this.objectMapper = objectMapper;
        this.pdvTokenizerService = pdvTokenizerService;
        this.anonymizerService = anonymizerService;
        this.paymentPositionProducer = paymentPositionProducer;
        this.paymentOptionProducer = paymentOptionProducer;
        this.transferProducer = transferProducer;
        this.placeholderOnPdvKO = placeholderOnPdvKO;
        this.placeholderOnAnonymizerKO = placeholderOnAnonymizerKO;
    }

    private static boolean isValidFiscalCode(String fiscalCode) {
        if (fiscalCode != null && !fiscalCode.isEmpty()) {
            return PATTERN_CF.matcher(fiscalCode).find() || PATTERN_PIVA.matcher(fiscalCode).find();
        }

        return false;
    }

    public void ingestPaymentPositions(List<String> messages) {
        logIngestionInit(messages, PAYMENT_POSITION_ENTITY_NAME);

        int nullMessages = 0;
        int errorMessages = 0;

        // persist the item
        for (String msg : messages) {
            initMDC(PAYMENT_POSITION_ENTITY_NAME);
            try {
                DataCaptureMessage<PaymentPosition> paymentPosition =
                        mapMessageToObject(msg, new TypeReference<DataCaptureMessage<PaymentPosition>>() {
                        });

                if (paymentPosition == null) {
                    nullMessages = handleNullMessage(nullMessages);
                    continue;
                }
                PaymentPosition valuesBefore = paymentPosition.getBefore();
                PaymentPosition valuesAfter = paymentPosition.getAfter();
                int id = (valuesAfter != null ? valuesAfter : valuesBefore).getId();
                log.debug("PaymentPosition ingestion called at {} with payment position id {}", getDateNow(), id);
                MDC.put("id", String.valueOf(id));

                boolean response = paymentPositionProducer.sendIngestedPaymentPosition(paymentPosition);
                errorMessages = verifySendToEventhub(response, errorMessages, PAYMENT_POSITION_ENTITY_NAME);
            } catch (Exception e) {
                errorMessages = handleException(e, errorMessages, PAYMENT_POSITION_ENTITY_NAME);
            } finally {
                MDC.clear();
            }
        }

        logTotalMessagesElaborated(PAYMENT_POSITION_ENTITY_NAME, messages, nullMessages, errorMessages);
    }

    public void ingestPaymentOptions(List<String> messages) {
        logIngestionInit(messages, PAYMENT_OPTION_ENTITY_NAME);

        int nullMessages = 0;
        int errorMessages = 0;

        // persist the item
        for (String msg : messages) {
            initMDC(PAYMENT_OPTION_ENTITY_NAME);
            try {
                DataCaptureMessage<PaymentOption> paymentOption =
                        mapMessageToObject(msg, new TypeReference<DataCaptureMessage<PaymentOption>>() {
                        });

                if (paymentOption == null) {
                    nullMessages = handleNullMessage(nullMessages);
                    continue;
                }
                PaymentOption valuesBefore = paymentOption.getBefore();
                PaymentOption valuesAfter = paymentOption.getAfter();
                int id = (valuesAfter != null ? valuesAfter : valuesBefore).getId();

                log.debug(
                        "PaymentOption ingestion called at {} with payment position id {}",
                        getDateNow(),
                        id);
                MDC.put("id", String.valueOf(id));

                paymentOption.setBefore(tokenizeFiscalCode(valuesBefore));
                paymentOption.setAfter(tokenizeFiscalCode(valuesAfter));

                boolean response = paymentOptionProducer.sendIngestedPaymentOption(paymentOption);
                errorMessages = verifySendToEventhub(response, errorMessages, PAYMENT_OPTION_ENTITY_NAME);
            } catch (Exception e) {
                errorMessages = handleException(e, errorMessages, PAYMENT_OPTION_ENTITY_NAME);
            } finally {
                MDC.clear();
            }
        }

        logTotalMessagesElaborated(PAYMENT_OPTION_ENTITY_NAME, messages, nullMessages, errorMessages);
    }

    private PaymentOption tokenizeFiscalCode(PaymentOption values) throws PDVTokenizerException, JsonProcessingException {
        if (values != null && isValidFiscalCode(values.getFiscalCode())) {
            try {
                values.setFiscalCode(
                        pdvTokenizerService.generateTokenForFiscalCodeWithRetry(
                                values.getFiscalCode()));
            } catch (Exception e) {
                if (Boolean.FALSE.equals(placeholderOnPdvKO)) {
                    throw e;
                } else {
                    log.error(PDV_TOKENIZER_EXCEPTION_MESSAGE, getDateNow(), e);
                    values.setFiscalCode(PDV_CF_TOKENIZER);
                }
            }
        }

        return values;
    }

    public void ingestTransfers(List<String> messages) {
        logIngestionInit(messages, TRANSFER_ENTITY_NAME);

        int nullMessages = 0;
        int errorMessages = 0;

        // persist the item
        for (String msg : messages) {
            initMDC(TRANSFER_ENTITY_NAME);
            try {
                DataCaptureMessage<Transfer> transfer =
                        mapMessageToObject(msg, new TypeReference<DataCaptureMessage<Transfer>>() {
                        });

                if (transfer == null) {
                    nullMessages = handleNullMessage(nullMessages);
                    continue;
                }

                Transfer valuesBefore = transfer.getBefore();
                Transfer valuesAfter = transfer.getAfter();
                int id = (valuesAfter != null ? valuesAfter : valuesBefore).getId();

                log.debug(
                        "Transfer ingestion called at {} with payment position id {}",
                        getDateNow(),
                        id);
                MDC.put("id", String.valueOf(id));

                transfer.setBefore(anonymizeRemittanceInformation(valuesBefore));
                transfer.setAfter(anonymizeRemittanceInformation(valuesAfter));

                boolean response = transferProducer.sendIngestedTransfer(transfer);
                errorMessages = verifySendToEventhub(response, errorMessages, TRANSFER_ENTITY_NAME);
            } catch (Exception e) {
                errorMessages = handleException(e, errorMessages, TRANSFER_ENTITY_NAME);
            } finally {
                MDC.clear();
            }
        }
        logTotalMessagesElaborated(TRANSFER_ENTITY_NAME, messages, nullMessages, errorMessages);
    }

    private Transfer anonymizeRemittanceInformation(Transfer values) throws AnonymizerException, JsonProcessingException {
        if (values != null && values.getRemittanceInformation() != null && !values.getRemittanceInformation().isBlank()) {
            try {
                values.setRemittanceInformation(
                        anonymizerService.anonymizeWithRetry(
                                values.getRemittanceInformation()));
            } catch (Exception e) {
                if (Boolean.FALSE.equals(placeholderOnAnonymizerKO)) {
                    throw e;
                } else {
                    log.error(ANONYMIZER_EXCEPTION_MESSAGE, getDateNow(), e);
                    values.setRemittanceInformation(ANONYMIZE_PLACEHOLDER);
                }
            }
        }
        return values;
    }

    private static LocalDateTime getDateNow() {
        return LocalDateTime.now(Clock.systemDefaultZone());
    }

    private static void initMDC(String entityName) {
        MDC.put("requestId", String.valueOf(UUID.randomUUID()));
        MDC.put("entity", entityName);
    }

    private <T> DataCaptureMessage<T> mapMessageToObject(String msg, TypeReference<DataCaptureMessage<T>> typeReference) throws JsonProcessingException {
        if (msg == null || msg.isBlank()) {
            return null;
        }
        return this.objectMapper.readValue(msg, typeReference);
    }

    private static int handleNullMessage(int nullMessages) {
        MDC.put("id", "null");
        nullMessages += 1;
        return nullMessages;
    }

    private static int verifySendToEventhub(boolean response, int errorMessages, String entityName) {
        MDC.put("sendResult", response ? "OK" : "KO");
        if (response) {
            log.debug("{} ingestion sent to eventhub at {}", entityName, getDateNow());
        } else {
            errorMessages += 1;
            log.error(
                    "{} ingestion unable to send to eventhub at {}", entityName, getDateNow());
        }
        return errorMessages;
    }

    private static void logIngestionInit(List<String> messages, String entityName) {
        log.debug(
                "{} ingestion called at {} with events list size {}",
                entityName,
                getDateNow(),
                messages.size());
    }

    private static void logTotalMessagesElaborated(String entityName, List<String> messages, int nullMessages, int errorMessages) {
        log.debug(
                "{} ingested at {}: total messages {}, {} null and {} errors",
                entityName,
                getDateNow(),
                messages.size(),
                nullMessages,
                errorMessages);
    }

    /**
     * Custom exceptions are
     * {@link PDVTokenizerException}
     * {@link PDVTokenizerUnexpectedException}
     * {@link AnonymizerException}
     * {@link AnonymizerUnexpectedException}
     */
    private static int handleException(Exception e, int errorMessages, String entityName) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        String errorType = cause.getClass().getSimpleName();
        MDC.put("errorType", errorType);
        MDC.put("errorMessage", cause.getMessage());
        errorMessages += 1;
        log.error("{} ingestion error {} at {}", entityName, errorType, getDateNow(), e);
        return errorMessages;
    }
}
