package it.gov.pagopa.gpd.ingestion.manager.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

class ObjectMapperUtilsTest {

    @Test
    void returnNullAfterException() {

        Assertions.assertNull(ObjectMapperUtils.writeValueAsString(InputStream.nullInputStream()));
        Assertions.assertThrows(JsonProcessingException.class, () -> ObjectMapperUtils.mapString("", InputStream.class));

    }
}
