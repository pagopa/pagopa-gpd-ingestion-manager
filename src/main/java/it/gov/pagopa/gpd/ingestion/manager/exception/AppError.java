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
      "Payment option message is not a processable JSON"),
  MESSAGE_NOT_SENT(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "Message not sent",
      "The message has not been sent to eventhub"),
  ERROR_TOKENIZING_FISCAL_CODE(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Fail to tokenize fiscal code",
          "The PDVTokenizer couldn't tokenize the fiscal code"),
  ERROR_ANONYMIZING_REMITTANCE_INFORMATION(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Fail to anonymize remittance information",
          "The Anonymizer couldn't anonymize the remittance information"),
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
