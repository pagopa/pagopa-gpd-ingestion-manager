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
import it.gov.pagopa.gpd.ingestion.manager.exception.*;
import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.EntityType;
import it.gov.pagopa.gpd.ingestion.manager.service.AnonymizerServiceRetryWrapper;
import it.gov.pagopa.gpd.ingestion.manager.service.IngestionService;
import it.gov.pagopa.gpd.ingestion.manager.service.PDVTokenizerServiceRetryWrapper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import static it.gov.pagopa.gpd.ingestion.manager.util.MDCUtility.*;

@Service
@Slf4j
public class IngestionServiceImpl implements IngestionService {

    private static final String PDV_TOKENIZER_EXCEPTION_MESSAGE =
            "PaymentOption ingestion error PDVTokenizerException at {}";
    private static final String PDV_CF_TOKENIZER = "PDV_CF_TOKENIZER";

    private static final String ANONYMIZER_EXCEPTION_MESSAGE =
            "Transfer ingestion error AnonymizerException at {}";
    private static final String ANONYMIZE_PLACEHOLDER = "Anonymized";

    private static final Pattern PATTERN_CF = Pattern.compile(
            "^[A-Z]{6}[0-9LMNPQRSTUV]{2}[ABCDEHLMPRST][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z]$"
    );
    private static final Pattern PATTERN_IVA = Pattern.compile("^\\d{11}$");

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
        this.paymentPositionProducer = paymentPositionProducer;
        this.paymentOptionProducer = paymentOptionProducer;
        this.transferProducer = transferProducer;
        this.placeholderOnPdvKO = placeholderOnPdvKO;
        this.anonymizerService = anonymizerService;
        this.placeholderOnAnonymizerKO = placeholderOnAnonymizerKO;
    }

    private static boolean isValidFiscalCode(String fiscalCode) {
        if (fiscalCode != null && !fiscalCode.isEmpty()) {
            String normalizedFiscalCode = fiscalCode.toUpperCase().trim();
            return PATTERN_CF.matcher(normalizedFiscalCode).find() || PATTERN_IVA.matcher(normalizedFiscalCode).find();
        }

        return false;
    }

    public void ingestPaymentPosition(Message<String> message) {
        logIngestionInit(EntityType.PAYMENT_POSITION.name());

        // persist the item
        try {
            initMDC(EntityType.PAYMENT_POSITION.name());

            DataCaptureMessage<PaymentPosition> paymentPosition =
                    mapMessageToObject(message, new TypeReference<DataCaptureMessage<PaymentPosition>>() {
                    });

            if (paymentPosition == null) {
                setMDCId("null");
                return;
            }
            PaymentPosition valuesBefore = paymentPosition.getBefore();
            PaymentPosition valuesAfter = paymentPosition.getAfter();
            int id = (valuesAfter != null ? valuesAfter : valuesBefore).getId();
            log.debug("PaymentPosition ingestion called at {} with payment position id {}", getDateNow(), id);
            setMDCId(String.valueOf(id));

            paymentPositionProducer.sendIngestedPaymentPosition(paymentPosition);
            setMDCSendResult("OK");
        } catch (JsonProcessingException e) {
            handleException(e, EntityType.PAYMENT_POSITION.name());
            throw new AppException(AppError.JSON_NOT_PROCESSABLE, e);
        } catch (AppException e) {
            handleException(e, EntityType.PAYMENT_POSITION.name());
            throw e;
        } catch (Exception e) {
            handleException(e, EntityType.PAYMENT_POSITION.name());
            throw new AppException(AppError.INTERNAL_SERVER_ERROR, e);
        } finally {
            clearMDC();
        }
    }

    public void ingestPaymentOption(Message<String> message) {
        logIngestionInit(EntityType.PAYMENT_OPTION.name());

        // persist the item
        try {
            initMDC(EntityType.PAYMENT_OPTION.name());

            DataCaptureMessage<PaymentOption> paymentOption =
                    mapMessageToObject(message, new TypeReference<DataCaptureMessage<PaymentOption>>() {
                    });

            if (paymentOption == null) {
                setMDCId("null");
                return;
            }
            PaymentOption valuesBefore = paymentOption.getBefore();
            PaymentOption valuesAfter = paymentOption.getAfter();
            int id = (valuesAfter != null ? valuesAfter : valuesBefore).getId();

            log.debug(
                    "PaymentOption ingestion called at {} with payment position id {}",
                    getDateNow(),
                    id);
            setMDCId(String.valueOf(id));

            paymentOption.setBefore(tokenizeFiscalCode(valuesBefore));
            paymentOption.setAfter(tokenizeFiscalCode(valuesAfter));

            paymentOptionProducer.sendIngestedPaymentOption(paymentOption);
            setMDCSendResult("OK");
        } catch (JsonProcessingException e) {
            handleException(e, EntityType.PAYMENT_OPTION.name());
            throw new AppException(AppError.JSON_NOT_PROCESSABLE, e);
        } catch (AppException e) {
            handleException(e, EntityType.PAYMENT_OPTION.name());
            throw e;
        } catch (PDVTokenizerException | PDVTokenizerUnexpectedException e) {
            handleException(e, EntityType.PAYMENT_OPTION.name());
            throw new AppException(AppError.ERROR_TOKENIZING_FISCAL_CODE, e);
        } catch (Exception e) {
            handleException(e, EntityType.PAYMENT_OPTION.name());
            throw new AppException(AppError.INTERNAL_SERVER_ERROR, e);
        } finally {
            clearMDC();
        }
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

    public void ingestTransfer(Message<String> message) {
        logIngestionInit(EntityType.TRANSFER.name());

        // persist the item
        try {
            initMDC(EntityType.TRANSFER.name());

            DataCaptureMessage<Transfer> transfer =
                    mapMessageToObject(message, new TypeReference<DataCaptureMessage<Transfer>>() {
                    });

            if (transfer == null) {
                setMDCId("null");
                return;
            }

            Transfer valuesBefore = transfer.getBefore();
            Transfer valuesAfter = transfer.getAfter();
            int id = (valuesAfter != null ? valuesAfter : valuesBefore).getId();

            log.debug(
                    "Transfer ingestion called at {} with payment position id {}",
                    getDateNow(),
                    id);
            setMDCId(String.valueOf(id));

            transfer.setBefore(anonymizeRemittanceInformation(valuesBefore));
            transfer.setAfter(anonymizeRemittanceInformation(valuesAfter));

            transferProducer.sendIngestedTransfer(transfer);
            setMDCSendResult("OK");
        } catch (JsonProcessingException e) {
            handleException(e, EntityType.TRANSFER.name());
            throw new AppException(AppError.JSON_NOT_PROCESSABLE, e);
        } catch (AppException e) {
            handleException(e, EntityType.TRANSFER.name());
            throw e;
        } catch (AnonymizerException | AnonymizerUnexpectedException e) {
            handleException(e, EntityType.TRANSFER.name());
            throw new AppException(AppError.ERROR_ANONYMIZING_REMITTANCE_INFORMATION, e);
        } catch (Exception e) {
            handleException(e, EntityType.TRANSFER.name());
            throw new AppException(AppError.INTERNAL_SERVER_ERROR, e);
        } finally {
            clearMDC();
        }
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

    private <T> DataCaptureMessage<T> mapMessageToObject(Message<?> message, TypeReference<DataCaptureMessage<T>> typeReference) throws JsonProcessingException {
        // Discard null messages
        if (message.getHeaders().getId() == null
                || !(message.getPayload() instanceof String msg)
                || msg.isBlank()
        ) {
            log.debug("NULL message ignored at {}", getDateNow());
            return null;
        }

        return this.objectMapper.readValue(msg, typeReference);
    }

    private static LocalDateTime getDateNow() {
        return LocalDateTime.now(Clock.systemDefaultZone());
    }

    private static void logIngestionInit(String entityName) {
        log.debug(
                "{} ingestion called at {}",
                entityName,
                getDateNow());
    }

    /**
     * Custom exceptions are
     * {@link PDVTokenizerException}
     * {@link PDVTokenizerUnexpectedException}
     */
    private static void handleException(Exception e, String entityName) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        String errorType = cause.getClass().getSimpleName();
        setMDCError(errorType, cause.getMessage());
        log.error("{} ingestion error {} at {}", entityName, errorType, getDateNow(), e);
    }
}