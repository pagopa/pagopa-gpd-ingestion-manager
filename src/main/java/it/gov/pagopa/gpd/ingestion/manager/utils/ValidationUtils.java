package it.gov.pagopa.gpd.ingestion.manager.utils;

import java.util.regex.Pattern;

public class ValidationUtils {

    /**
     * Hide from public usage.
     */
    private ValidationUtils() {
    }

    public static boolean isValidFiscalCode(String fiscalCode) {
        if (fiscalCode != null && !fiscalCode.isEmpty()) {
            Pattern patternCF = Pattern.compile("^[A-Z]{6}[0-9LMNPQRSTUV]{2}[ABCDEHLMPRST][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z]$");
            Pattern patternPIVA = Pattern.compile("/^[0-9]{11}$/");

            return patternCF.matcher(fiscalCode).find() || patternPIVA.matcher(fiscalCode).find();
        }

        return false;
    }
}
