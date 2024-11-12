package it.gov.pagopa.gpd.ingestion.manager.entity.enumeration;

import com.google.api.client.util.Value;

public enum TransferStatus {
    @Value("T_REPORTED")
    T_REPORTED,
    @Value("T_UNREPORTED")
    T_UNREPORTED
}
