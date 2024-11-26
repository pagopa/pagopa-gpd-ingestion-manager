package it.gov.pagopa.gpd.ingestion.manager.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gpd.ingestion.manager.client.PDVTokenizerClient;
import it.gov.pagopa.gpd.ingestion.manager.exception.PDVTokenizerException;
import it.gov.pagopa.gpd.ingestion.manager.model.tokenizer.ErrorMessage;
import it.gov.pagopa.gpd.ingestion.manager.model.tokenizer.ErrorResponse;
import it.gov.pagopa.gpd.ingestion.manager.model.tokenizer.PiiResource;
import it.gov.pagopa.gpd.ingestion.manager.model.tokenizer.TokenResource;
import it.gov.pagopa.gpd.ingestion.manager.service.PDVTokenizerService;
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
public class PDVTokenizerServiceImpl implements PDVTokenizerService {

    private final Logger logger = LoggerFactory.getLogger(PDVTokenizerServiceImpl.class);

    private final PDVTokenizerClient pdvTokenizerClient;

    private final ObjectMapper objectMapper;

    @Autowired
    PDVTokenizerServiceImpl(PDVTokenizerClient pdvTokenizerClient, ObjectMapper objectMapper) {
        this.pdvTokenizerClient = pdvTokenizerClient;
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateTokenForFiscalCode(String fiscalCode) throws PDVTokenizerException, JsonProcessingException {
        logger.debug("PDV Tokenizer generateTokenForFiscalCode called");
        PiiResource piiResource = PiiResource.builder().pii(fiscalCode).build();
        String tokenizerBody = objectMapper.writeValueAsString(piiResource);

        HttpResponse<String> httpResponse = pdvTokenizerClient.createToken(tokenizerBody);

        if (httpResponse.statusCode() == HttpStatus.SC_BAD_REQUEST
                || httpResponse.statusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            ErrorResponse response = objectMapper.readValue(httpResponse.body(), ErrorResponse.class);
            String errMsg = String.format("PDV Tokenizer generateTokenForFiscalCode invocation failed with status %s and message: %s. Error description: %s (%s)",
                    response.getStatus(), response.getTitle(), response.getDetail(), response.getType());
            throw new PDVTokenizerException(errMsg, response.getStatus());
        }
        if (httpResponse.statusCode() != HttpStatus.SC_OK) {
            ErrorMessage response = objectMapper.readValue(httpResponse.body(), ErrorMessage.class);
            String errMsg = String.format("PDV Tokenizer generateTokenForFiscalCode invocation failed with status %s and message: %s.",
                    httpResponse.statusCode(), response.getMessage());
            throw new PDVTokenizerException(errMsg, httpResponse.statusCode());
        }
        TokenResource tokenResource = objectMapper.readValue(httpResponse.body(), TokenResource.class);
        logger.debug("PDV Tokenizer generateTokenForFiscalCode invocation completed");
        return tokenResource.getToken();
    }
}
