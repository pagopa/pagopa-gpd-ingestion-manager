package it.gov.pagopa.gpd.ingestion.manager.entity.enumeration;

import com.google.api.client.util.Value;

public enum PaymentOptionStatus {
    @Value("PO_UNPAID")
    PO_UNPAID,
    @Value("PO_PAID")
    PO_PAID,
    @Value("PO_PARTIALLY_REPORTED")
    PO_PARTIALLY_REPORTED,
    @Value("PO_REPORTED")
    PO_REPORTED
}
