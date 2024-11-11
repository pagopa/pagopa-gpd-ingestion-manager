package it.gov.pagopa.gpd.ingestion.manager.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gpd.ingestion.manager.entity.PaymentOption;
import it.gov.pagopa.gpd.ingestion.manager.entity.PaymentPosition;
import it.gov.pagopa.gpd.ingestion.manager.entity.Transfer;
import it.gov.pagopa.gpd.ingestion.manager.model.DataCaptureMessage;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.util.List;

public class ObjectMapperUtils {

    private static final ModelMapper modelMapper;
    private static final ObjectMapper objectMapper;

    /**
     * Model mapper property setting are specified in the following block.
     * Default property matching strategy is set to Strict see {@link MatchingStrategies}
     * Custom mappings are added using {@link ModelMapper#addMappings(PropertyMap)}
     */
    static {
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        objectMapper = new ObjectMapper();
    }

    /**
     * Hide from public usage.
     */
    private ObjectMapperUtils() {
    }

    /**
     * Encodes an object to a string
     *
     * @param value Object to be encoded
     * @return encoded string
     */
    public static String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Maps string to object of defined Class
     *
     * @param string   String to map
     * @param outClass Class to be mapped to
     * @param <T>      Defined Class
     * @return object of the defined Class
     */
    public static <T> T mapString(final String string, Class<T> outClass) throws JsonProcessingException {
        return objectMapper.readValue(string, outClass);
    }

    /**
     * Maps string to object of DataCaptureMessage paymentPosition list
     *
     * @param string   String to map
     * @param outClass Class to be mapped to
     * @return object of the defined Class
     */
    public static List<DataCaptureMessage<PaymentPosition>> mapDataCapturePaymentPositionListString(final String string, TypeReference<List<DataCaptureMessage<PaymentPosition>>> outClass) throws JsonProcessingException {
        return objectMapper
                .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .readValue(string, outClass);
    }

    /**
     * Maps string to object of DataCaptureMessage paymentOption list
     *
     * @param string   String to map
     * @param outClass Class to be mapped to
     * @return object of the defined Class
     */
    public static List<DataCaptureMessage<PaymentOption>> mapDataCapturePaymentOptionListString(final String string, TypeReference<List<DataCaptureMessage<PaymentOption>>> outClass) throws JsonProcessingException {
        return objectMapper
                .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .readValue(string, outClass);
    }

    /**
     * Maps string to object of DataCaptureMessage transfer list
     *
     * @param string   String to map
     * @param outClass Class to be mapped to
     * @return object of the defined Class
     */
    public static List<DataCaptureMessage<Transfer>> mapDataCaptureTransferListString(final String string, TypeReference<List<DataCaptureMessage<Transfer>>> outClass) throws JsonProcessingException {
        return objectMapper
                .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .readValue(string, outClass);
    }


}
