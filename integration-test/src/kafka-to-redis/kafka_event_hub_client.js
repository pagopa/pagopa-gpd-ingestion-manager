const { Kafka } = require('kafkajs')
const { createClient } = require('redis');

const connectionString = process.env.INGESTION_EVENTHUB_CONN_STRING;

async function eventHubToRedisHandler() {
    try {
        const kafka = new Kafka({
            clientId: 'my-app', brokers: ['pagopa-d-itn-observ-gpd-evh.servicebus.windows.net:9093'],   // 
            authenticationTimeout: 10000, // 
            reauthenticationThreshold: 10000,
            ssl: true,
            sasl: {
                mechanism: 'plain', // scram-sha-256 or scram-sha-512
                username: '$ConnectionString',
                password: ""
            },
        })
        // Connect to Kafka broker
        const consumer = kafka.consumer({ groupId: 'gpd-ingestion-integration-test-consumer-group' });
        await consumer.connect();
        await consumer.subscribe({ topics: ['gpd-ingestion.apd.payment_position'] })

        // Create Redis client
        const client = createClient({
            socket: {
                port: "6379",
                host: "127.0.0.1"
            }
        });
        client.on('error', err => console.log('Redis Client Error', err))
        await client.connect();

        client.on('connect', function () {
            console.log('Connected!');
        });


        // Listen to the topic
        let decoder = new TextDecoder("utf-8");
        await consumer.run({
            eachMessage: async ({ topic, partition, message, heartbeat, pause }) => {
                writeOnRedis(client, decoder, message);
            },
        })

        
        // when call client close?
        // await client.quit();
    } catch (e) {
        console.error(e);
    }
}

async function writeOnRedis(client, decoder, message) {
    let decodedMessage = JSON.parse(decoder.decode(message.value));
    console.log(decodedMessage)
    let id = getEventId(decodedMessage);
    console.log("ID ", id);
    await client.set(id, message);
}

function getEventId(event) {
    if (event.op === "c") {
        return event.after.id + "-c";
    } else if (event.op === "d") {
        return event.before.id + "-d";
    } else {
        return event.after.id + "-u";
    }
}

eventHubToRedisHandler();
