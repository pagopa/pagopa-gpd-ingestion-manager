package it.gov.pagopa.gpd.ingestion.manager.events.producer.impl;

import it.gov.pagopa.gpd.ingestion.manager.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.Transfer;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.before.TransferBefore;
import it.gov.pagopa.gpd.ingestion.manager.events.producer.IngestedTransferProducer;
import java.util.function.Supplier;

import it.gov.pagopa.gpd.ingestion.manager.exception.AppError;
import it.gov.pagopa.gpd.ingestion.manager.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class IngestedTransferProducerImpl implements IngestedTransferProducer {

  private final StreamBridge streamBridge;

  @Autowired
  public IngestedTransferProducerImpl(StreamBridge streamBridge) {
    this.streamBridge = streamBridge;
  }

  private static Message<DataCaptureMessage<Transfer, TransferBefore>> buildMessage(
      DataCaptureMessage<Transfer, TransferBefore> ingestedTransfer) {
    return MessageBuilder.withPayload(ingestedTransfer).build();
  }

  @Override
  public void sendIngestedTransfer(DataCaptureMessage<Transfer, TransferBefore> ingestedTransfer) {
    var res = streamBridge.send("ingestTransfer-out-0", buildMessage(ingestedTransfer));

    if(!res){
      throw new AppException(AppError.MESSAGE_NOT_SENT);
    }
  }

  /** Declared just to let know Spring to connect the producer at startup */
  @Slf4j
  @Configuration
  static class IngestedTransferProducerConfig {

    @Bean
    public Supplier<Flux<Message<DataCaptureMessage<Transfer, TransferBefore>>>> sendIngestedTransfer() {
      return Flux::empty;
    }
  }
}
