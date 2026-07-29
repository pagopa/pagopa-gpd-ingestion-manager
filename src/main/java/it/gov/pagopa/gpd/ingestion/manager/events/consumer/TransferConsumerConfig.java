package it.gov.pagopa.gpd.ingestion.manager.events.consumer;

import it.gov.pagopa.gpd.ingestion.manager.model.enumeration.EntityType;
import it.gov.pagopa.gpd.ingestion.manager.service.DeadLetterService;
import it.gov.pagopa.gpd.ingestion.manager.service.IngestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.function.Consumer;

@Configuration
@Slf4j
public class TransferConsumerConfig {

    @Bean
    public Consumer<List<String>> ingestTransfer(IngestionService ingestionService, DeadLetterService deadLetterService) {
        return messages -> {
            for (String msg : messages) {
                try {
                    ingestionService.ingestTransfer(msg);
                } catch (Exception e) {
                    deadLetterService.sendToDeadLetter(msg, EntityType.TRANSFER, e);
                }
            }
        };
    }
}