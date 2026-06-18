package it.gov.pagopa.gpd.ingestion.manager.util;

import org.slf4j.MDC;

import java.util.UUID;

public class MDCUtils {

    private static final String MDC_KEY_REQUEST_ID = "requestId";
    private static final String MDC_KEY_ENTITY = "entity";
    private static final String MDC_KEY_ID = "id";
    private static final String MDC_KEY_SEND_RESULT = "sendResult";
    private static final String MDC_KEY_ERROR_TYPE = "errorType";
    private static final String MDC_KEY_ERROR_MESSAGE = "errorMessage";

    public static void initMDC(String entityName) {
        MDC.put(MDC_KEY_REQUEST_ID, String.valueOf(UUID.randomUUID()));
        MDC.put(MDC_KEY_ENTITY, entityName);
    }

    public static void setMDCId(String id){
        MDC.put(MDC_KEY_ID, id);
    }

    public static void setMDCSendResult(String sendResult){
        MDC.put(MDC_KEY_SEND_RESULT, sendResult);
    }

    public static void setMDCError(String errorType, String errorMessage){
        MDC.put(MDC_KEY_ERROR_TYPE, errorType);
        MDC.put(MDC_KEY_ERROR_MESSAGE, errorMessage);
    }

    public static void clearMDC(){
        MDC.remove(MDC_KEY_REQUEST_ID);
        MDC.remove(MDC_KEY_ENTITY);
        MDC.remove(MDC_KEY_ID);
        MDC.remove(MDC_KEY_SEND_RESULT);
        MDC.remove(MDC_KEY_ERROR_TYPE);
        MDC.remove(MDC_KEY_ERROR_MESSAGE);
    }
}
