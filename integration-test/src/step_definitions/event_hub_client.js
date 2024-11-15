const { EventHubConsumerClient, earliestEventPosition } = require("@azure/event-hubs");

const connectionString = process.env.INGESTION_EVENTHUB_CONN_STRING;
const consumerGroup = process.env.INGESTION_EVENTHUB_CONSUMER_GROUP_NAME;

const consumerClient = new EventHubConsumerClient(consumerGroup, connectionString);

async function readMessage(id) {
    console.log(`Running receiveEvents sample`);

    try {
        const subscription = consumerClient.subscribe(
            {
                // The callback where you add your code to process incoming events
                processEvents: async (events, context) => {
                    // Note: It is possible for `events` to be an empty array.
                    // This can happen if there were no new events to receive
                    // in the `maxWaitTimeInSeconds`, which is defaulted to
                    // 60 seconds.
                    // The `maxWaitTimeInSeconds` can be changed by setting
                    // it in the `options` passed to `subscribe()`.
                    for (const event of events) {
                        if (event?.body?.before?.id === id) {
                            return event.body;
                        } else if (event?.body?.after?.id === id) {
                            return event.body;
                        }
                        console.log(
                            `Received event: '${event.body}' from partition: '${context.partitionId}' and consumer group: '${context.consumerGroup}'`,
                        );
                    }
                },
                processError: async (err, context) => {
                    console.log(`Error on partition "${context.partitionId}": ${err}`);
                },
            },
            { startPosition: earliestEventPosition },
        );

        // Wait for a bit before cleaning up the sample
        setTimeout(async () => {
            await subscription.close();
            await consumerClient.close();
            console.log(`Exiting receiveEvents sample`);
        }, 30 * 1000);

    } catch (err) {
        console.error("Error running sample:", error);
    }
}


module.exports = { readMessage };