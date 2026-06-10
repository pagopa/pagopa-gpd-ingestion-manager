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
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
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
            Pattern patternCF =
                    Pattern.compile(
                            "^[A-Z]{6}[0-9LMNPQRSTUV]{2}[ABCDEHLMPRST][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z]$");
            Pattern patternPIVA = Pattern.compile("^\\d{11}$");

            return patternCF.matcher(fiscalCode).find() || patternPIVA.matcher(fiscalCode).find();
        }

        return false;
    }

    public void ingestPaymentPositions(List<String> messages) {
        log.debug(
                "PaymentPosition ingestion called at {} for payment positions with events list size {}",
                getDateNow(),
                messages.size());
        int nullMessages = 0;
        int errorMessages = 0;
        messages.removeAll(Collections.singleton(null));
        // persist the item
        for (String msg : messages) {
            try {
                DataCaptureMessage<PaymentPosition> paymentPosition =
                        objectMapper.readValue(
                                msg, new TypeReference<DataCaptureMessage<PaymentPosition>>() {
                                });

                if (paymentPosition == null) {
                    nullMessages += 1;
                    continue;
                }
                PaymentPosition valuesBefore = paymentPosition.getBefore();
                PaymentPosition valuesAfter = paymentPosition.getAfter();

                log.debug(
                        "PaymentPosition ingestion called at {} with payment position id {}",
                        getDateNow(),
                        (valuesAfter != null ? valuesAfter : valuesBefore).getId());

                boolean response = paymentPositionProducer.sendIngestedPaymentPosition(paymentPosition);

                if (response) {
                    log.debug("PaymentPosition ingestion sent to eventhub at {}", getDateNow());
                } else {
                    errorMessages += 1;
                    log.error(
                            "PaymentPosition ingestion unable to send to eventhub at {}", getDateNow());
                }
            } catch (JsonProcessingException e) {
                errorMessages += 1;
                log.error(
                        "PaymentPosition ingestion error JsonProcessingException at {}",
                        getDateNow(),
                        e);
            } catch (Exception e) {
                errorMessages += 1;
                log.error(
                        "PaymentPosition ingestion error Generic exception at {}", getDateNow(), e);
            }
        }

        log.debug(
                "PaymentPosition ingested at {}: total messages {}, {} null and {} errors",
                getDateNow(),
                messages.size(),
                nullMessages,
                errorMessages);
    }

    public void ingestPaymentOptions(List<String> messages) {
        log.debug(
                "PaymentOption ingestion called at {} for payment positions with events list size {}",
                getDateNow(),
                messages.size());
        int nullMessages = 0;
        int errorMessages = 0;
        messages.removeAll(Collections.singleton(null));
        // persist the item
        for (String msg : messages) {
            try {
                DataCaptureMessage<PaymentOption> paymentOption =
                        objectMapper.readValue(msg, new TypeReference<DataCaptureMessage<PaymentOption>>() {
                        });

                if (paymentOption == null) {
                    nullMessages += 1;
                    continue;
                }
                PaymentOption valuesBefore = paymentOption.getBefore();
                PaymentOption valuesAfter = paymentOption.getAfter();

                log.debug(
                        "PaymentOption ingestion called at {} with payment position id {}",
                        getDateNow(),
                        (valuesAfter != null ? valuesAfter : valuesBefore).getId());

                paymentOption.setBefore(tokenizeFiscalCode(valuesBefore));
                paymentOption.setAfter(tokenizeFiscalCode(valuesAfter));

                boolean response = paymentOptionProducer.sendIngestedPaymentOption(paymentOption);

                if (response) {
                    log.debug("PaymentOption ingestion sent to eventhub at {}", getDateNow());
                } else {
                    errorMessages += 1;
                    log.error(
                            "PaymentOption ingestion unable to send to eventhub at {}", getDateNow());
                }
            } catch (JsonProcessingException e) {
                errorMessages += 1;
                log.error(
                        "PaymentOption ingestion error JsonProcessingException at {}", getDateNow(), e);
            } catch (PDVTokenizerException e) {
                errorMessages += 1;
                log.error(PDV_TOKENIZER_EXCEPTION_MESSAGE, getDateNow(), e);
            } catch (PDVTokenizerUnexpectedException e) {
                errorMessages += 1;
                log.error(
                        "PaymentOption ingestion error PDVTokenizerUnexpectedException at {}",
                        getDateNow(),
                        e);
            } catch (Exception e) {
                errorMessages += 1;
                log.error("PaymentOption ingestion error Generic exception at {}", getDateNow(), e);
            }
        }

        log.debug(
                "PaymentOption ingested at {}: total messages {}, {} null and {} errors",
                getDateNow(),
                messages.size(),
                nullMessages,
                errorMessages);
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
        log.debug(
                "Transfer ingestion called at {} for payment positions with events list size {}",
                getDateNow(),
                messages.size());

        int nullMessages = 0;
        int errorMessages = 0;
        messages.removeAll(Collections.singleton(null));
        // persist the item
        for (String msg : messages) {
            try {
                DataCaptureMessage<Transfer> transfer =
                        objectMapper.readValue(msg, new TypeReference<DataCaptureMessage<Transfer>>() {
                        });

                if (transfer == null) {
                    nullMessages += 1;
                    continue;
                }
                Transfer valuesBefore = transfer.getBefore();
                Transfer valuesAfter = transfer.getAfter();

                log.debug(
                        "Transfer ingestion called at {} with payment position id {}",
                        getDateNow(),
                        (valuesAfter != null ? valuesAfter : valuesBefore).getId());

                transfer.setBefore(anonymizeRemittanceInformation(valuesBefore));
                transfer.setAfter(anonymizeRemittanceInformation(valuesAfter));

                boolean response = transferProducer.sendIngestedTransfer(transfer);

                if (response) {
                    log.debug("Transfer ingestion sent to eventhub at {}", getDateNow());
                } else {
                    errorMessages += 1;
                    log.error("Transfer ingestion unable to send to eventhub at {}", getDateNow());
                }
            } catch (JsonProcessingException e) {
                errorMessages += 1;
                log.error("Transfer ingestion error JsonProcessingException at {}", getDateNow(), e);
            } catch (AnonymizerException e) {
                errorMessages += 1;
                log.error(ANONYMIZER_EXCEPTION_MESSAGE, getDateNow(), e);
            } catch (AnonymizerUnexpectedException e) {
                errorMessages += 1;
                log.error(
                        "Transfer ingestion error AnonymizerUnexpectedException at {}",
                        getDateNow(),
                        e);
            } catch (Exception e) {
                errorMessages += 1;
                log.error("Transfer ingestion error Generic exception at {}", getDateNow(), e);
            }
        }
        log.debug(
                "Transfer ingested at {}: total messages {}, {} null and {} errors",
                getDateNow(),
                messages.size(),
                nullMessages,
                errorMessages);
    }

    private Transfer anonymizeRemittanceInformation(Transfer values) throws AnonymizerException, JsonProcessingException {
        if (values != null && values.getRemittanceInformation() != null) {
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
}
