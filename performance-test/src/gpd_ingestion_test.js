

const { insertPaymentPositionWithValidFiscalCode, insertPaymentPositionWithInvalidFiscalCode, deletePaymentPositions } = require("./modules/pg_gpd_client.js");
const { REDIS_ARRAY_IDS_TOKENIZED, REDIS_ARRAY_IDS_NOT_TOKENIZED } = require("./modules/common.js");
const { setValueRedis } = require("./modules/redis_client.js");

const NUMBER_OF_EVENTS = JSON.parse(process.env.NUMBER_OF_EVENTS);

function insertEvents() {
    const arrayIdTokenized = [];
    const arrayIdNotTokenized = [];

    console.log("Selected number of events: ", NUMBER_OF_EVENTS);
    // SAVE ON DB paymentPositions
    for (let i = 0; i < NUMBER_OF_EVENTS; i++) {
        const idValidFiscalCode = "PERFORMANCE_GPD_INGESTION_VALID_FISCAL_CODE_" + new Date().getTime();
        insertPaymentPositionWithValidFiscalCode(idValidFiscalCode);
        arrayIdTokenized.push(idValidFiscalCode);

        const idInvalidFiscalCode = "PERFORMANCE_GPD_INGESTION_INVALID_FISCAL_CODE_" + new Date().getTime();
        insertPaymentPositionWithInvalidFiscalCode(idInvalidFiscalCode);
        arrayIdNotTokenized.push(idInvalidFiscalCode);
    }
    console.log("Inserted in database paymentOptions with valid fiscal code with ids: ", JSON.stringify(arrayIdTokenized));
    console.log("Inserted in database paymentOptions with invalid fiscal code with ids: ", JSON.stringify(arrayIdNotTokenized));


    // SAVE ID ARRAYS ON REDIS
    setValueRedis(REDIS_ARRAY_IDS_TOKENIZED, arrayIdTokenized);
    setValueRedis(REDIS_ARRAY_IDS_NOT_TOKENIZED, arrayIdNotTokenized);

    // DELETE paymentPositions
    deletePaymentPositions();
}

insertEvents();