package it.gov.pagopa.gpd.ingestion.manager.model;

import lombok.*;

/**
 * Model class used for request and response of the AnonymizerClient
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnonymizerModel {
    String text;
}