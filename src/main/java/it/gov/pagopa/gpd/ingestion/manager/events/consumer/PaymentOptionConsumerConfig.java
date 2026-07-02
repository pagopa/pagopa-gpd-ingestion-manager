package it.gov.pagopa.gpd.ingestion.manager.events.consumer;

import it.gov.pagopa.gpd.ingestion.manager.service.DeadLetterService;
import it.gov.pagopa.gpd.ingestion.manager.service.IngestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class PaymentOptionConsumerConfig {

    @Bean
    public Consumer<Message<String>> ingestPaymentOption(IngestionService ingestionService) {
        return ingestionService::ingestPaymentOption;
    }

    @Bean
    public Consumer<ErrorMessage> deadLetterPaymentOptionErrorHandler(DeadLetterService deadLetterService) {
        return deadLetterService::sendToDeadLetter;
    }
}