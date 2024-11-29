

const { insertPaymentPositionWithValidFiscalCode, insertPaymentPositionWithInvalidFiscalCode, deletePaymentPositions } = require("../modules/pg_gpd_client.js");
const { REDIS_ARRAY_IDS_TOKENIZED, REDIS_ARRAY_IDS_NOT_TOKENIZED } = require("../modules/common.js");
const { setValueRedis, shutDownClient } = require("../modules/redis_client.js");

const NUMBER_OF_EVENTS = JSON.parse(process.env.NUMBER_OF_EVENTS);

async function insertEvents() {
    // Clean up paymentPositions
    await deletePaymentPositions();

    const arrayIdTokenized = [];
    const arrayIdNotTokenized = [];

    console.log("Selected number of events: ", NUMBER_OF_EVENTS);
    // SAVE ON DB paymentPositions
    for (let i = 0; i < (Math.floor(NUMBER_OF_EVENTS / 2)); i++) {
        const uniqueId = 120798 + i;
        const idValidFiscalCode = uniqueId;
        await insertPaymentPositionWithValidFiscalCode(idValidFiscalCode);
        arrayIdTokenized.push(idValidFiscalCode);

        const idInvalidFiscalCode = uniqueId + (NUMBER_OF_EVENTS * 2);
        await insertPaymentPositionWithInvalidFiscalCode(idInvalidFiscalCode);
        arrayIdNotTokenized.push(idInvalidFiscalCode);
    }
    console.log(`Inserted ${arrayIdTokenized.length} elements in database paymentPositions with valid fiscal code with ids: `, JSON.stringify(arrayIdTokenized));
    console.log(`Inserted ${arrayIdNotTokenized.length} elements in database paymentPositions with invalid fiscal code with ids: `, JSON.stringify(arrayIdNotTokenized));


    // SAVE ID ARRAYS ON REDIS
    await setValueRedis({ key: REDIS_ARRAY_IDS_TOKENIZED, value: JSON.stringify(arrayIdTokenized) });
    await setValueRedis({ key: REDIS_ARRAY_IDS_NOT_TOKENIZED, value: JSON.stringify(arrayIdNotTokenized) });

    // DELETE paymentPositions
    await deletePaymentPositions();
    console.log("Deleted payment positions");

    await shutDownClient();

    return null;
}

insertEvents().then(() => {
    console.log("Insert script ended");
    process.exit();
});