package it.gov.pagopa.gpd.ingestion.manager.events.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReasonError {
    private int code;
    private String message;

}
