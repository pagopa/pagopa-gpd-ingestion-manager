package it.gov.pagopa.gpd.ingestion.manager.util;

import java.time.Clock;
import java.time.LocalDateTime;

public class Utility {
    public static LocalDateTime getDateNow() {
        return LocalDateTime.now(Clock.systemDefaultZone());
    }
}
