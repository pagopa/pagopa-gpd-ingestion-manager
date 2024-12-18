package it.gov.pagopa.gpd.ingestion.manager.events.producer.impl;

import it.gov.pagopa.gpd.ingestion.manager.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.ingestion.manager.events.model.entity.PaymentPosition;
import it.gov.pagopa.gpd.ingestion.manager.events.producer.IngestedPaymentPositionProducer;
import java.util.function.Supplier;
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

@Service
@Slf4j
public class IngestedPaymentPositionProducerImpl implements IngestedPaymentPositionProducer {

  private final StreamBridge streamBridge;

  @Autowired
  public IngestedPaymentPositionProducerImpl(StreamBridge streamBridge) {
    this.streamBridge = streamBridge;
  }

  private static Message<DataCaptureMessage<PaymentPosition>> buildMessage(
      DataCaptureMessage<PaymentPosition> ingestedPaymentPosition) {
    return MessageBuilder.withPayload(ingestedPaymentPosition).build();
  }

  @Override
  public boolean sendIngestedPaymentPosition(
      DataCaptureMessage<PaymentPosition> ingestedPaymentPosition) {
    var res =
        streamBridge.send("ingestPaymentPosition-out-0", buildMessage(ingestedPaymentPosition));

    MDC.put("topic", "payment position");
    MDC.put("action", "sent");
    log.debug("Payment Position Retry Sent");
    MDC.remove("topic");
    MDC.remove("action");

    return res;
  }

  /** Declared just to let know Spring to connect the producer at startup */
  @Slf4j
  @Configuration
  static class IngestedPaymentPositionProducerConfig {

    @Bean
    public Supplier<Flux<Message<DataCaptureMessage<PaymentPosition>>>>
        sendIngestedPaymentPosition() {
      return Flux::empty;
    }
  }
}
