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
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerUnexpectedException;
import it.gov.pagopa.gpd.ingestion.manager.service.IngestionService;
import it.gov.pagopa.gpd.ingestion.manager.service.PDVTokenizerServiceRetryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class IngestionServiceImpl implements IngestionService {

    public static final String PAYMENT_OPTION_PDV_TOKENIZER_EXCEPTION_MESSAGE = "PaymentOption ingestion error PDVTokenizerException at {}";
    public static final String PDV_CF_TOKENIZER = "PDV_CF_TOKENIZER";
    public static final String PAYMENT_POSITION_PDV_TOKENIZER_EXCEPTION_MESSAGE = "PaymentPosition ingestion error PDVTokenizerException at {}";
    private final ObjectMapper objectMapper;
    private final PDVTokenizerServiceRetryWrapper pdvTokenizerService;
    private final IngestedPaymentPositionProducer paymentPositionProducer;
    private final IngestedPaymentOptionProducer paymentOptionProducer;

    private final IngestedTransferProducer transferProducer;

    private final Boolean placeholderOnPdvKO;

    @Autowired
    public IngestionServiceImpl(
            ObjectMapper objectMapper,
            PDVTokenizerServiceRetryWrapper pdvTokenizerService,
            IngestedPaymentPositionProducer paymentPositionProducer,
            IngestedPaymentOptionProducer paymentOptionProducer,
            IngestedTransferProducer transferProducer,
            @Value("${pdv.tokenizer.placeholderOnPdvKO}") Boolean placeholderOnPdvKO) {
        this.objectMapper = objectMapper;
        this.pdvTokenizerService = pdvTokenizerService;
        this.paymentPositionProducer = paymentPositionProducer;
        this.paymentOptionProducer = paymentOptionProducer;
        this.transferProducer = transferProducer;
        this.placeholderOnPdvKO = placeholderOnPdvKO;
    }

    private static boolean isValidFiscalCode(String fiscalCode) {
        if (fiscalCode != null && !fiscalCode.isEmpty()) {
            Pattern patternCF =
                    Pattern.compile(
                            "^[A-Z]{6}[0-9LMNPQRSTUV]{2}[ABCDEHLMPRST][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z]$");
            Pattern patternPIVA = Pattern.compile("/^[0-9]{11}$/");

            return patternCF.matcher(fiscalCode).find() || patternPIVA.matcher(fiscalCode).find();
        }

        return false;
    }

    public void ingestPaymentPositions(List<String> messages) {
        log.debug(
                "PaymentPosition ingestion called at {} for payment positions with events list size {}",
                LocalDateTime.now(),
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
                        LocalDateTime.now(),
                        (valuesAfter != null ? valuesAfter : valuesBefore).getId());

                // tokenize fiscal codes
                if (valuesBefore != null && isValidFiscalCode(valuesBefore.getFiscalCode())) {
                    try {
                        valuesBefore.setFiscalCode(
                                pdvTokenizerService.generateTokenForFiscalCodeWithRetry(
                                        valuesBefore.getFiscalCode()));
                        paymentPosition.setBefore(valuesBefore);
                    } catch (Exception e) {
                        if (Boolean.FALSE.equals(placeholderOnPdvKO)) {
                            throw e;
                        } else {
                            log.error(
                                    PAYMENT_POSITION_PDV_TOKENIZER_EXCEPTION_MESSAGE, LocalDateTime.now(), e);
                            valuesBefore.setFiscalCode(PDV_CF_TOKENIZER);
                            paymentPosition.setBefore(valuesBefore);
                        }
                    }
                }
                if (valuesAfter != null && isValidFiscalCode(valuesAfter.getFiscalCode())) {
                    try {
                        valuesAfter.setFiscalCode(
                                pdvTokenizerService.generateTokenForFiscalCodeWithRetry(valuesAfter.getFiscalCode()));
                        paymentPosition.setAfter(valuesAfter);
                    } catch (Exception e) {
                        if (Boolean.FALSE.equals(placeholderOnPdvKO)) {
                            throw e;
                        } else {
                            log.error(
                                    PAYMENT_POSITION_PDV_TOKENIZER_EXCEPTION_MESSAGE, LocalDateTime.now(), e);
                            valuesAfter.setFiscalCode(PDV_CF_TOKENIZER);
                            paymentPosition.setAfter(valuesAfter);
                        }
                    }
                }

                boolean response = paymentPositionProducer.sendIngestedPaymentPosition(paymentPosition);

                if (response) {
                    log.debug("PaymentPosition ingestion sent to eventhub at {}", LocalDateTime.now());
                } else {
                    errorMessages += 1;
                    log.error(
                            "PaymentPosition ingestion unable to send to eventhub at {}", LocalDateTime.now());
                }
            } catch (JsonProcessingException e) {
                errorMessages += 1;
                log.error(
                        "PaymentPosition ingestion error JsonProcessingException at {}",
                        LocalDateTime.now(),
                        e);
            } catch (PDVTokenizerException e) {
                errorMessages += 1;
                log.error(
                        PAYMENT_POSITION_PDV_TOKENIZER_EXCEPTION_MESSAGE, LocalDateTime.now(), e);
            } catch (PDVTokenizerUnexpectedException e) {
                errorMessages += 1;
                log.error(
                        "PaymentPosition ingestion error PDVTokenizerUnexpectedException at {}",
                        LocalDateTime.now(),
                        e);
            } catch (Exception e) {
                errorMessages += 1;
                log.error(
                        "PaymentPosition ingestion error Generic exception at {}", LocalDateTime.now(), e);
            }
        }

        log.debug(
                "PaymentPosition ingested at {}: total messages {}, {} null and {} errors",
                LocalDateTime.now(),
                messages.size(),
                nullMessages,
                errorMessages);
    }

    public void ingestPaymentOptions(List<String> messages) {
        log.debug(
                "PaymentOption ingestion called at {} for payment positions with events list size {}",
                LocalDateTime.now(),
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
                        LocalDateTime.now(),
                        (valuesAfter != null ? valuesAfter : valuesBefore).getId());

                // tokenize fiscal codes
                if (valuesBefore != null && isValidFiscalCode(valuesBefore.getFiscalCode())) {
                    try {
                        valuesBefore.setFiscalCode(
                                pdvTokenizerService.generateTokenForFiscalCodeWithRetry(
                                        valuesBefore.getFiscalCode()));
                        paymentOption.setBefore(valuesBefore);
                    } catch (Exception e) {
                        if (Boolean.FALSE.equals(placeholderOnPdvKO)) {
                            throw e;
                        } else {
                            log.error(
                                    PAYMENT_OPTION_PDV_TOKENIZER_EXCEPTION_MESSAGE, LocalDateTime.now(), e);
                            valuesBefore.setFiscalCode(PDV_CF_TOKENIZER);
                            paymentOption.setBefore(valuesBefore);
                        }
                    }
                }
                if (valuesAfter != null && isValidFiscalCode(valuesAfter.getFiscalCode())) {
                    try {
                        valuesAfter.setFiscalCode(
                                pdvTokenizerService.generateTokenForFiscalCodeWithRetry(valuesAfter.getFiscalCode()));
                        paymentOption.setAfter(valuesAfter);
                    } catch (Exception e) {
                        if (Boolean.FALSE.equals(placeholderOnPdvKO)) {
                            throw e;
                        } else {
                            log.error(
                                    PAYMENT_OPTION_PDV_TOKENIZER_EXCEPTION_MESSAGE, LocalDateTime.now(), e);
                            valuesAfter.setFiscalCode(PDV_CF_TOKENIZER);
                            paymentOption.setAfter(valuesAfter);
                        }
                    }
                }

                boolean response = paymentOptionProducer.sendIngestedPaymentOption(paymentOption);

                if (response) {
                    log.debug("PaymentOption ingestion sent to eventhub at {}", LocalDateTime.now());
                } else {
                    errorMessages += 1;
                    log.error(
                            "PaymentOption ingestion unable to send to eventhub at {}", LocalDateTime.now());
                }
            } catch (JsonProcessingException e) {
                errorMessages += 1;
                log.error(
                        "PaymentOption ingestion error JsonProcessingException at {}", LocalDateTime.now(), e);
            } catch (PDVTokenizerException e) {
                errorMessages += 1;
                log.error(
                        PAYMENT_OPTION_PDV_TOKENIZER_EXCEPTION_MESSAGE, LocalDateTime.now(), e);
            } catch (PDVTokenizerUnexpectedException e) {
                errorMessages += 1;
                log.error(
                        "PaymentOption ingestion error PDVTokenizerUnexpectedException at {}",
                        LocalDateTime.now(),
                        e);
            } catch (Exception e) {
                errorMessages += 1;
                log.error("PaymentOption ingestion error Generic exception at {}", LocalDateTime.now(), e);
            }
        }

        log.debug(
                "PaymentOption ingested at {}: total messages {}, {} null and {} errors",
                LocalDateTime.now(),
                messages.size(),
                nullMessages,
                errorMessages);
    }

    public void ingestTransfers(List<String> messages) {
        log.debug(
                "Transfer ingestion called at {} for payment positions with events list size {}",
                LocalDateTime.now(),
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
                        LocalDateTime.now(),
                        (valuesAfter != null ? valuesAfter : valuesBefore).getId());

                boolean response = transferProducer.sendIngestedTransfer(transfer);

                if (response) {
                    log.debug("Transfer ingestion sent to eventhub at {}", LocalDateTime.now());
                } else {
                    errorMessages += 1;
                    log.error("Transfer ingestion unable to send to eventhub at {}", LocalDateTime.now());
                }
            } catch (JsonProcessingException e) {
                errorMessages += 1;
                log.error("Transfer ingestion error JsonProcessingException at {}", LocalDateTime.now(), e);
            } catch (Exception e) {
                errorMessages += 1;
                log.error("Transfer ingestion error Generic exception at {}", LocalDateTime.now(), e);
            }
        }
        log.debug(
                "Transfer ingested at {}: total messages {}, {} null and {} errors",
                LocalDateTime.now(),
                messages.size(),
                nullMessages,
                errorMessages);
    }
}
