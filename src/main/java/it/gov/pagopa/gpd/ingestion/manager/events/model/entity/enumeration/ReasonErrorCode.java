package it.gov.pagopa.gpd.ingestion.manager.events.model.entity.enumeration;

public enum ReasonErrorCode {

    ERROR_PDV_IO(800),
    ERROR_PDV_UNEXPECTED(801),
    ERROR_ANONYMIZER_IO(802),
    ERROR_ANONYMIZER_UNEXPECTED(803)
    ;

    private final int code;

    ReasonErrorCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}
