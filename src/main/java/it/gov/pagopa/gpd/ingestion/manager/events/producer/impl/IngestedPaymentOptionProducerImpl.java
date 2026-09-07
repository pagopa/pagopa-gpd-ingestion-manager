package it.gov.pagopa.gpd.ingestion.manager.events.producer.impl;

import it.gov.pagopa.gpd.ingestion.manager.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.PaymentOption;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.before.PaymentOptionBefore;
import it.gov.pagopa.gpd.ingestion.manager.events.producer.IngestedPaymentOptionProducer;
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
public class IngestedPaymentOptionProducerImpl implements IngestedPaymentOptionProducer {

  private final StreamBridge streamBridge;

  @Autowired
  public IngestedPaymentOptionProducerImpl(StreamBridge streamBridge) {
    this.streamBridge = streamBridge;
  }

  private static Message<DataCaptureMessage<PaymentOption, PaymentOptionBefore>> buildMessage(
      DataCaptureMessage<PaymentOption, PaymentOptionBefore> ingestedPaymentOption) {
    return MessageBuilder.withPayload(ingestedPaymentOption).build();
  }

  @Override
  public void sendIngestedPaymentOption(
      DataCaptureMessage<PaymentOption, PaymentOptionBefore> ingestedPaymentOption) {
    var res = streamBridge.send("ingestPaymentOption-out-0", buildMessage(ingestedPaymentOption));

    if(!res){
      throw new AppException(AppError.MESSAGE_NOT_SENT);
    }
  }

  /** Declared just to let know Spring to connect the producer at startup */
  @Slf4j
  @Configuration
  static class IngestedPaymentOptionProducerConfig {

    @Bean
    public Supplier<Flux<Message<DataCaptureMessage<PaymentOption, PaymentOptionBefore>>>> sendIngestedPaymentOption() {
      return Flux::empty;
    }
  }
}
