package it.gov.pagopa.gpd.ingestion.manager.events.model.entity.enumeration;

import com.google.api.client.util.Value;

public enum PaymentPositionStatus {
    @Value("DRAFT")
    DRAFT,
    @Value("PUBLISHED")
    PUBLISHED,
    @Value("VALID")
    VALID,
    @Value("INVALID")
    INVALID,
    @Value("EXPIRED")
    EXPIRED,
    @Value("PAID")
    PAID,
    @Value("PARTIALLY_PAID")
    PARTIALLY_PAID,
    @Value("REPORTED")
    REPORTED
}
