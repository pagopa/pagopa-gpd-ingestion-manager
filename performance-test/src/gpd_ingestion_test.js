

import { insertPaymentPositionWithValidFiscalCode, insertPaymentPositionWithInvalidFiscalCode, deletePaymentPositions } from "./modules/pg_gpd_client.js";
import { REDIS_ARRAY_IDS_TOKENIZED, REDIS_ARRAY_IDS_NOT_TOKENIZED } from "./modules/common.js";
import { setValueRedis } from "./modules/redis_client.js";

const NUMBER_OF_EVENTS = JSON.parse(open(__ENV.NUMBER_OF_EVENTS));

export default function insertEvents() {
    const arrayIdTokenized = [];
    const arrayIdNotTokenized = [];

    console.log("Selected number of events: ", NUMBER_OF_EVENTS);
    // SAVE ON DB paymentPositions
    for (let i = 0; i < NUMBER_OF_EVENTS; i++) {
        const idValidFiscalCode = "PERFORMANCE_GPD_INGESTION_VALID_FISCAL_CODE" + new Date().getTime();
        insertPaymentPositionWithValidFiscalCode(idValidFiscalCode);
        arrayIdTokenized.push(idValidFiscalCode);
        console.log("Inserted in database paymentOptions with valid fiscal code with ids: ", JSON.stringify(arrayIdTokenized));

        const idInvalidFiscalCode = "PERFORMANCE_GPD_INGESTION_INVALID_FISCAL_CODE" + new Date().getTime();
        insertPaymentPositionWithInvalidFiscalCode(idInvalidFiscalCode);
        arrayIdNotTokenized.push(idInvalidFiscalCode);
        console.log("Inserted in database paymentOptions with invalid fiscal code with ids: ", JSON.stringify(arrayIdNotTokenized));
    }

    // SAVE ID ARRAYS ON REDIS
    setValueRedis(REDIS_ARRAY_IDS_TOKENIZED, arrayIdTokenized);
    setValueRedis(REDIS_ARRAY_IDS_NOT_TOKENIZED, arrayIdNotTokenized);

    // DELETE paymentPositions
    deletePaymentPositions();
}

insertEvents();