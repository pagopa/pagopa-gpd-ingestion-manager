package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class IngestionServiceImpl implements IngestionService {

    private final ObjectMapper objectMapper;
    private final PDVTokenizerServiceRetryWrapper pdvTokenizerService;
    private final IngestedPaymentPositionProducer paymentPositionProducer;
    private final IngestedPaymentOptionProducer paymentOptionProducer;

    private final IngestedTransferProducer transferProducer;

    @Autowired
    public IngestionServiceImpl(ObjectMapper objectMapper, PDVTokenizerServiceRetryWrapper pdvTokenizerService, IngestedPaymentPositionProducer paymentPositionProducer, IngestedPaymentOptionProducer paymentOptionProducer, IngestedTransferProducer transferProducer) {
        this.objectMapper = objectMapper;
        this.pdvTokenizerService = pdvTokenizerService;
        this.paymentPositionProducer = paymentPositionProducer;
        this.paymentOptionProducer = paymentOptionProducer;
        this.transferProducer = transferProducer;
    }

    private static boolean isValidFiscalCode(String fiscalCode) {
        if (fiscalCode != null && !fiscalCode.isEmpty()) {
            Pattern patternCF = Pattern.compile("^[A-Z]{6}[0-9LMNPQRSTUV]{2}[ABCDEHLMPRST][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z]$");
            Pattern patternPIVA = Pattern.compile("/^[0-9]{11}$/");

            return patternCF.matcher(fiscalCode).find() || patternPIVA.matcher(fiscalCode).find();
        }

        return false;
    }

    public void ingestPaymentPositions(List<String> messages) {
        log.info("PaymentPosition ingestion called at {} for payment positions with events list size {}",
                LocalDateTime.now(), messages.size());

        // persist the item
        try {
            for (String msg : messages) {
                DataCaptureMessage<PaymentPosition> paymentPosition = objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true).readValue(msg, new TypeReference<DataCaptureMessage<PaymentPosition>>() {});

                if (paymentPosition == null) {
                    continue;
                }
                PaymentPosition valuesBefore = paymentPosition.getBefore();
                PaymentPosition valuesAfter = paymentPosition.getAfter();

                log.info("PaymentPosition ingestion called at {} with payment position id {}",
                        LocalDateTime.now(), (valuesAfter != null ? valuesAfter : valuesBefore).getId());

                // tokenize fiscal codes
                if (valuesBefore != null && isValidFiscalCode(valuesBefore.getFiscalCode())) {
                    valuesBefore.setFiscalCode(pdvTokenizerService.generateTokenForFiscalCodeWithRetry(valuesBefore.getFiscalCode()));
                    paymentPosition.setBefore(valuesBefore);
                }
                if (valuesAfter != null && isValidFiscalCode(valuesAfter.getFiscalCode())) {
                    valuesAfter.setFiscalCode(pdvTokenizerService.generateTokenForFiscalCodeWithRetry(valuesAfter.getFiscalCode()));
                    paymentPosition.setAfter(valuesAfter);
                }

                boolean response = paymentPositionProducer.sendIngestedPaymentPosition(paymentPosition);

                if (response) {
                    log.info("PaymentPosition ingestion sent to eventhub at {}",
                            LocalDateTime.now());
                } else {
                    log.error("PaymentPosition ingestion unable to send to eventhub at {}",
                            LocalDateTime.now());
                }
            }
        } catch (JsonProcessingException e) {
            log.error("PaymentPosition ingestion error JsonProcessingException at {} : {}",
                    LocalDateTime.now(), e.getMessage());
        } catch (PDVTokenizerException e) {
            log.error("PaymentPosition ingestion error PDVTokenizerException at {} : {}",
                    LocalDateTime.now(), e.getMessage());
        } catch (PDVTokenizerUnexpectedException e) {
            log.error("PaymentPosition ingestion error PDVTokenizerUnexpectedException at {} : {}",
                    LocalDateTime.now(), e.getMessage());
        } catch (Exception e) {
            log.error("PaymentPosition ingestion error Generic exception at {} : {}",
                    LocalDateTime.now(), e.getMessage());
        }

    }

    public void ingestPaymentOptions(List<String> messages) {
        log.info("PaymentOption ingestion called at {} for payment positions with events list size {}",
                LocalDateTime.now(), messages.size());

        // persist the item
        try {
            for (String msg : messages) {
                DataCaptureMessage<PaymentOption> paymentOption = objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true).readValue(msg, new TypeReference<DataCaptureMessage<PaymentOption>>() {});

                if (paymentOption == null) {
                    continue;
                }
                PaymentOption valuesBefore = paymentOption.getBefore();
                PaymentOption valuesAfter = paymentOption.getAfter();

                log.info("PaymentOption ingestion called at {} with payment position id {}",
                        LocalDateTime.now(), (valuesAfter != null ? valuesAfter : valuesBefore).getId());

                boolean response = paymentOptionProducer.sendIngestedPaymentOption(paymentOption);

                if (response) {
                    log.info("PaymentOption ingestion sent to eventhub at {}",
                            LocalDateTime.now());
                } else {
                    log.error("PaymentOption ingestion unable to send to eventhub at {}",
                            LocalDateTime.now());
                }
            }
        } catch (JsonProcessingException e) {
            log.error("PaymentOption ingestion error JsonProcessingException at {} : {}",
                    LocalDateTime.now(), e.getMessage());
        } catch (Exception e) {
            log.error("PaymentOption ingestion error Generic exception at {} : {}",
                    LocalDateTime.now(), e.getMessage());
        }

    }

    public void ingestTransfers(List<String> messages) {
        log.info("Transfer ingestion called at {} for payment positions with events list size {}",
                LocalDateTime.now(), messages.size());

        // persist the item
        try {
            for (String msg : messages) {
                DataCaptureMessage<Transfer> transfer = objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true).readValue(msg, new TypeReference<DataCaptureMessage<Transfer>>() {});

                if (transfer == null) {
                    continue;
                }
                Transfer valuesBefore = transfer.getBefore();
                Transfer valuesAfter = transfer.getAfter();

                log.info("Transfer ingestion called at {} with payment position id {}",
                        LocalDateTime.now(), (valuesAfter != null ? valuesAfter : valuesBefore).getId());

                boolean response = transferProducer.sendIngestedTransfer(transfer);

                if (response) {
                    log.info("Transfer ingestion sent to eventhub at {}",
                            LocalDateTime.now());
                } else {
                    log.error("Transfer ingestion unable to send to eventhub at {}",
                            LocalDateTime.now());
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Transfer ingestion error JsonProcessingException at {} : {}",
                    LocalDateTime.now(), e.getMessage());
        } catch (Exception e) {
            log.error("Transfer ingestion error Generic exception at {} : {}",
                    LocalDateTime.now(), e.getMessage());
        }

    }
}
