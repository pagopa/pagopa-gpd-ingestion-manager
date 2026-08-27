package it.gov.pagopa.gpd.ingestion.manager.events.model.entity.before;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentPositionBefore {

    private long id;
}
