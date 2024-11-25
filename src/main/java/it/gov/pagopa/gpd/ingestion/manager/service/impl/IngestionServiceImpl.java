package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import it.gov.pagopa.gpd.ingestion.manager.service.PDVTokenizerService;
import it.gov.pagopa.gpd.ingestion.manager.utils.ObjectMapperUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static it.gov.pagopa.gpd.ingestion.manager.utils.ValidationUtils.isValidFiscalCode;

@Service
@Slf4j
public class IngestionServiceImpl implements IngestionService {

    private final PDVTokenizerService pdvTokenizerService;
    private final IngestedPaymentPositionProducer paymentPositionProducer;
    private final IngestedPaymentOptionProducer paymentOptionProducer;

    private final IngestedTransferProducer transferProducer;

    @Autowired
    public IngestionServiceImpl(PDVTokenizerService pdvTokenizerService, IngestedPaymentPositionProducer paymentPositionProducer, IngestedPaymentOptionProducer paymentOptionProducer, IngestedTransferProducer transferProducer) {
        this.pdvTokenizerService = pdvTokenizerService;
        this.paymentPositionProducer = paymentPositionProducer;
        this.paymentOptionProducer = paymentOptionProducer;
        this.transferProducer = transferProducer;
    }

    public void ingestPaymentPositions(List<String> messages) {
        log.info("PaymentPosition ingestion called at {} for payment positions with events list size {}",
                LocalDateTime.now(), messages.size());

        // persist the item
        try {
            for (String msg : messages) {
                DataCaptureMessage<PaymentPosition> pp = ObjectMapperUtils.mapDataCapturePaymentPositionString(msg);

                if (pp == null) {
                    continue;
                }
                PaymentPosition valuesBefore = pp.getBefore();
                PaymentPosition valuesAfter = pp.getAfter();

                log.info("PaymentPosition ingestion called at {} with payment position id {}",
                        LocalDateTime.now(), (valuesAfter != null ? valuesAfter : valuesBefore).getId());

                // tokenize fiscal codes
                if (valuesBefore != null && isValidFiscalCode(valuesBefore.getFiscalCode())) {
                    valuesBefore.setFiscalCode(pdvTokenizerService.generateTokenForFiscalCode(valuesBefore.getFiscalCode()));
                    pp.setBefore(valuesBefore);
                }
                if (valuesAfter != null && isValidFiscalCode(valuesAfter.getFiscalCode())) {
                    valuesAfter.setFiscalCode(pdvTokenizerService.generateTokenForFiscalCode(valuesAfter.getFiscalCode()));
                    pp.setAfter(valuesAfter);
                }

                boolean response = paymentPositionProducer.sendIngestedPaymentPosition(pp);

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
                DataCaptureMessage<PaymentOption> pp = ObjectMapperUtils.mapDataCapturePaymentOptionString(msg);

                if (pp == null) {
                    continue;
                }
                PaymentOption valuesBefore = pp.getBefore();
                PaymentOption valuesAfter = pp.getAfter();

                log.info("PaymentOption ingestion called at {} with payment position id {}",
                        LocalDateTime.now(), (valuesAfter != null ? valuesAfter : valuesBefore).getId());

                boolean response = paymentOptionProducer.sendIngestedPaymentOption(pp);

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
                DataCaptureMessage<Transfer> pp = ObjectMapperUtils.mapDataCaptureTransferString(msg);

                if (pp == null) {
                    continue;
                }
                Transfer valuesBefore = pp.getBefore();
                Transfer valuesAfter = pp.getAfter();

                log.info("Transfer ingestion called at {} with payment position id {}",
                        LocalDateTime.now(), (valuesAfter != null ? valuesAfter : valuesBefore).getId());


                transferProducer.sendIngestedTransfer(pp);
                boolean response = transferProducer.sendIngestedTransfer(pp);

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
