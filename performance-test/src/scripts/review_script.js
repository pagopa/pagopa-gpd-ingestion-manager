import { REDIS_ARRAY_IDS_NOT_TOKENIZED, REDIS_ARRAY_IDS_TOKENIZED } from "../modules/common.js";
import { readFromRedisWithKey } from "../modules/redis_client.js";

const REDIS_RAW_SUFFIX = "-raw-c";
const REDIS_ING_SUFFIX = "-ing-c";

// Performance Debezium connector
// 1. Retrieve messages from topic "raw"
// 2. Calculate difference between timestamps -> obj.source.tsMs (time of insert on db) : obj.tsMs (time of insert on eventhub)
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
    const tokenizedIds = readFromRedisWithKey(REDIS_ARRAY_IDS_TOKENIZED);
    const notTokenizedIds = readFromRedisWithKey(REDIS_ARRAY_IDS_NOT_TOKENIZED);

    for (const id of tokenizedIds) {
        // RETRIEVE RAW MESSAGE FROM REDIS
        const rawMsg = readFromRedisWithKey(id + REDIS_RAW_SUFFIX);

        if (rawMsg) {
            const rawMsgValue = JSON.parse(rawMsg.value.toString());
            console.log("Processing raw message with id: " + rawMsgValue.after.id);

            // CALCULATE TIME TO CAPTURE
            let timePsgToRaw = rawMsgValue.source.tsMs - rawMsgValue.tsMs;
            arrayTimePsgToRaw.push(timePsgToRaw);
            totalTimePsgToRaw += timePsgToRaw;
            minTimePsgToRaw = minTimePsgToRaw === null || timePsgToRaw < minTimePsgToRaw ? timePsgToRaw : minTimePsgToRaw;
            maxTimePsgToRaw = maxTimePsgToRaw === null || timePsgToRaw > maxTimePsgToRaw ? timePsgToRaw : maxTimePsgToRaw;

            // RETRIEVE TOKENIZED MESSAGE FROM REDIS WITH RAW OBJ ID
            const tokenizedMsg = readFromRedisWithKey(id + REDIS_ING_SUFFIX);

            if (tokenizedMsg) {
                const tokenizedMsgValue = JSON.parse(tokenizedMsg.value.toString());
                console.log("Processing tokenized message with id: " + tokenizedMsgValue.after.id);

                // CALCULATE TIME TO TOKENIZE
                let timeRawToTokenize = rawMsg.timestamp - tokenizedMsgValue.timestamp;
                arrayTimeRawToTokenize.push(timeRawToTokenize);
                totalTimeRawToTokenize += timeRawToTokenize;
                minTimeRawToTokenize = minTimeRawToTokenize === null || timeRawToTokenize < minTimeRawToTokenize ? timeRawToTokenize : minTimeRawToTokenize;
                maxTimeRawToTokenize = maxTimeRawToTokenize === null || timeRawToTokenize > maxTimeRawToTokenize ? timeRawToTokenize : maxTimeRawToTokenize;
            } else {
                failedTokenized += 1;
            }
        } else {
            failedRaw += 1;
        }

    }

    for (const id of notTokenizedIds) {
        // RETRIEVE RAW MESSAGE FROM REDIS
        const rawMsg = readFromRedisWithKey(id + REDIS_RAW_SUFFIX);

        if (rawMsg) {
            const rawMsgValue = JSON.parse(rawMsg.value.toString());
            console.log("Processing raw message with id: " + rawMsgValue.after.id);

            // CALCULATE TIME TO CAPTURE
            let timePsgToRaw = rawMsgValue.source.tsMs - rawMsgValue.tsMs;
            arrayTimePsgToRaw.push(timePsgToRaw);
            totalTimePsgToRaw += timePsgToRaw;
            minTimePsgToRaw = minTimePsgToRaw === null || timePsgToRaw < minTimePsgToRaw ? timePsgToRaw : minTimePsgToRaw;
            maxTimePsgToRaw = maxTimePsgToRaw === null || timePsgToRaw > maxTimePsgToRaw ? timePsgToRaw : maxTimePsgToRaw;

            // RETRIEVE INGESTED MESSAGE FROM REDIS WITH RAW OBJ ID
            const ingestedMsg = readFromRedisWithKey(id + REDIS_ING_SUFFIX);

            if (ingestedMsg) {
                const ingestedMsgValue = JSON.parse(ingestedMsg.value.toString());
                console.log("Processing ingested message with id: " + ingestedMsgValue.after.id);

                // CALCULATE TIME TO INGEST WITHOUT TOKENIZER
                let timeRawToIngest = rawMsg.timestamp - ingestedMsgValue.timestamp;
                arrayTimeRawToIngest.push(timeRawToIngest);
                totalTimeRawToIngest += timeRawToIngest;
                minTimeRawToIngest = minTimeRawToIngest === null || timeRawToIngest < minTimeRawToIngest ? timeRawToIngest : minTimeRawToIngest;
                maxTimeRawToIngest = maxTimeRawToIngest === null || timeRawToIngest > maxTimeRawToIngest ? timeRawToIngest : maxTimeRawToIngest;
            } else {
                failedIngested += 1;
            }
        } else {
            failedRaw += 1;
        }
    }

    console.log("/////////////////////////////////");
    console.log("/----------- METRICS -----------/");
    console.log("/////////////////////////////////");
    console.log("--------------------------------");
    console.log(`mean time psg to evh..............: ${totalTimePsgToRaw ? getTimeString(Math.round(totalTimePsgToRaw / arrayTimePsgToRaw.length)) : "-"}`);
    console.log(`mean time to tokenize.............: ${totalTimeRawToTokenize ? getTimeString(Math.round(totalTimeRawToTokenize / arrayTimeRawToTokenize.length)) : "-"}`);
    console.log(`mean time to ingest...............: ${totalTimeRawToIngest ? getTimeString(Math.round(totalTimeRawToIngest / arrayTimeRawToIngest.length)) : "-"}`);
    console.log("--------------------------------");
    console.log(`min time psg to evh...............: ${minTimePsgToRaw ? getTimeString(minTimePsgToRaw) : "-"}`);
    console.log(`min time to tokenize..............: ${minTimeRawToTokenize ? getTimeString(minTimeRawToTokenize) : "-"}`);
    console.log(`min time to ingest................: ${minTimeRawToIngest ? getTimeString(minTimeRawToIngest) : "-"}`);
    console.log("--------------------------------");
    console.log(`max time psg to evh...............: ${maxTimePsgToRaw ? getTimeString(maxTimePsgToRaw) : "-"}`);
    console.log(`max time to tokenize..............: ${maxTimeRawToTokenize ? getTimeString(maxTimeRawToTokenize) : "-"}`);
    console.log(`max time to ingest................: ${maxTimeRawToIngest ? getTimeString(maxTimeRawToIngest) : "-"}`);
    console.log("--------------------------------");
    console.log(`failed to be captured.............: ${failedRaw}`);
    console.log(`failed to be tokenized............: ${failedTokenized}`);
    console.log(`failed to be ingested.............: ${failedIngested}`);
    console.log("/////////////////////////////////");
    console.log("/------------- END -------------/");
    console.log("/////////////////////////////////");
}

function getTimeString(time) {
    return `${time}ms | ${time / 1000}s`;
}

reviewIngestionTimeToProcess();