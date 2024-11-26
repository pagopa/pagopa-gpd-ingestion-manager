package it.gov.pagopa.gpd.ingestion.manager.events.producer.impl;

import it.gov.pagopa.gpd.ingestion.manager.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.PaymentOption;
import it.gov.pagopa.gpd.ingestion.manager.events.producer.IngestedPaymentOptionProducer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

@Service
@Slf4j
public class IngestedPaymentOptionProducerImpl implements IngestedPaymentOptionProducer {


    private final StreamBridge streamBridge;

    @Autowired
    public IngestedPaymentOptionProducerImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }


    private static Message<DataCaptureMessage<PaymentOption>> buildMessage(
            DataCaptureMessage<PaymentOption> ingestedPaymentOption) {
        return MessageBuilder.withPayload(ingestedPaymentOption).build();
    }

    @Override
    public boolean sendIngestedPaymentOption(DataCaptureMessage<PaymentOption> ingestedPaymentOption) {
        var res = streamBridge.send("ingestPaymentOption-out-0",
                buildMessage(ingestedPaymentOption));


        MDC.put("topic", "payment option");
        MDC.put("action", "sent");
        log.info("Payment Option Retry Sent");
        MDC.remove("topic");
        MDC.remove("action");

        return res;
    }

    /**
     * Declared just to let know Spring to connect the producer at startup
     */
    @Slf4j
    @Configuration
    static class IngestedPaymentOptionProducerConfig {

        @Bean
        public Supplier<Flux<Message<DataCaptureMessage<PaymentOption>>>> sendIngestedPaymentOption() {
            return Flux::empty;
        }

    }

}