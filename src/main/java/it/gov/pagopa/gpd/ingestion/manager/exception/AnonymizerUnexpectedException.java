package it.gov.pagopa.gpd.ingestion.manager.exception;

import lombok.Getter;

/**
 * Thrown in case an unexpected error occur when invoking Anonymizer service
 */
@Getter
public class AnonymizerUnexpectedException extends RuntimeException {

    /**
     * Constructs new exception with provided cause
     *
     * @param cause Exception causing the constructed one
     */
    public AnonymizerUnexpectedException(Throwable cause) {
        super(cause);
    }
}
