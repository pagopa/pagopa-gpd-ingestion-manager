package it.gov.pagopa.gpd.ingestion.manager.events.consumer;

import it.gov.pagopa.gpd.ingestion.manager.service.DeadLetterService;
import it.gov.pagopa.gpd.ingestion.manager.service.IngestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

import java.util.List;
import java.util.function.Consumer;

@Configuration
@Slf4j
public class TransferConsumerConfig {

    @Bean
    public Consumer<List<Message<String>>> ingestTransfer(IngestionService ingestionService, DeadLetterService deadLetterService) {
        return messages -> {
            for (Message<String> msg : messages) {
                try {
                    ingestionService.ingestTransfer(msg);
                } catch (Exception e) {
                    ErrorMessage error = new ErrorMessage(e, msg.getHeaders(), msg);
                    deadLetterService.sendToDeadLetter(error);
                }
            }
        };
    }
}