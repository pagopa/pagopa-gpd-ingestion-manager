package it.gov.pagopa.gpd.ingestion.manager.exception;

import lombok.Getter;

/**
 * Thrown in case an error occur when invoking Anonymizer service
 */
@Getter
public class AnonymizerException extends Exception {

    private final int statusCode;

    /**
     * Constructs new exception with provided message
     *
     * @param message    Detail message
     * @param statusCode status code
     */
    public AnonymizerException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Constructs new exception with provided message
     *
     * @param message    Detail message
     * @param statusCode status code
     * @param cause      Exception causing the constructed one
     */
    public AnonymizerException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
}
