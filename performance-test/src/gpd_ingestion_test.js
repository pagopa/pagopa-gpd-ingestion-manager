

import { insertPaymentPositionWithValidFiscalCode, insertPaymentPositionWithInvalidFiscalCode, deletePaymentPositions } from "./modules/pg_gpd_client.js";
import { REDIS_ARRAY_IDS_TOKENIZED, REDIS_ARRAY_IDS_NOT_TOKENIZED } from "./modules/common.js";
import { setValueRedis } from "./modules/redis_client.js";

const NUMBER_OF_EVENTS = JSON.parse(open(__ENV.NUMBER_OF_EVENTS));

export function setup() {
    const arrayIdTokenized = [];
    const arrayIdNotTokenized = [];

    return { arrayIdTokenized, arrayIdNotTokenized };
}

export default function (arrayIds) {
    console.log("ENVVV", NUMBER_OF_EVENTS);
    // SAVE ON DB paymentPositions
    for (let i = 0; i < NUMBER_OF_EVENTS; i++) {
        const idValidFiscalCode = "PERFORMANCE_GPD_INGESTION_VALID_FISCAL_CODE" + new Date().getTime();
        insertPaymentPositionWithValidFiscalCode(idValidFiscalCode);
        arrayIds.arrayIdTokenized.push(idValidFiscalCode);
        console.log("Inserted in database paymentOptions with valid fiscal code with ids: ", JSON.stringify(arrayIds.arrayIdTokenized));

        const idInvalidFiscalCode = "PERFORMANCE_GPD_INGESTION_INVALID_FISCAL_CODE" + new Date().getTime();
        insertPaymentPositionWithInvalidFiscalCode(idInvalidFiscalCode);
        arrayIds.arrayIdNotTokenized.push(idInvalidFiscalCode);
        console.log("Inserted in database paymentOptions with invalid fiscal code with ids: ", JSON.stringify(arrayIds.arrayIdNotTokenized));
    }

    // SAVE ID ARRAYS ON REDIS
    setValueRedis(REDIS_ARRAY_IDS_TOKENIZED, arrayIds.arrayIdTokenized);
    setValueRedis(REDIS_ARRAY_IDS_NOT_TOKENIZED, arrayIds.arrayIdNotTokenized);
}

export function teardown() {
    // DELETE paymentPositions
    deletePaymentPositions();
}