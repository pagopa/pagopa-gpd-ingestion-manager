const { createClient } = require('redis');

const redisHost = process.env.REDIS_HOST;
const redisPort = process.env.REDIS_PORT;

const client = createClient({
    socket: {
        port: redisPort,
        host: redisHost
    }
});

client.on('error', err => console.log('Redis Client Error', err))
client.connect();

client.on('connect', function () {
    console.log('Connected!');
});

async function readFromRedisWithKey(key) {
    return await client.get(key);
}

async function shutDownClient() {
    await client.quit();
  }

module.exports = {
    readFromRedisWithKey, shutDownClient
  }