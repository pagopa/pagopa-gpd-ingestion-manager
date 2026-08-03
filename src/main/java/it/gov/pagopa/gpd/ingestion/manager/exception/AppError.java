package it.gov.pagopa.gpd.ingestion.manager.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AppError {
  INTERNAL_SERVER_ERROR(
      HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Something was wrong"),
  NULL_MESSAGE(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "Null Message",
      "The message is null, unable to process messages"),
  JSON_NOT_PROCESSABLE(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "JSON not processable",
      "Message is not a processable JSON"),
  DEAD_LETTER_NOT_PROCESSABLE(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Dead letter message not processable",
          "The message has not a processable entity type"),
  MESSAGE_NOT_SENT(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "Message not sent",
      "The message has not been sent to eventhub"),
  ERROR_TOKENIZING_FISCAL_CODE(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Fail to tokenize fiscal code",
          "The PDVTokenizer couldn't tokenize the fiscal code"),
  ERROR_ANONYMIZING_TRANSFER_REMITTANCE_INFORMATION(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Fail to anonymize transfer remittance information",
          "The Anonymizer couldn't anonymize the transfer remittance information"),
  ERROR_ANONYMIZING_PAYMENT_OPTION_DESCRIPTION(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Fail to anonymize payment option description",
          "The Anonymizer couldn't anonymize the payment option description"),
  UNKNOWN(null, null, null);

  public final HttpStatus httpStatus;
  public final String title;
  public final String details;

  AppError(HttpStatus httpStatus, String title, String details) {
    this.httpStatus = httpStatus;
    this.title = title;
    this.details = details;
  }
}
