const { REDIS_ARRAY_IDS_NOT_TOKENIZED, REDIS_ARRAY_IDS_TOKENIZED } = require("../modules/common.js");
const { readFromRedisWithKey, shutDownClient } = require("../modules/redis_client.js");

const REDIS_RAW_SUFFIX = "-raw-c";
const REDIS_ING_SUFFIX = "-ing-c";

// Performance Debezium connector
// 1. Retrieve messages from topic "raw"
// 2. Calculate difference between timestamps -> obj.ts_ms (time of insert on eventhub) : obj.source.ts_ms (time of insert on db)
// Performance gpd-ingestion-manager
// 1. Retrieve messages from topic "ingested"
// 2. Calculate difference between raw and ingested timestamps -> rawMsg.timestamp (timestamp of the message from topic raw) : ingestedMsg.timestamp (timestamp of the message from topic raw)
const reviewIngestionTimeToProcess = async () => {
    let arrayTimePsgToRaw = [];
    let totalTimePsgToRaw = 0;
    let minTimePsgToRaw = null;
    let maxTimePsgToRaw = null;
    let failedRaw = 0;

    let arrayTimeRawToTokenize = [];
    let totalTimeRawToTokenize = 0;
    let minTimeRawToTokenize = null;
    let maxTimeRawToTokenize = null;
    let failedTokenized = 0;

    let arrayTimeRawToIngest = [];
    let totalTimeRawToIngest = 0;
    let minTimeRawToIngest = null;
    let maxTimeRawToIngest = null;
    let failedIngested = 0;

    // RETRIEVE ARRAYS OF IDS
    const tokenizedIds = await readFromRedisWithKey(REDIS_ARRAY_IDS_TOKENIZED);
    const arrTokenizedParsed = JSON.parse(tokenizedIds);
    const notTokenizedIds = await readFromRedisWithKey(REDIS_ARRAY_IDS_NOT_TOKENIZED);
    const arrNotTokenizedParsed = JSON.parse(notTokenizedIds);

    for (const id of arrTokenizedParsed) {
        // RETRIEVE RAW MESSAGE FROM REDIS
        console.log("Retrieving from Redis message with id: " + id);
        const retrievedMsg = await readFromRedisWithKey(id + REDIS_RAW_SUFFIX);
        const rawMsg = JSON.parse(retrievedMsg);
        if (rawMsg) {
            const rawMsgValue = rawMsg.value;
            console.log("Processing raw message with id: " + id);

            // CALCULATE TIME TO CAPTURE
            let timePsgToRaw = rawMsgValue.ts_ms - rawMsgValue.source.ts_ms;
            arrayTimePsgToRaw.push(timePsgToRaw);
            totalTimePsgToRaw += timePsgToRaw;
            minTimePsgToRaw = minTimePsgToRaw === null || timePsgToRaw < minTimePsgToRaw ? timePsgToRaw : minTimePsgToRaw;
            maxTimePsgToRaw = maxTimePsgToRaw === null || timePsgToRaw > maxTimePsgToRaw ? timePsgToRaw : maxTimePsgToRaw;

            // RETRIEVE TOKENIZED MESSAGE FROM REDIS WITH RAW OBJ ID
            const tokenizedMsg = await readFromRedisWithKey(id + REDIS_ING_SUFFIX);

            if (tokenizedMsg) {
                const tokenizedMsgValue = JSON.parse(tokenizedMsg);
                console.log("Processing tokenized message with id: " + id);

                // CALCULATE TIME TO TOKENIZE
                let timeRawToTokenize = Number(tokenizedMsgValue.timestamp) - Number(rawMsg.timestamp);
                arrayTimeRawToTokenize.push(timeRawToTokenize);
                totalTimeRawToTokenize += timeRawToTokenize;
                minTimeRawToTokenize = minTimeRawToTokenize === null || timeRawToTokenize < minTimeRawToTokenize ? timeRawToTokenize : minTimeRawToTokenize;
                maxTimeRawToTokenize = maxTimeRawToTokenize === null || timeRawToTokenize > maxTimeRawToTokenize ? timeRawToTokenize : maxTimeRawToTokenize;
            } else {
                console.log("Fail to tokenize message with id: " + id);
                failedTokenized += 1;
            }
        } else {
            console.log("Fail to capture message with id: " + id);
            failedRaw += 1;
        }

    }

    for (const id of arrNotTokenizedParsed) {
        // RETRIEVE RAW MESSAGE FROM REDIS
        console.log("Retrieving from Redis message with id: " + id);
        const retrievedMsg = await readFromRedisWithKey(id + REDIS_RAW_SUFFIX);
        const rawMsg = JSON.parse(retrievedMsg);
        if (rawMsg) {
            const rawMsgValue = rawMsg.value;
            console.log("Processing raw message with id: " + id);

            // CALCULATE TIME TO CAPTURE
            let timePsgToRaw = rawMsgValue.ts_ms - rawMsgValue.source.ts_ms;
            arrayTimePsgToRaw.push(timePsgToRaw);
            totalTimePsgToRaw += timePsgToRaw;
            minTimePsgToRaw = minTimePsgToRaw === null || timePsgToRaw < minTimePsgToRaw ? timePsgToRaw : minTimePsgToRaw;
            maxTimePsgToRaw = maxTimePsgToRaw === null || timePsgToRaw > maxTimePsgToRaw ? timePsgToRaw : maxTimePsgToRaw;

            // RETRIEVE INGESTED MESSAGE FROM REDIS WITH RAW OBJ ID
            const ingestedMsg = await readFromRedisWithKey(id + REDIS_ING_SUFFIX);

            if (ingestedMsg) {
                const ingestedMsgValue = JSON.parse(ingestedMsg);
                console.log("Processing ingested message with id: " + id);

                // CALCULATE TIME TO INGEST WITHOUT TOKENIZER
                let timeRawToIngest = Number(ingestedMsgValue.timestamp) - Number(rawMsg.timestamp);
                arrayTimeRawToIngest.push(timeRawToIngest);
                totalTimeRawToIngest += timeRawToIngest;
                minTimeRawToIngest = minTimeRawToIngest === null || timeRawToIngest < minTimeRawToIngest ? timeRawToIngest : minTimeRawToIngest;
                maxTimeRawToIngest = maxTimeRawToIngest === null || timeRawToIngest > maxTimeRawToIngest ? timeRawToIngest : maxTimeRawToIngest;
            } else {
                console.log("Fail to ingest message with id: " + id);
                failedIngested += 1;
            }
        } else {
            console.log("Fail to capture message with id: " + id);
            failedRaw += 1;
        }
    }

    console.log("/////////////////////////////////");
    console.log("/----------- METRICS -----------/");
    console.log("/////////////////////////////////");
    console.log("--------------------------------");
    console.log(`total messages....................: ${arrTokenizedParsed.length + arrNotTokenizedParsed.length}`);
    console.log("--------------------------------");
    console.log(`mean time to capture..............: ${totalTimePsgToRaw ? getTimeString(Math.round(totalTimePsgToRaw / arrayTimePsgToRaw.length)) : "-"}`);
    console.log(`mean time to tokenize.............: ${totalTimeRawToTokenize ? getTimeString(Math.round(totalTimeRawToTokenize / arrayTimeRawToTokenize.length)) : "-"}`);
    console.log(`mean time to ingest...............: ${totalTimeRawToIngest ? getTimeString(Math.round(totalTimeRawToIngest / arrayTimeRawToIngest.length)) : "-"}`);
    console.log("--------------------------------");
    console.log(`min time to capture...............: ${minTimePsgToRaw ? getTimeString(minTimePsgToRaw) : "-"}`);
    console.log(`min time to tokenize..............: ${minTimeRawToTokenize ? getTimeString(minTimeRawToTokenize) : "-"}`);
    console.log(`min time to ingest................: ${minTimeRawToIngest ? getTimeString(minTimeRawToIngest) : "-"}`);
    console.log("--------------------------------");
    console.log(`max time to capture...............: ${maxTimePsgToRaw ? getTimeString(maxTimePsgToRaw) : "-"}`);
    console.log(`max time to tokenize..............: ${maxTimeRawToTokenize ? getTimeString(maxTimeRawToTokenize) : "-"}`);
    console.log(`max time to ingest................: ${maxTimeRawToIngest ? getTimeString(maxTimeRawToIngest) : "-"}`);
    console.log("--------------------------------");
    console.log(`failed to be captured.............: ${failedRaw}`);
    console.log(`failed to be tokenized............: ${failedTokenized}`);
    console.log(`failed to be ingested.............: ${failedIngested}`);
    console.log("/////////////////////////////////");
    console.log("/------------- END -------------/");
    console.log("/////////////////////////////////");

    await shutDownClient();

    return null;
}

function getTimeString(time) {
    return `${time}ms | ${time / 1000}s`;
}

reviewIngestionTimeToProcess().then(() => {
    process.exit();
});;