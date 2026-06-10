package it.gov.pagopa.gpd.ingestion.manager.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AnonymizerModel {

    String text;
}