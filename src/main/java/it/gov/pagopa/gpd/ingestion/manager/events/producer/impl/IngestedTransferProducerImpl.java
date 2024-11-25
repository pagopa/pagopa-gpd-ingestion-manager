package it.gov.pagopa.gpd.ingestion.manager.events.producer.impl;

import it.gov.pagopa.gpd.ingestion.manager.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.Transfer;
import it.gov.pagopa.gpd.ingestion.manager.events.producer.IngestedTransferProducer;
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
public class IngestedTransferProducerImpl implements IngestedTransferProducer {


    private final StreamBridge streamBridge;

    @Autowired
    public IngestedTransferProducerImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }


    private static Message<DataCaptureMessage<Transfer>> buildMessage(
            DataCaptureMessage<Transfer> ingestedTransfer) {
        return MessageBuilder.withPayload(ingestedTransfer).build();
    }

    @Override
    public boolean sendIngestedTransfer(DataCaptureMessage<Transfer> ingestedTransfer) {
        var res = streamBridge.send("ingestTransfer-out-0",
                buildMessage(ingestedTransfer));


        MDC.put("topic", "transfer");
        MDC.put("action", "sent");
        log.info("Transfer Retry Sent");
        MDC.remove("topic");
        MDC.remove("action");

        return res;
    }

    /**
     * Declared just to let know Spring to connect the producer at startup
     */
    @Slf4j
    @Configuration
    static class IngestedTransferProducerConfig {

        @Bean
        public Supplier<Flux<Message<DataCaptureMessage<Transfer>>>> sendIngestedTransfer() {
            return Flux::empty;
        }

    }

}