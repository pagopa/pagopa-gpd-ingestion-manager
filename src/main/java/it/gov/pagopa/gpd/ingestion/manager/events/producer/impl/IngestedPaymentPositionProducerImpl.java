package it.gov.pagopa.gpd.ingestion.manager.events.producer.impl;

import it.gov.pagopa.gpd.ingestion.manager.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.PaymentPosition;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.before.PaymentPositionBefore;
import it.gov.pagopa.gpd.ingestion.manager.events.producer.IngestedPaymentPositionProducer;
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
public class IngestedPaymentPositionProducerImpl implements IngestedPaymentPositionProducer {

  private final StreamBridge streamBridge;

  @Autowired
  public IngestedPaymentPositionProducerImpl(StreamBridge streamBridge) {
    this.streamBridge = streamBridge;
  }

  private static Message<DataCaptureMessage<PaymentPosition, PaymentPositionBefore>> buildMessage(
      DataCaptureMessage<PaymentPosition, PaymentPositionBefore> ingestedPaymentPosition) {
    return MessageBuilder.withPayload(ingestedPaymentPosition).build();
  }

  @Override
  public void sendIngestedPaymentPosition(
      DataCaptureMessage<PaymentPosition, PaymentPositionBefore> ingestedPaymentPosition) {
    var res =
        streamBridge.send("ingestPaymentPosition-out-0", buildMessage(ingestedPaymentPosition));

    if(!res){
      throw new AppException(AppError.MESSAGE_NOT_SENT);
    }
  }

  /** Declared just to let know Spring to connect the producer at startup */
  @Slf4j
  @Configuration
  static class IngestedPaymentPositionProducerConfig {

    @Bean
    public Supplier<Flux<Message<DataCaptureMessage<PaymentPosition, PaymentPositionBefore>>>>
        sendIngestedPaymentPosition() {
      return Flux::empty;
    }
  }
}
