package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gpd.ingestion.manager.client.AnonymizerClient;
import it.gov.pagopa.gpd.ingestion.manager.exception.AnonymizerException;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
import it.gov.pagopa.gpd.ingestion.manager.model.AnonymizerModel;
import it.gov.pagopa.gpd.ingestion.manager.model.tokenizer.ErrorMessage;
import it.gov.pagopa.gpd.ingestion.manager.model.tokenizer.ErrorResponse;
import it.gov.pagopa.gpd.ingestion.manager.model.tokenizer.TokenResource;
import it.gov.pagopa.gpd.ingestion.manager.service.AnonymizerService;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.http.HttpResponse;

/**
 * {@inheritDoc}
 */
@Service
public class AnonymizerServiceImpl implements AnonymizerService {

    private final Logger logger = LoggerFactory.getLogger(AnonymizerServiceImpl.class);

    private final AnonymizerClient anonymizerClient;

    private final ObjectMapper objectMapper;

    @Autowired
    AnonymizerServiceImpl(AnonymizerClient anonymizerClient, ObjectMapper objectMapper) {
        this.anonymizerClient = anonymizerClient;
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String anonymize(String body) throws AnonymizerException, JsonProcessingException {
        logger.debug("Anonymizer anonymize called");
        AnonymizerModel anonymizerModel = AnonymizerModel.builder().text(body).build();
        String anonymizerBody = objectMapper.writeValueAsString(anonymizerModel);

        HttpResponse<String> httpResponse = anonymizerClient.anonymize(anonymizerBody);

        if (httpResponse.statusCode() == HttpStatus.SC_BAD_REQUEST
                || httpResponse.statusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            ErrorResponse response = objectMapper.readValue(httpResponse.body(), ErrorResponse.class);
            String errMsg = String.format("Anonymizer anonymize invocation failed with status %s and message: %s. Error description: %s (%s)",
                    response.getStatus(), response.getTitle(), response.getDetail(), response.getType());
            throw new AnonymizerException(errMsg, response.getStatus());
        }
        if (httpResponse.statusCode() != HttpStatus.SC_OK) {
            ErrorMessage response = objectMapper.readValue(httpResponse.body(), ErrorMessage.class);
            String errMsg = String.format("Anonymizer anonymize invocation failed with status %s and message: %s.",
                    httpResponse.statusCode(), response.getMessage());
            throw new AnonymizerException(errMsg, httpResponse.statusCode());
        }
        AnonymizerModel anonymizerModelResponse = objectMapper.readValue(httpResponse.body(), AnonymizerModel.class);
        logger.debug("Anonymizer anonymize invocation completed");
        return anonymizerModelResponse.getText();
    }
}
